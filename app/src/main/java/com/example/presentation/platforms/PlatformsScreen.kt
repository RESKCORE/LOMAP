package com.example.presentation.platforms

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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.datastore.PersistedAppSettings
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.PlatformIntegrationStatus
import com.example.domain.model.RidePlatform
import com.example.presentation.components.accentColor
import com.example.presentation.components.platformIcon
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.StatusFiltered
import com.example.ui.theme.StatusInfo
import com.example.utils.CurrencyFormatter
import com.example.utils.PermissionHelper
import com.example.utils.PermissionStatusSnapshot

@Composable
fun PlatformsScreen(
    settings: PersistedAppSettings,
    permissionSnapshot: PermissionStatusSnapshot,
    listenerConnected: Boolean,
    onTogglePlatform: (RidePlatform, Boolean) -> Unit,
    onToggleAuthorizedSim: (RidePlatform, Boolean) -> Unit,
    onConfigurePlatformFares: (RidePlatform) -> Unit,
    onOpenPermissions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val configs = listOf(
        settings.uberConfig,
        settings.rapidoConfig,
        settings.olaConfig
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Ride Platforms",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Manage independent monitoring, INR fare preferences, and authorized integration status for Uber Driver, Rapido Captain, and Ola Driver.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(configs, key = { it.platform.id }) { config ->
            val isAppInstalled = when (config.platform) {
                RidePlatform.UBER -> permissionSnapshot.uberInstalled
                RidePlatform.RAPIDO -> permissionSnapshot.rapidoInstalled
                RidePlatform.OLA -> permissionSnapshot.olaInstalled
            }
            val integrationStatus = PermissionHelper.resolvePlatformIntegrationStatus(
                platformEnabled = config.platformEnabled,
                authorizedAdapterSimulated = config.authorizedIntegrationSimulated,
                notificationListenerGranted = permissionSnapshot.notificationListenerGranted,
                listenerServiceConnected = listenerConnected
            )

            PlatformConfigurationCard(
                config = config,
                integrationStatus = integrationStatus,
                isPartnerAppInstalled = isAppInstalled,
                onToggleEnabled = { onTogglePlatform(config.platform, it) },
                onToggleAuthorizedSim = { onToggleAuthorizedSim(config.platform, it) },
                onConfigureFares = { onConfigurePlatformFares(config.platform) },
                onOpenPermissions = onOpenPermissions
            )
        }

        item {
            ComplianceDisclaimerCard()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlatformConfigurationCard(
    config: PlatformFareConfig,
    integrationStatus: PlatformIntegrationStatus,
    isPartnerAppInstalled: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onToggleAuthorizedSim: (Boolean) -> Unit,
    onConfigureFares: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val platform = config.platform
    val accent = platform.accentColor()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("platform_card_${platform.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = if (config.platformEnabled) accent.copy(alpha = 0.55f) else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        color = accent.copy(alpha = 0.16f),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                    ) {
                        Icon(
                            imageVector = platform.platformIcon(),
                            contentDescription = platform.appTitle,
                            tint = accent,
                            modifier = Modifier
                                .padding(10.dp)
                                .size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = platform.appTitle,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Package: ${platform.packageName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Switch(
                    checked = config.platformEnabled,
                    onCheckedChange = onToggleEnabled,
                    modifier = Modifier.testTag("switch_platform_${platform.id}")
                )
            }

            // Integration Status Pill & Details
            val statusColor = when (integrationStatus) {
                PlatformIntegrationStatus.READY_FOR_PERMITTED_MONITORING,
                PlatformIntegrationStatus.CONNECTED_AUTHORIZED -> ElectricLime
                PlatformIntegrationStatus.PERMISSION_REQUIRED,
                PlatformIntegrationStatus.NOTIFICATION_ACCESS_UNAVAILABLE -> StatusFiltered
                PlatformIntegrationStatus.NOT_CONFIGURED,
                PlatformIntegrationStatus.INTEGRATION_UNAVAILABLE -> StatusInfo
            }

            Surface(
                color = statusColor.copy(alpha = 0.12f),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = if (integrationStatus == PlatformIntegrationStatus.PERMISSION_REQUIRED) {
                                Icons.Default.WarningAmber
                            } else {
                                Icons.Default.CheckCircle
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Status: ${integrationStatus.label}",
                            style = MaterialTheme.typography.labelLarge,
                            color = statusColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = integrationStatus.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (isPartnerAppInstalled) {
                            "Device check: ${platform.appTitle} APK detected on device"
                        } else {
                            "Device check: ${platform.appTitle} APK not installed (Demo dispatches still supported)"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Fare Preference Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Configured Fare Window",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = if (config.fareFilterEnabled) {
                            "${CurrencyFormatter.formatInr(config.minimumFareInr)} – ${CurrencyFormatter.formatInr(config.maximumFareInr)}"
                        } else {
                            "All Positive Fares (Filter Off)"
                        },
                        style = MaterialTheme.typography.headlineMedium,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = onConfigureFares,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.testTag("configure_fares_${platform.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Configure")
                }
            }

            // Sandbox Authorized Integration Toggle
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Authorized Partner Dispatch Sandbox",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (config.authorizedIntegrationSimulated) {
                                "Simulating official partner webhook auto-acceptance"
                            } else {
                                "Official auto-accept API unavailable — manual confirmation only"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = config.authorizedIntegrationSimulated,
                        onCheckedChange = onToggleAuthorizedSim
                    )
                }
            }

            if (integrationStatus == PlatformIntegrationStatus.PERMISSION_REQUIRED) {
                OutlinedButton(
                    onClick = onOpenPermissions,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Grant Notification Listener Access")
                }
            }
        }
    }
}

@Composable
private fun ComplianceDisclaimerCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Authorized Integration Policy",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "LOMAP never uses unauthorized Accessibility auto-clicking or private APIs. Notification formats vary across Uber, Rapido, and Ola; missing fields are reported as UNAVAILABLE rather than guessed.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
