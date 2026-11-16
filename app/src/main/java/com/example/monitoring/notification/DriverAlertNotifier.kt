package com.example.monitoring.notification

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.RideEventRecord
import com.example.utils.CurrencyFormatter

class DriverAlertNotifier(private val context: Context) {

    companion object {
        const val CHANNEL_MONITORING_ID = "ridepilot_monitoring_channel"
        const val CHANNEL_ALERTS_ID = "ridepilot_alerts_channel"
        const val FOREGROUND_NOTIFICATION_ID = 4101

        const val ACTION_CONFIRM_RIDE = "com.example.ridepilot.ACTION_CONFIRM_RIDE"
        const val ACTION_REJECT_RIDE = "com.example.ridepilot.ACTION_REJECT_RIDE"
        const val ACTION_STOP_MONITORING = "com.example.ridepilot.ACTION_STOP_MONITORING"
        const val EXTRA_EVENT_ID = "extra_event_id"
    }

    init {
        ensureChannelsCreated()
    }

    fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            val monitoringChannel = NotificationChannel(
                CHANNEL_MONITORING_ID,
                context.getString(R.string.monitoring_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = context.getString(R.string.monitoring_channel_desc)
                setShowBadge(false)
            }

            val alertsChannel = NotificationChannel(
                CHANNEL_ALERTS_ID,
                context.getString(R.string.alerts_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.alerts_channel_desc)
                enableVibration(true)
            }

            manager.createNotificationChannel(monitoringChannel)
            manager.createNotificationChannel(alertsChannel)
        }
    }

    fun buildForegroundMonitoringNotification(
        activePlatformsSummary: String,
        detectedCount: Int
    ): Notification {
        ensureChannelsCreated()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            100,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, RideNotificationActionReceiver::class.java).apply {
            action = ACTION_STOP_MONITORING
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            context,
            101,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_MONITORING_ID)
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setContentTitle("LOMAP Monitoring Active")
            .setContentText("Watching $activePlatformsSummary • $detectedCount requests evaluated")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(openPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop Monitoring",
                stopPendingIntent
            )
            .build()
    }

    fun notifyRideEvaluated(
        record: RideEventRecord,
        preferences: NotificationPreferences
    ) {
        val shouldNotify = when (record.fareEvaluationResult) {
            FareEvaluationResult.ELIGIBLE ->
                if (record.isOverallEligible) preferences.notifyEligibleFare else preferences.notifyFilteredFare
            FareEvaluationResult.BELOW_MINIMUM,
            FareEvaluationResult.ABOVE_MAXIMUM -> preferences.notifyFilteredFare
            FareEvaluationResult.UNKNOWN_FARE -> preferences.notifyUnknownFare
            FareEvaluationResult.PLATFORM_DISABLED,
            FareEvaluationResult.INVALID_CONFIGURATION -> preferences.notifyFilteredFare
        }

        if (!shouldNotify && !preferences.notifyIncomingDetected) return
        if (!canPostNotifications()) return

        if (preferences.vibrationEnabled) {
            triggerHapticPulse(isEligible = record.isOverallEligible)
        }

        val demoPrefix = if (record.isDemo) "[DEMO] " else ""
        val fareDisplay = CurrencyFormatter.formatInr(record.fareInr)
        val title = when {
            record.isOverallEligible ->
                "$demoPrefix✓ Eligible ${record.platform.displayName} Ride • $fareDisplay"
            record.fareEvaluationResult == FareEvaluationResult.UNKNOWN_FARE ->
                "$demoPrefix? Unknown Fare on ${record.platform.displayName}"
            record.fareEvaluationResult == FareEvaluationResult.BELOW_MINIMUM ->
                "$demoPrefix↓ Below Min (${record.platform.displayName} • $fareDisplay)"
            record.fareEvaluationResult == FareEvaluationResult.ABOVE_MAXIMUM ->
                "$demoPrefix↑ Above Max (${record.platform.displayName} • $fareDisplay)"
            else ->
                "$demoPrefix${record.platform.displayName} Filtered • ${record.statusDetail}"
        }

        val routeSummary = buildString {
            record.pickupLocation?.let { append("Pickup: $it") }
            record.destinationLocation?.let {
                if (isNotEmpty()) append(" → ")
                append("Drop: $it")
            }
            record.tripDistanceKm?.let {
                if (isNotEmpty()) append(" • ")
                append(CurrencyFormatter.formatDistanceKm(it))
            }
            if (isEmpty()) {
                append(record.statusDetail)
            }
        }

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context,
            record.id.toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(routeSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText("$routeSummary\n${record.statusDetail}"))
            .setAutoCancel(true)
            .setContentIntent(openPending)
            .setPriority(
                if (record.isOverallEligible) NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )

        if (!preferences.soundEnabled) {
            builder.setSilent(true)
        }

        if (record.id > 0L &&
            (record.isOverallEligible || record.acceptanceOutcome == AcceptanceOutcome.UNKNOWN_FARE)
        ) {
            val confirmIntent = Intent(context, RideNotificationActionReceiver::class.java).apply {
                action = ACTION_CONFIRM_RIDE
                putExtra(EXTRA_EVENT_ID, record.id)
            }
            val confirmPending = PendingIntent.getBroadcast(
                context,
                (record.id * 10 + 1).toInt(),
                confirmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val rejectIntent = Intent(context, RideNotificationActionReceiver::class.java).apply {
                action = ACTION_REJECT_RIDE
                putExtra(EXTRA_EVENT_ID, record.id)
            }
            val rejectPending = PendingIntent.getBroadcast(
                context,
                (record.id * 10 + 2).toInt(),
                rejectIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(
                android.R.drawable.checkbox_on_background,
                "Confirm Ride",
                confirmPending
            )
            builder.addAction(
                android.R.drawable.ic_delete,
                "Reject",
                rejectPending
            )
        }

        try {
            NotificationManagerCompat.from(context).notify((4200 + (record.id % 500)).toInt(), builder.build())
        } catch (_: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun notifySystemAlert(title: String, message: String, preferences: NotificationPreferences) {
        if (!preferences.notifyServiceLifecycle || !canPostNotifications()) return
        ensureChannelsCreated()

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(title)
            .setContentText(message)
            .setAutoCancel(true)
            .setSilent(!preferences.soundEnabled)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(4199, notification)
        } catch (_: SecurityException) {
            // Ignored
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun triggerHapticPulse(isEligible: Boolean) {
        try {
            val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator?.hasVibrator() == true) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val timings = if (isEligible) longArrayOf(0, 90, 60, 140) else longArrayOf(0, 60)
                    val effect = VibrationEffect.createWaveform(timings, -1)
                    vibrator.vibrate(effect)
                }
            }
        } catch (_: Exception) {
            // Ignore vibration failures in restricted environments
        }
    }
}
