package com.example.monitoring.notification

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.example.di.AppContainer
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.RidePlatform
import com.example.monitoring.service.MonitoringStateRegistry
import com.example.monitoring.service.RideMonitoringService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Permitted Android NotificationListenerService that observes ONLY notifications
 * from supported ride-hailing partner apps (Uber Driver, Rapido Captain, Ola Driver).
 *
 * Strictly ignores all personal messages, OTPs, banking, and unrelated apps.
 */
class RideNotificationListener : NotificationListenerService() {

    private val listenerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onListenerConnected() {
        super.onListenerConnected()
        MonitoringStateRegistry.setListenerConnected(true)
        AppContainer.getInstance(applicationContext).rideEventProcessor.appendDiagnostic(
            title = "NotificationListener Connected",
            detail = "Android OS bound RideNotificationListener for permitted partner apps."
        )
    }

    override fun onListenerDisconnected() {
        MonitoringStateRegistry.setListenerConnected(false)
        AppContainer.getInstance(applicationContext).rideEventProcessor.appendDiagnostic(
            title = "NotificationListener Disconnected",
            detail = "Android OS unbound RideNotificationListener.",
            isWarning = true
        )
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val statusBarNotification = sbn ?: return
        val pkg = statusBarNotification.packageName ?: return

        // Privacy boundary: strictly ignore any package outside Uber, Rapido, Ola
        val supportedPlatform = RidePlatform.fromPackageName(pkg) ?: return

        // Only process live notifications when RideMonitoringService is ACTIVE
        if (MonitoringStateRegistry.serviceState.value != MonitoringServiceState.ACTIVE) {
            return
        }

        val extras = statusBarNotification.notification?.extras
        val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras?.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        if (title.isNullOrBlank() && text.isNullOrBlank() && bigText.isNullOrBlank()) {
            return
        }

        val normalizedEvent = NormalizedNotificationEvent(
            notificationKey = statusBarNotification.key ?: "${supportedPlatform.id}_${statusBarNotification.postTime}",
            packageName = pkg,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            postedAtMillis = statusBarNotification.postTime.takeIf { it > 0L } ?: System.currentTimeMillis(),
            isDemo = false,
            demoScenarioName = null
        )

        val container = AppContainer.getInstance(applicationContext)
        listenerScope.launch {
            container.rideEventProcessor.processIncomingNotification(
                event = normalizedEvent,
                onInstantAutoAcceptTrigger = {
                    try {
                        val actions = statusBarNotification.notification?.actions
                        val acceptAction = actions?.firstOrNull { action ->
                            val titleStr = action.title?.toString()?.lowercase().orEmpty()
                            titleStr.contains("accept") || titleStr.contains("confirm") || titleStr.contains("go")
                        }
                        if (acceptAction?.actionIntent != null) {
                            acceptAction.actionIntent.send()
                        }
                    } catch (_: Exception) {
                        // PendingIntent already consumed or restricted
                    }
                }
            )
        }
    }

    override fun onDestroy() {
        MonitoringStateRegistry.setListenerConnected(false)
        listenerScope.cancel()
        super.onDestroy()
    }
}

/**
 * Handles actionable buttons inside driver alert notifications ("Confirm Ride", "Reject", "Stop Monitoring").
 */
class RideNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val container = AppContainer.getInstance(context.applicationContext)
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (action) {
                    DriverAlertNotifier.ACTION_CONFIRM_RIDE -> {
                        val eventId = intent.getLongExtra(DriverAlertNotifier.EXTRA_EVENT_ID, -1L)
                        if (eventId > 0L) {
                            container.rideEventProcessor.confirmRideByDriver(eventId)
                        }
                    }
                    DriverAlertNotifier.ACTION_REJECT_RIDE -> {
                        val eventId = intent.getLongExtra(DriverAlertNotifier.EXTRA_EVENT_ID, -1L)
                        if (eventId > 0L) {
                            container.rideEventProcessor.rejectRideByDriver(eventId)
                        }
                    }
                    DriverAlertNotifier.ACTION_STOP_MONITORING -> {
                        RideMonitoringService.stopMonitoring(context)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
