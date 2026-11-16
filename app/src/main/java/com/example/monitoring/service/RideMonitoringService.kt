package com.example.monitoring.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.content.ContextCompat
import com.example.di.AppContainer
import com.example.domain.model.MonitoringServiceState
import com.example.monitoring.notification.DriverAlertNotifier
import com.example.utils.PermissionHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Process-wide source of truth for the native foreground service & listener binding state.
 * Only transitions to MonitoringServiceState.ACTIVE when RideMonitoringService.startForeground()
 * has genuinely succeeded.
 */
object MonitoringStateRegistry {
    private val _serviceState = MutableStateFlow(MonitoringServiceState.INACTIVE)
    val serviceState: StateFlow<MonitoringServiceState> = _serviceState.asStateFlow()

    private val _elapsedSeconds = MutableStateFlow(0L)
    val elapsedSeconds: StateFlow<Long> = _elapsedSeconds.asStateFlow()

    private val _listenerConnected = MutableStateFlow(false)
    val listenerConnected: StateFlow<Boolean> = _listenerConnected.asStateFlow()

    private val _lastErrorDetail = MutableStateFlow<String?>(null)
    val lastErrorDetail: StateFlow<String?> = _lastErrorDetail.asStateFlow()

    fun updateServiceState(state: MonitoringServiceState, errorDetail: String? = null) {
        _serviceState.value = state
        _lastErrorDetail.value = errorDetail
        if (state == MonitoringServiceState.INACTIVE ||
            state == MonitoringServiceState.PERMISSION_REQUIRED ||
            state == MonitoringServiceState.SERVICE_ERROR
        ) {
            _elapsedSeconds.value = 0L
        }
    }

    fun updateElapsedSeconds(seconds: Long) {
        _elapsedSeconds.value = seconds
    }

    fun setListenerConnected(connected: Boolean) {
        _listenerConnected.value = connected
    }
}

class RideMonitoringService : Service() {

    companion object {
        const val ACTION_START = "com.example.ridepilot.service.ACTION_START"
        const val ACTION_PAUSE = "com.example.ridepilot.service.ACTION_PAUSE"
        const val ACTION_RESUME = "com.example.ridepilot.service.ACTION_RESUME"
        const val ACTION_STOP = "com.example.ridepilot.service.ACTION_STOP"

        fun startMonitoring(context: Context, allowDemoWithoutListener: Boolean = false) {
            val permissionSnapshot = PermissionHelper.getPermissionSnapshot(context)
            if (!permissionSnapshot.postNotificationsGranted && !allowDemoWithoutListener) {
                MonitoringStateRegistry.updateServiceState(
                    MonitoringServiceState.PERMISSION_REQUIRED,
                    "Notification permission (POST_NOTIFICATIONS) is required to show the persistent monitoring indicator."
                )
                return
            }

            MonitoringStateRegistry.updateServiceState(MonitoringServiceState.STARTING)
            val intent = Intent(context, RideMonitoringService::class.java).apply {
                action = ACTION_START
            }
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                MonitoringStateRegistry.updateServiceState(
                    MonitoringServiceState.SERVICE_ERROR,
                    e.localizedMessage ?: "Unable to start foreground service."
                )
            }
        }

        fun pauseMonitoring(context: Context) {
            val intent = Intent(context, RideMonitoringService::class.java).apply {
                action = ACTION_PAUSE
            }
            context.startService(intent)
        }

        fun resumeMonitoring(context: Context) {
            val intent = Intent(context, RideMonitoringService::class.java).apply {
                action = ACTION_RESUME
            }
            context.startService(intent)
        }

