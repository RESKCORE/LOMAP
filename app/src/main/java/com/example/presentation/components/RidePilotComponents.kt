package com.example.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalTaxi
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RideInfoAvailability
import com.example.domain.model.RidePlatform
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.OlaAccent
import com.example.ui.theme.RapidoAccent
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusEligible
import com.example.ui.theme.StatusFiltered
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.UberAccent
import com.example.utils.CurrencyFormatter

fun RidePlatform.accentColor(): Color = when (this) {
    RidePlatform.UBER -> UberAccent
    RidePlatform.RAPIDO -> RapidoAccent
    RidePlatform.OLA -> OlaAccent
}

fun RidePlatform.platformIcon(): ImageVector = when (this) {
    RidePlatform.UBER -> Icons.Default.DirectionsCar
    RidePlatform.RAPIDO -> Icons.AutoMirrored.Filled.DirectionsBike
    RidePlatform.OLA -> Icons.Default.LocalTaxi
}

@Composable
fun PlatformBadge(
    platform: RidePlatform,
    isDemo: Boolean = false,
    modifier: Modifier = Modifier
) {
    val accent = platform.accentColor()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = accent.copy(alpha = 0.14f),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, accent.copy(alpha = 0.45f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = platform.platformIcon(),
                    contentDescription = platform.displayName,
                    tint = accent,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = platform.displayName.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (isDemo) {
            Surface(
                color = StatusFiltered.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, StatusFiltered.copy(alpha = 0.6f))
            ) {
                Text(
                    text = "DEMO",
                    style = MaterialTheme.typography.labelSmall,
                    color = StatusFiltered,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun EligibilityPill(
    result: FareEvaluationResult,
    isOverallEligible: Boolean,
    modifier: Modifier = Modifier
) {
    val (color, text) = when {
        isOverallEligible -> StatusEligible to "ELIGIBLE"
        result == FareEvaluationResult.BELOW_MINIMUM -> StatusFiltered to "BELOW MIN"
        result == FareEvaluationResult.ABOVE_MAXIMUM -> StatusFiltered to "ABOVE MAX"
        result == FareEvaluationResult.UNKNOWN_FARE -> StatusInfo to "UNKNOWN FARE"
        result == FareEvaluationResult.PLATFORM_DISABLED -> StatusDanger to "DISABLED"
        result == FareEvaluationResult.INVALID_CONFIGURATION -> StatusDanger to "INVALID CFG"
        else -> StatusFiltered to "FILTERED"
    }

    Surface(
        modifier = modifier,
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.45f))
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun MonitoringStatusBadge(
    state: MonitoringServiceState,
    modifier: Modifier = Modifier
) {
    val dotColor = when (state) {
        MonitoringServiceState.ACTIVE -> ElectricLime
        MonitoringServiceState.STARTING,
        MonitoringServiceState.PAUSED -> StatusFiltered
        MonitoringServiceState.PERMISSION_REQUIRED,
        MonitoringServiceState.SERVICE_ERROR -> StatusDanger
        MonitoringServiceState.INACTIVE -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        color = dotColor.copy(alpha = 0.14f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Text(
                text = state.label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = dotColor,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun MetricStatCard(
    label: String,
    value: String,
    subtitle: String,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = accentColor,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RideEventCard(
    event: RideEventRecord,
    onConfirm: ((Long) -> Unit)? = null,
    onReject: ((Long) -> Unit)? = null,
    onDelete: ((Long) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ride_event_card_${event.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (event.isOverallEligible) {
                ElectricLime.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outline
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlatformBadge(platform = event.platform, isDemo = event.isDemo)
                EligibilityPill(
                    result = event.fareEvaluationResult,
                    isOverallEligible = event.isOverallEligible
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = when (event.fareAvailability) {
                            RideInfoAvailability.AVAILABLE -> CurrencyFormatter.formatInr(event.fareInr)
                            RideInfoAvailability.UNAVAILABLE -> "Fare Unavailable"
                            RideInfoAvailability.INVALID -> "Invalid Fare Data"
                        },
                        style = MaterialTheme.typography.headlineLarge,
                        color = if (event.isOverallEligible) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyFormatter.formatTimestamp(event.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = "Trip distance",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = when (event.distanceAvailability) {
                                RideInfoAvailability.AVAILABLE -> CurrencyFormatter.formatDistanceKm(event.tripDistanceKm)
                                RideInfoAvailability.UNAVAILABLE -> "Dist Unavailable"
                                RideInfoAvailability.INVALID -> "Dist Invalid"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    if (event.estimatedFarePerKmInr != null) {
                        Text(
                            text = CurrencyFormatter.formatRatePerKm(event.estimatedFarePerKmInr),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Route Pickup / Drop details
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Pickup",
                            tint = ElectricLime,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Pickup: ${event.pickupLocation ?: "Unavailable in notification"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Place,
                            contentDescription = "Drop",
                            tint = StatusInfo,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Drop: ${event.destinationLocation ?: "Unavailable in notification"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Outcome & Status Detail
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${event.acceptanceOutcome.label} • ${event.driverAction}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = event.statusDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (onDelete != null) {
                    IconButton(
                        onClick = { onDelete(event.id) },
                        modifier = Modifier.testTag("delete_event_${event.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete ride record",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Driver Confirmation Action Row when awaiting decision
            val canDriverAct = onConfirm != null && onReject != null &&
                (event.acceptanceOutcome == AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE ||
                    event.acceptanceOutcome == AcceptanceOutcome.UNKNOWN_FARE)

            if (canDriverAct) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { onConfirm?.invoke(event.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("confirm_event_${event.id}"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirm Ride")
                    }

                    OutlinedButton(
                        onClick = { onReject?.invoke(event.id) },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("reject_event_${event.id}"),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Reject")
                    }
                }
            }
        }
    }
}

@Composable
fun FeedbackBanner(
    message: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = !message.isNullOrBlank(),
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
                .clickable { onDismiss() },
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = message.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss notification banner",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
