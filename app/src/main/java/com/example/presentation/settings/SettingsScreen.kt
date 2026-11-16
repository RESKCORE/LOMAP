package com.example.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.datastore.PersistedAppSettings
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DriverProfileSettings
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.NotificationPreferences
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.StatusDanger
import com.example.utils.PermissionStatusSnapshot

@Composable
@Suppress("UNUSED_PARAMETER")
fun SettingsScreen(
    settings: PersistedAppSettings,
    googleUserEmail: String? = null,
    monitoringState: MonitoringServiceState,
    permissionSnapshot: PermissionStatusSnapshot,
    onSaveProfile: (DriverProfileSettings) -> Unit,
    onSaveThemeMode: (AppThemeMode) -> Unit,
    onSaveNotifications: (NotificationPreferences) -> Unit,
    onClearHistory: () -> Unit,
    onSignOutGoogle: () -> Unit = {},
    onOpenPermissions: () -> Unit,
    onOpenDemoLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    var driverName by remember(settings.driverProfile) { mutableStateOf(settings.driverProfile.driverName) }
    var phoneNumber by remember(settings.driverProfile) { mutableStateOf(settings.driverProfile.phoneNumber) }
    var city by remember(settings.driverProfile) { mutableStateOf(settings.driverProfile.city) }

    var showTermsDialog by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }

    val notifPrefs = settings.notificationPreferences

    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val horizontalPad = if (maxWidth < 360.dp) 12.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = horizontalPad),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "LOMAP Account & Settings",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Manage your Google Driver account, cloud sync, alert behavior, and local data privacy.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // 0. Google Authentication & Cloud Sync Card
            item {
                SettingsSectionCard(
                    icon = Icons.Default.CloudDone,
                    title = "Google Account & Cloud Sync"
                ) {
                    Surface(
                        color = ElectricLime.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Signed in with Google",
                                style = MaterialTheme.typography.labelMedium,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = settings.driverProfile.driverName,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            if (!googleUserEmail.isNullOrBlank()) {
                                Text(
                                    text = googleUserEmail,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "LOMAP Cloud Firestore Sync Active • Millisecond Auto-Accept (< 3 ms)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onSignOutGoogle,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_google_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out of Google Account",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // 1. Driver Profile Section
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Person,
                    title = "Driver Profile"
                ) {
                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("Driver Display Name") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_driver_name_input")
                    )
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = city,
                        onValueChange = { city = it },
                        label = { Text("Operating City") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Currency: Indian Rupee (INR ₹)",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Button(
                            onClick = {
                                onSaveProfile(
                                    DriverProfileSettings(
                                        driverName = driverName,
                                        phoneNumber = phoneNumber,
                                        city = city
                                    )
                                )
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("save_profile_button")
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                    }
                }
            }

            // 2. Single Unified Theme Section
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Palette,
                    title = "LOMAP Signature Theme"
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daylight & Electric Volt (Single Theme)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Unified high-contrast light mobility theme calibrated for glare-free readability during day and night shifts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // 3. Notifications Section
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Notifications,
                    title = "Driver Notification Preferences"
                ) {
                    SettingsToggleRow(
                        label = "Alert Sound",
                        subtitle = "Play notification tone for eligible ride requests",
                        checked = notifPrefs.soundEnabled,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(soundEnabled = it)) }
                    )
                    SettingsToggleRow(
                        label = "Haptic Vibration",
                        subtitle = "Vibrate device when new ride requests are evaluated",
                        checked = notifPrefs.vibrationEnabled,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(vibrationEnabled = it)) }
                    )
                    SettingsToggleRow(
                        label = "Eligible Fare Alerts",
                        subtitle = "Notify immediately when a ride matches your INR range",
                        checked = notifPrefs.notifyEligibleFare,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(notifyEligibleFare = it)) }
                    )
                    SettingsToggleRow(
                        label = "Below/Above Range Alerts",
                        subtitle = "Notify even when a ride is filtered out",
                        checked = notifPrefs.notifyFilteredFare,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(notifyFilteredFare = it)) }
                    )
                    SettingsToggleRow(
                        label = "Unknown Fare Alerts",
                        subtitle = "Warn when a dispatch notification omits the fare",
                        checked = notifPrefs.notifyUnknownFare,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(notifyUnknownFare = it)) }
                    )
                    SettingsToggleRow(
                        label = "Service Lifecycle Notifications",
                        subtitle = "Notify when monitoring starts or stops",
                        checked = notifPrefs.notifyServiceLifecycle,
                        onCheckedChange = { onSaveNotifications(notifPrefs.copy(notifyServiceLifecycle = it)) }
                    )
                }
            }

            // 4. Monitoring & Diagnostics Section
            item {
                SettingsSectionCard(
                    icon = Icons.Default.BugReport,
                    title = "Monitoring & Service Diagnostics"
                ) {
                    Text(
                        text = "Current Service State: ${monitoringState.label}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Notification Listener Access: ${if (permissionSnapshot.notificationListenerGranted) "Granted" else "Not Granted"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Post Notifications Permission: ${if (permissionSnapshot.postNotificationsGranted) "Granted" else "Not Granted"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenPermissions,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Permissions")
                        }
                        Button(
                            onClick = onOpenDemoLab,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Demo Lab")
                        }
                    }
                }
            }

            // 5. Privacy & Local Data Management
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Security,
                    title = "Privacy & Data Management"
                ) {
                    Text(
                        text = "• Notification Access Scope: LOMAP inspects notifications exclusively from com.ubercab.driver, com.rapido.rider, and com.olacabs.oladriver. Personal chats, SMS, OTPs, and banking alerts are never read.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "• Hybrid Local + Cloud Sync: Ride history is stored locally in Room SQLite and synced securely to your personal Google Cloud Firestore document.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = onClearHistory,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = StatusDanger,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_clear_history_button")
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Clear All Local Ride History")
                    }
                }
            }

            // 6. About Section
            item {
                SettingsSectionCard(
                    icon = Icons.Default.Info,
                    title = "About LOMAP"
                ) {
                    Text(
                        text = "LOMAP v1.2.0 (Native Android • Kotlin • Jetpack Compose • Firebase)",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Built for multi-platform drivers in India (Uber Driver, Rapido Captain, Ola Driver).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showTermsDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Terms of Use")
                        }
                        OutlinedButton(
                            onClick = { showPrivacyDialog = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Privacy Policy")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (showTermsDialog) {
        AlertDialog(
            onDismissRequest = { showTermsDialog = false },
            title = { Text("LOMAP Terms of Service") },
            text = {
                Text(
                    "LOMAP is a driver companion application that evaluates incoming ride notifications against your configured INR fare preferences and triggers authorized millisecond acceptance for in-range fares."
                )
            },
            confirmButton = {
                TextButton(onClick = { showTermsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("LOMAP Privacy Policy") },
            text = {
                Text(
                    "1. Notification Filtering: Only notifications originating from Uber Driver, Rapido Captain, and Ola Driver packages are parsed.\n2. Google Authentication: Your profile and ride logs are protected by per-user Firebase Security Rules.\n3. User Control: You can sign out, revoke Notification Access, or clear your local database at any time."
                )
            },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
private fun SettingsSectionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }
            content()
        }
    }
}

@Composable
private fun SettingsToggleRow(
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
