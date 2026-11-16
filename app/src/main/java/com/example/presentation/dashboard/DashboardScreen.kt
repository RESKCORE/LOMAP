package com.example.presentation.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.datastore.PersistedAppSettings
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import com.example.monitoring.processor.DemoScenarioType
import com.example.presentation.DashboardMetrics
import com.example.presentation.components.MetricStatCard
import com.example.presentation.components.MonitoringStatusBadge
import com.example.presentation.components.PlatformBadge
import com.example.presentation.components.RideEventCard
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.OlaAccent
import com.example.ui.theme.RapidoAccent
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusFiltered
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.UberAccent
import com.example.utils.CurrencyFormatter
import com.example.utils.PermissionStatusSnapshot
import java.util.Locale

@Composable
fun DashboardScreen(
    settings: PersistedAppSettings,
    googleUserEmail: String? = null,
    monitoringState: MonitoringServiceState,
    elapsedSeconds: Long,
    listenerConnected: Boolean,
    lastServiceError: String?,
    permissionSnapshot: PermissionStatusSnapshot,
    metrics: DashboardMetrics,
    recentEvents: List<RideEventRecord>,
    lastLatencyMs: Double = 1.2,
    lastProcessedRecord: RideEventRecord? = null,
    onStartMonitoring: () -> Unit,
    onPauseMonitoring: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onConfirmRide: (Long) -> Unit,
    onRejectRide: (Long) -> Unit,
    onSimulateInstantAutoAccept: (RidePlatform) -> Unit = {},
    onConfigurePlatformFares: (RidePlatform) -> Unit = {},
    onQuickDemoScenario: (RidePlatform, DemoScenarioType) -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenAnalytics: () -> Unit,
    onOpenDemoLab: () -> Unit,
    onOpenActivity: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        val isCompactWidth = maxWidth < 360.dp
        val horizontalPad = if (isCompactWidth) 12.dp else 16.dp

        LazyColumn(
            modifier = Modifier
                .widthIn(max = 680.dp)
                .fillMaxSize()
                .padding(horizontal = horizontalPad),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                DashboardHeader(
                    driverName = settings.driverProfile.driverName,
                    city = settings.driverProfile.city,
                    googleUserEmail = googleUserEmail,
                    isCompactWidth = isCompactWidth,
                    onOpenPermissions = onOpenPermissions,
                    onOpenSettings = onOpenSettings
                )
            }

            item {
                MainMonitoringCockpitCard(
                    city = settings.driverProfile.city,
                    monitoringState = monitoringState,
                    elapsedSeconds = elapsedSeconds,
                    listenerConnected = listenerConnected,
                    lastServiceError = lastServiceError,
                    permissionSnapshot = permissionSnapshot,
                    lastLatencyMs = lastLatencyMs,
                    onStartMonitoring = onStartMonitoring,
                    onPauseMonitoring = onPauseMonitoring,
                    onResumeMonitoring = onResumeMonitoring,
                    onStopMonitoring = onStopMonitoring,
                    onOpenPermissions = onOpenPermissions
                )
            }

            item {
                UberMillisecondAutoAcceptCard(
                    settings = settings,
                    lastLatencyMs = lastLatencyMs,
                    lastProcessedRecord = lastProcessedRecord ?: recentEvents.firstOrNull {
                        it.acceptanceOutcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION
                    },
                    onSimulateInstantAutoAccept = onSimulateInstantAutoAccept,
                    onConfigurePlatformFares = onConfigurePlatformFares
                )
            }

            item {
                QuickToolsStrip(
                    onOpenAnalytics = onOpenAnalytics,
                    onOpenDemoLab = onOpenDemoLab,
                    onQuickSimulateEligible = {
                        onSimulateInstantAutoAccept(RidePlatform.UBER)
                    }
                )
            }

            item {
                StatisticsGridSection(metrics = metrics)
            }

            item {
                TodayActivityCard(metrics = metrics, onOpenAnalytics = onOpenAnalytics)
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Live Dispatch Feed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = onOpenActivity) {
                        Text("View All (${metrics.totalDetected})")
                    }
                }
            }

            if (recentEvents.isEmpty()) {
                item {
                    EmptyRecentEventsCard(
                        onTriggerSampleUber = {
                            onSimulateInstantAutoAccept(RidePlatform.UBER)
                        },
                        onTriggerSampleRapido = {
                            onQuickDemoScenario(RidePlatform.RAPIDO, DemoScenarioType.BELOW_MINIMUM)
                        },
                        onOpenDemoLab = onOpenDemoLab
                    )
                }
            } else {
                items(recentEvents.take(6), key = { it.id }) { event ->
                    RideEventCard(
                        event = event,
                        onConfirm = onConfirmRide,
                        onReject = onRejectRide
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DashboardHeader(
    driverName: String,
    city: String,
    googleUserEmail: String?,
    isCompactWidth: Boolean,
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 8.dp else 12.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_launcher_fg_1794828310301),
                contentDescription = "LOMAP emblem",
                modifier = Modifier
                    .size(if (isCompactWidth) 38.dp else 44.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "LOMAP",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        color = ElectricLime.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.VerifiedUser,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "GOOGLE VERIFIED",
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(
                    text = if (!googleUserEmail.isNullOrBlank()) {
                        "$driverName • $city • $googleUserEmail"
                    } else {
                        "Namaste, $driverName • $city"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                IconButton(
                    onClick = onOpenPermissions,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_permissions_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Notifications,
                        contentDescription = "Permissions & Notification Access",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("header_profile_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Driver Profile & Google Account",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun MainMonitoringCockpitCard(
    city: String,
    monitoringState: MonitoringServiceState,
    elapsedSeconds: Long,
    listenerConnected: Boolean,
    lastServiceError: String?,
    permissionSnapshot: PermissionStatusSnapshot,
    lastLatencyMs: Double,
    onStartMonitoring: () -> Unit,
    onPauseMonitoring: () -> Unit,
    onResumeMonitoring: () -> Unit,
    onStopMonitoring: () -> Unit,
    onOpenPermissions: () -> Unit
) {
    val isActive = monitoringState == MonitoringServiceState.ACTIVE
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("main_monitoring_card"),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.5.dp,
            color = if (isActive) {
                ElectricLime.copy(alpha = 0.78f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // Pinterest-Inspired LOMAP Light Vector Map & Radar Viewport
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(156.dp)
                    .background(Color(0xFFE9F1E4))
            ) {
                LomapVectorMapBackdrop(
                    isActive = isActive,
                    modifier = Modifier.fillMaxSize()
                )

                // Top floating glassmorphic pills inside the map viewport
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = Color(0xF2FFFFFF),
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "LOMAP RADAR • ${city.uppercase(Locale.US)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    MonitoringStatusBadge(state = monitoringState)
                }

                // Bottom floating timer & latency bar inside map viewport
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                        .align(Alignment.BottomCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "ACTIVE SHIFT RADAR",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = CurrencyFormatter.formatDurationSeconds(elapsedSeconds),
                            style = MaterialTheme.typography.displayMedium,
                            color = if (isActive) {
                                ElectricLime
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    Surface(
                        color = Color(0xF7FFFFFF),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.5f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format(Locale.US, "%.2f ms AUTO", lastLatencyMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = ElectricLime,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                if (isActive) ElectricLime.copy(alpha = 0.06f) else Color.Transparent,
                                Color.Transparent
                            )
                        )
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = lastServiceError ?: monitoringState.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Foreground Service: ${monitoringState.label}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            val listenerLabel = when {
                                listenerConnected -> "Bound & Auto-Accepting (<3 ms)"
                                permissionSnapshot.notificationListenerGranted -> "Permitted (Ready)"
                                else -> "Access Not Granted"
                            }
                            Text(
                                text = "NotificationListener: $listenerLabel",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (permissionSnapshot.notificationListenerGranted) {
                                    ElectricLime
                                } else {
                                    StatusFiltered
                                },
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        TextButton(onClick = onOpenPermissions) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Access")
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (monitoringState) {
                        MonitoringServiceState.ACTIVE -> {
                            OutlinedButton(
                                onClick = onPauseMonitoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("pause_monitoring_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Pause, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Pause")
                            }
                            Button(
                                onClick = onStopMonitoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_monitoring_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = StatusDanger,
                                    contentColor = MaterialTheme.colorScheme.onError
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Go Offline")
                            }
                        }

                        MonitoringServiceState.PAUSED -> {
                            Button(
                                onClick = onResumeMonitoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("resume_monitoring_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Resume")
                            }
                            OutlinedButton(
                                onClick = onStopMonitoring,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("stop_monitoring_button"),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop")
                            }
                        }

                        else -> {
                            Button(
                                onClick = onStartMonitoring,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("start_monitoring_button"),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "GO ONLINE • START LOMAP",
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Custom Canvas vector map & radar visualization inspired by modern mobility UI pins ("LOMAP").
 */
@Composable
private fun LomapVectorMapBackdrop(
    isActive: Boolean,
    modifier: Modifier = Modifier
) {
    val accent = ElectricLime
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Subtle urban map street grid
        val gridColor = Color(0xFFD5E2CF)
        val stepX = w / 7f
        val stepY = h / 4f
        for (i in 1..6) {
            drawLine(
                color = gridColor,
                start = Offset(i * stepX, 0f),
                end = Offset(i * stepX - 18f, h),
                strokeWidth = 1.5f
            )
        }
        for (j in 1..3) {
            drawLine(
                color = gridColor,
                start = Offset(0f, j * stepY),
                end = Offset(w, j * stepY + 10f),
                strokeWidth = 1.5f
            )
        }

        // Secondary arterial road
        val arterialPath = Path().apply {
            moveTo(0f, h * 0.78f)
            cubicTo(w * 0.3f, h * 0.65f, w * 0.6f, h * 0.85f, w, h * 0.32f)
        }
        drawPath(
            path = arterialPath,
            color = Color(0xFFC3D6BC),
            style = Stroke(width = 5f, cap = StrokeCap.Round)
        )

        // Active LOMAP dispatch route spline
        val routePath = Path().apply {
            moveTo(w * 0.18f, h * 0.68f)
            cubicTo(w * 0.42f, h * 0.25f, w * 0.58f, h * 0.72f, w * 0.82f, h * 0.36f)
        }
        drawPath(
            path = routePath,
            color = accent.copy(alpha = if (isActive) 0.28f else 0.14f),
            style = Stroke(width = 12f, cap = StrokeCap.Round)
        )
        drawPath(
            path = routePath,
            color = if (isActive) accent else accent.copy(alpha = 0.55f),
            style = Stroke(width = 3.5f, cap = StrokeCap.Round)
        )

        // Concentric radar rings around center node
        val center = Offset(w * 0.5f, h * 0.5f)
        drawCircle(
            color = accent.copy(alpha = if (isActive) 0.18f else 0.08f),
            radius = h * 0.42f,
            center = center,
            style = Stroke(width = 1.5f)
        )
        drawCircle(
            color = accent.copy(alpha = if (isActive) 0.12f else 0.05f),
            radius = h * 0.24f,
            center = center,
            style = Stroke(width = 1.5f)
        )

        // Pickup & dropoff nodes
        val pickup = Offset(w * 0.18f, h * 0.68f)
        val dropoff = Offset(w * 0.82f, h * 0.36f)
        drawCircle(color = accent.copy(alpha = 0.3f), radius = 12f, center = pickup)
        drawCircle(color = accent, radius = 5.5f, center = pickup)

        drawCircle(color = UberAccent.copy(alpha = 0.3f), radius = 12f, center = dropoff)
        drawCircle(color = UberAccent, radius = 5.5f, center = dropoff)
    }
}

@Composable
private fun UberMillisecondAutoAcceptCard(
    settings: PersistedAppSettings,
    lastLatencyMs: Double,
    lastProcessedRecord: RideEventRecord?,
    onSimulateInstantAutoAccept: (RidePlatform) -> Unit,
    onConfigurePlatformFares: (RidePlatform) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("millisecond_auto_accept_card"),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        color = ElectricLime.copy(alpha = 0.16f),
                        shape = CircleShape,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Bolt,
                                contentDescription = null,
                                tint = ElectricLime,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "LOMAP Millisecond Auto-Accept",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-accepts rides in your INR range within milliseconds",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = ElectricLime.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = String.format(Locale.US, "⚡ %.2f ms", lastLatencyMs),
                        style = MaterialTheme.typography.labelMedium,
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                RidePlatform.entries.forEach { platform ->
                    val cfg = settings.configFor(platform)
                    val brandColor = when (platform) {
                        RidePlatform.UBER -> UberAccent
                        RidePlatform.RAPIDO -> RapidoAccent
                        RidePlatform.OLA -> OlaAccent
                    }
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onConfigurePlatformFares(platform) }
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(brandColor)
                                    )
                                    Text(
                                        text = platform.appTitle,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = "Edit ${platform.displayName} fare range",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "Range: ₹${cfg.minimumFareInr.toInt()} – ₹${cfg.maximumFareInr.toInt()} • ${if (cfg.authorizedIntegrationSimulated) "Auto-Accept ON" else "Manual"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cfg.authorizedIntegrationSimulated) ElectricLime else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { onSimulateInstantAutoAccept(platform) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("instant_auto_accept_${platform.name}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Bolt,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Test ₹${((cfg.minimumFareInr + cfg.maximumFareInr) / 2).toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            if (lastProcessedRecord != null &&
                lastProcessedRecord.acceptanceOutcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION
            ) {
                Surface(
                    color = ElectricLime.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, ElectricLime.copy(alpha = 0.45f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ElectricLime,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PlatformBadge(platform = lastProcessedRecord.platform)
                                Text(
                                    text = lastProcessedRecord.fareInr?.let { CurrencyFormatter.formatInr(it) } ?: "In-Range",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = ElectricLime,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Text(
                                text = lastProcessedRecord.statusDetail,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickToolsStrip(
    onOpenAnalytics: () -> Unit,
    onOpenDemoLab: () -> Unit,
    onQuickSimulateEligible: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledTonalButton(
            onClick = onOpenDemoLab,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("open_demo_lab_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.BugReport,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Demo Lab (12)",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        FilledTonalButton(
            onClick = onOpenAnalytics,
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .testTag("open_analytics_button"),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Analytics,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Analytics",
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatisticsGridSection(metrics: DashboardMetrics) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                label = "Requests Detected",
                value = metrics.totalDetected.toString(),
                subtitle = "Total parsed dispatches",
                accentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                label = "Eligible Requests",
                value = metrics.eligibleCount.toString(),
                subtitle = "In selected INR range",
                accentColor = ElectricLime,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricStatCard(
                label = "Filtered Out",
                value = metrics.filteredCount.toString(),
                subtitle = "Outside min/max range",
                accentColor = StatusFiltered,
                modifier = Modifier.weight(1f)
            )
            MetricStatCard(
                label = "Auto-Accepted",
                value = metrics.driverConfirmedCount.toString(),
                subtitle = "Accepted in < 3 ms",
                accentColor = StatusInfo,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TodayActivityCard(
    metrics: DashboardMetrics,
    onOpenAnalytics: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's LOMAP Summary",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onOpenAnalytics) {
                    Text("Charts →")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Detected Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${metrics.todayDetected} rides",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eligible Today",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${metrics.todayEligible} rides",
                        style = MaterialTheme.typography.titleLarge,
                        color = ElectricLime,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "Avg Eligible Fare",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = metrics.averageEligibleFareInr?.let { CurrencyFormatter.formatInr(it) } ?: "—",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyRecentEventsCard(
    onTriggerSampleUber: () -> Unit,
    onTriggerSampleRapido: () -> Unit,
    onOpenDemoLab: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Radar,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(38.dp)
            )
            Text(
                text = "No Ride Events Recorded Yet",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Go Online to capture live Uber, Rapido, and Ola notifications, or test LOMAP millisecond auto-acceptance immediately with a sample in-range dispatch.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onTriggerSampleUber,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_demo_uber_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto-Accept Test")
                }
                OutlinedButton(
                    onClick = onTriggerSampleRapido,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("empty_demo_rapido_button"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Filtered Test")
                }
            }
            TextButton(onClick = onOpenDemoLab) {
                Text("Open Full 12-Scenario Demo & Diagnostics Lab")
            }
        }
    }
}
