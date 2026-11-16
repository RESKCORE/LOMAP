package com.example.utils

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.ContextCompat
import com.example.domain.model.PlatformIntegrationStatus
import com.example.domain.model.RidePlatform
import com.example.monitoring.notification.RideNotificationListener

data class PermissionStatusSnapshot(
    val notificationListenerGranted: Boolean,
    val postNotificationsGranted: Boolean,
    val foregroundServicePermitted: Boolean,
    val uberInstalled: Boolean,
    val rapidoInstalled: Boolean,
    val olaInstalled: Boolean
) {
    val allRequiredGranted: Boolean
        get() = notificationListenerGranted && postNotificationsGranted && foregroundServicePermitted
}

object PermissionHelper {

    fun isNotificationListenerEnabled(context: Context): Boolean {
        return try {
            val cn = ComponentName(context, RideNotificationListener::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            ) ?: return false
            flat.split(":").any { componentString ->
                ComponentName.unflattenFromString(componentString) == cn ||
                    componentString.contains(context.packageName)
            }
        } catch (_: Exception) {
            false
        }
    }

    fun isPostNotificationsGranted(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun isPackageInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (_: PackageManager.NameNotFoundException) {
            false
        } catch (_: Exception) {
            false
        }
    }

    fun getPermissionSnapshot(context: Context): PermissionStatusSnapshot {
        return PermissionStatusSnapshot(
            notificationListenerGranted = isNotificationListenerEnabled(context),
            postNotificationsGranted = isPostNotificationsGranted(context),
            foregroundServicePermitted = true, // Declared in AndroidManifest with FOREGROUND_SERVICE_DATA_SYNC
            uberInstalled = isPackageInstalled(context, RidePlatform.UBER.packageName),
            rapidoInstalled = isPackageInstalled(context, RidePlatform.RAPIDO.packageName),
            olaInstalled = isPackageInstalled(context, RidePlatform.OLA.packageName)
        )
    }

    fun resolvePlatformIntegrationStatus(
        platformEnabled: Boolean,
        authorizedAdapterSimulated: Boolean,
        notificationListenerGranted: Boolean,
        listenerServiceConnected: Boolean
    ): PlatformIntegrationStatus {
        if (!platformEnabled) {
            return PlatformIntegrationStatus.NOT_CONFIGURED
        }
        if (authorizedAdapterSimulated) {
            return PlatformIntegrationStatus.CONNECTED_AUTHORIZED
        }
        if (!notificationListenerGranted) {
            return PlatformIntegrationStatus.PERMISSION_REQUIRED
        }
        if (!listenerServiceConnected) {
            // Even when NotificationListener permission is granted in settings,
            // explain clearly whether listener is actively bound or ready for permitted monitoring.
            return PlatformIntegrationStatus.READY_FOR_PERMITTED_MONITORING
        }
        return PlatformIntegrationStatus.READY_FOR_PERMITTED_MONITORING
    }

    fun buildNotificationListenerSettingsIntent(): Intent {
        return Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    fun buildAppNotificationSettingsIntent(context: Context): Intent {
        return Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }
}
