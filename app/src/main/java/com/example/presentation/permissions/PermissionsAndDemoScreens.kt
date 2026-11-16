package com.example.presentation.permissions

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.MonitoringSessionRecord
import com.example.domain.model.RidePlatform
import com.example.monitoring.processor.DemoScenarioType
import com.example.monitoring.processor.ProcessingDiagnosticEntry
import com.example.presentation.components.accentColor
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.StatusFiltered
import com.example.utils.CurrencyFormatter
import com.example.utils.PermissionHelper
import com.example.utils.PermissionStatusSnapshot

@Composable
fun PermissionsScreen(
    permissionSnapshot: PermissionStatusSnapshot,
    listenerConnected: Boolean,
    onRefreshPermissions: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val postNotificationsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { onRefreshPermissions() }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column {
                        Text(
                            text = "Monitoring Permissions",
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onBackground,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Verified live against Android OS permission state",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(
                    onClick = onRefreshPermissions,
                    modifier = Modifier.testTag("refresh_permissions_button")
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh permission status")
                }
            }
        }

        // 1. Notification Listener Access
        item {
            PermissionStatusCard(
                title = "1. Notification Listener Access",
                granted = permissionSnapshot.notificationListenerGranted,
                statusText = when {
                    listenerConnected -> "Granted & Bound by Android OS"
                    permissionSnapshot.notificationListenerGranted -> "Granted in Android Settings"
                    else -> "Permission Required"
                },
                explanation = "Allows RideNotificationListener to observe incoming ride notifications from Uber Driver, Rapido Captain, and Ola Driver. LOMAP never reads personal messages, OTPs, or other apps.",
                buttonLabel = "Open Android Notification Access Settings",
                onActionClick = {
                    runCatching {
                        context.startActivity(PermissionHelper.buildNotificationListenerSettingsIntent())
                    }
                }
            )
        }

        // 2. Post Notifications Permission
        item {
            PermissionStatusCard(
                title = "2. Runtime Notification Permission (POST_NOTIFICATIONS)",
                granted = permissionSnapshot.postNotificationsGranted,
                statusText = if (permissionSnapshot.postNotificationsGranted) {
                    "Granted by Android OS"
                } else {
                    "Permission Required (Android 13+)"
                },
                explanation = "Required to display the persistent Foreground Service monitoring banner and high-priority alerts when an eligible INR ride fare is detected.",
                buttonLabel = if (permissionSnapshot.postNotificationsGranted) {
                    "Open App Notification Settings"
                } else {
                    "Request Notification Permission"
                },
                onActionClick = {
                    if (!permissionSnapshot.postNotificationsGranted &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                    ) {
                        postNotificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        runCatching {
                            context.startActivity(PermissionHelper.buildAppNotificationSettingsIntent(context))
                        }
                    }
                }
            )
        }

        // 3. Foreground Service Capability
        item {
            PermissionStatusCard(
                title = "3. Foreground Monitoring Service (DATA_SYNC)",
                granted = permissionSnapshot.foregroundServicePermitted,
                statusText = "Declared & Ready in AndroidManifest",
                explanation = "LOMAP runs a user-visible Foreground Service with a Stop button in the notification shade so Android does not suspend fare evaluation while you are waiting for rides.",
                buttonLabel = null,
                onActionClick = null
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionStatusCard(
    title: String,
    granted: Boolean,
    statusText: String,
    explanation: String,
    buttonLabel: String?,
    onActionClick: (() -> Unit)?
) {
    val indicatorColor = if (granted) ElectricLime else StatusFiltered

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, indicatorColor.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (granted) Icons.Default.CheckCircle else Icons.Default.WarningAmber,
                    contentDescription = null,
                    tint = indicatorColor
                )
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.labelSmall,
                        color = indicatorColor
                    )
                }
            }

            Text(
                text = explanation,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (buttonLabel != null && onActionClick != null) {
                OutlinedButton(
                    onClick = onActionClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(buttonLabel)
                }
            }
        }
    }
}

@Composable
fun DemoDiagnosticsScreen(
    diagnostics: List<ProcessingDiagnosticEntry>,
    duplicatesBlocked: Int,
    recentSessions: List<MonitoringSessionRecord>,
    onRunScenario: (RidePlatform, DemoScenarioType) -> Unit,
    onClearDemoEvents: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by rememberSaveable { mutableStateOf(RidePlatform.UBER) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text(
                        text = "Demo Mode & Diagnostics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "All simulated events are explicitly badged [DEMO] and separated from live dispatches.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Target Platform Picker for Demo Scenarios
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Select Target Platform for Simulation",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedButton(
                            onClick = onClearDemoEvents,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Purge DEMO Events", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RidePlatform.entries.forEach { platform ->
                            FilterChip(
                                selected = selectedPlatform == platform,
                                onClick = { selectedPlatform = platform },
                                label = { Text(platform.displayName) },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("demo_platform_${platform.id}")
                            )
                        }
                    }
                }
            }
        }

        // 12 Required Test Scenarios
        item {
            Text(
                text = "12 Standard Verification Scenarios (${selectedPlatform.appTitle})",
                style = MaterialTheme.typography.titleMedium,
                color = selectedPlatform.accentColor(),
                fontWeight = FontWeight.Bold
            )
        }

        items(DemoScenarioType.entries, key = { it.id }) { scenario ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
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
                            text = scenario.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = scenario.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Button(
                        onClick = { onRunScenario(selectedPlatform, scenario) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("run_demo_scenario_${scenario.id}")
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run")
                    }
                }
            }
        }

        // Processor Diagnostics & Deduplication Telemetry
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.BugReport,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Event Processor Diagnostics (Duplicates Blocked: $duplicatesBlocked)",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (diagnostics.isEmpty()) {
                        Text(
                            text = "No diagnostic log entries yet. Trigger any scenario above to inspect pipeline execution.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        diagnostics.take(10).forEach { entry ->
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = entry.title,
                                            style = MaterialTheme.typography.labelLarge,
                                            color = if (entry.isWarning) StatusFiltered else ElectricLime
                                        )
                                        Text(
                                            text = CurrencyFormatter.formatShortTime(entry.timestampMillis),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = entry.detail,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    if (recentSessions.isNotEmpty()) {
                        Text(
                            text = "Recent Monitoring Sessions (${recentSessions.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        recentSessions.take(4).forEach { session ->
                            Text(
                                text = "Session #${session.id} • Started ${CurrencyFormatter.formatTimestamp(session.startedAtMillis)} • ${session.stopReason ?: "Active"}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