        fun stopMonitoring(context: Context) {
            val intent = Intent(context, RideMonitoringService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var timerJob: Job? = null
    private var activeSessionId: Long? = null
    private var accumulatedSeconds: Long = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val container = AppContainer.getInstance(applicationContext)
        when (intent?.action) {
            ACTION_START -> handleStart(container)
            ACTION_PAUSE -> handlePause(container)
            ACTION_RESUME -> handleResume(container)
            ACTION_STOP -> handleStop(container, "Driver stopped monitoring")
            else -> handleStart(container)
        }
        return START_STICKY
    }

    private fun handleStart(container: AppContainer) {
        try {
            val notification = container.driverAlertNotifier.buildForegroundMonitoringNotification(
                activePlatformsSummary = "Uber • Rapido • Ola",
                detectedCount = 0
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    DriverAlertNotifier.FOREGROUND_NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                )
            } else {
                startForeground(DriverAlertNotifier.FOREGROUND_NOTIFICATION_ID, notification)
            }

            accumulatedSeconds = 0L
            MonitoringStateRegistry.updateElapsedSeconds(0L)
            MonitoringStateRegistry.updateServiceState(MonitoringServiceState.ACTIVE)

            serviceScope.launch {
                val settings = container.repository.getCurrentSettings()
                activeSessionId = container.repository.startMonitoringSession(System.currentTimeMillis())
                container.rideEventProcessor.appendDiagnostic(
                    title = "RideMonitoringService Started",
                    detail = "Foreground service active (Session #${activeSessionId ?: 1})."
                )
                container.driverAlertNotifier.notifySystemAlert(
                    title = "LOMAP Monitoring Started",
                    message = "Actively evaluating incoming ride notifications against your INR filters.",
                    preferences = settings.notificationPreferences
                )
            }
            startDurationTicker()
        } catch (e: Exception) {
            MonitoringStateRegistry.updateServiceState(
                MonitoringServiceState.SERVICE_ERROR,
                e.localizedMessage ?: "Foreground service start failed."
            )
            stopSelf()
        }
    }

    private fun handlePause(container: AppContainer) {
        if (MonitoringStateRegistry.serviceState.value == MonitoringServiceState.ACTIVE) {
            timerJob?.cancel()
            MonitoringStateRegistry.updateServiceState(MonitoringServiceState.PAUSED)
            container.rideEventProcessor.appendDiagnostic(
                title = "RideMonitoringService Paused",
                detail = "Foreground service paused by driver at ${accumulatedSeconds}s."
            )
        }
    }

    private fun handleResume(container: AppContainer) {
        if (MonitoringStateRegistry.serviceState.value == MonitoringServiceState.PAUSED) {
            MonitoringStateRegistry.updateServiceState(MonitoringServiceState.ACTIVE)
            startDurationTicker()
            container.rideEventProcessor.appendDiagnostic(
                title = "RideMonitoringService Resumed",
                detail = "Foreground service resumed active monitoring."
            )
        }
    }

    private fun handleStop(container: AppContainer, reason: String) {
        timerJob?.cancel()
        val sessionIdToClose = activeSessionId
        activeSessionId = null

        serviceScope.launch {
            val settings = container.repository.getCurrentSettings()
            if (sessionIdToClose != null) {
                container.repository.endMonitoringSession(
                    sessionId = sessionIdToClose,
                    endedAtMillis = System.currentTimeMillis(),
                    stopReason = reason
                )
            }
            container.rideEventProcessor.appendDiagnostic(
                title = "RideMonitoringService Stopped",
                detail = reason
            )
            container.driverAlertNotifier.notifySystemAlert(
                title = "LOMAP Monitoring Stopped",
                message = "Background ride request evaluation has been stopped.",
                preferences = settings.notificationPreferences
            )
        }

        MonitoringStateRegistry.updateServiceState(MonitoringServiceState.INACTIVE)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun startDurationTicker() {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive && MonitoringStateRegistry.serviceState.value == MonitoringServiceState.ACTIVE) {
                delay(1_000L)
                accumulatedSeconds += 1L
                MonitoringStateRegistry.updateElapsedSeconds(accumulatedSeconds)
            }
        }
    }

    override fun onDestroy() {
        timerJob?.cancel()
        if (MonitoringStateRegistry.serviceState.value == MonitoringServiceState.ACTIVE ||
            MonitoringStateRegistry.serviceState.value == MonitoringServiceState.PAUSED
        ) {
            MonitoringStateRegistry.updateServiceState(MonitoringServiceState.INACTIVE)
        }
        serviceScope.cancel()
        super.onDestroy()
    }
}
