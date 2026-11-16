package com.example.presentation.analytics

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import com.example.presentation.DashboardMetrics
import com.example.presentation.components.MetricStatCard
import com.example.presentation.components.accentColor
import com.example.ui.theme.ElectricLime
import com.example.ui.theme.StatusDanger
import com.example.ui.theme.StatusFiltered
import com.example.ui.theme.StatusInfo
import com.example.utils.CurrencyFormatter
import java.util.Locale

@Composable
fun AnalyticsScreen(
    metrics: DashboardMetrics,
    events: List<RideEventRecord>,
    onBack: () -> Unit,
    onOpenDemoLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        text = "Driver Ride Analytics",
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Computed 100% from local Room history — zero arbitrary or fake numbers",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Top KPI Grid
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        label = "Total Detected",
                        value = metrics.totalDetected.toString(),
                        subtitle = "All recorded notifications",
                        accentColor = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Eligible Rides",
                        value = metrics.eligibleCount.toString(),
                        subtitle = "Passed fare & secondary filters",
                        accentColor = ElectricLime,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        label = "Avg Eligible Fare",
                        value = metrics.averageEligibleFareInr?.let { CurrencyFormatter.formatInr(it) } ?: "N/A",
                        subtitle = "Mean of eligible INR rides",
                        accentColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        label = "Acceptance Rate",
                        value = metrics.acceptanceSuccessRatePercent?.let {
                            String.format(Locale.US, "%.0f%%", it)
                        } ?: "N/A",
                        subtitle = "From confirmed/decided outcomes",
                        accentColor = StatusInfo,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (events.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Analytics,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "Insufficient History for Charts",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Analytics charts render strictly from stored Room events. Monitor live notifications or run Demo Mode scenarios to populate platform comparisons and fare distributions.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        OutlinedButton(
                            onClick = onOpenDemoLab,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Open Demo Lab to Generate Events")
                        }
                    }
                }
            }
        } else {
            // Chart 1: Platform Comparison
            item {
                PlatformComparisonChartCard(metrics = metrics)
            }

            // Chart 2: Eligible vs Filtered Breakdown
            item {
                EligibilityBreakdownChartCard(metrics = metrics)
            }

            // Chart 3: Fare Distribution Buckets (INR)
            item {
                FareDistributionChartCard(events = events)
            }

            // Chart 4: Requests Over Time (Recent Sequence)
            item {
                RequestsTimelineChartCard(events = events)
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlatformComparisonChartCard(metrics: DashboardMetrics) {
    val total = metrics.totalDetected.coerceAtLeast(1)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Platform-Wise Request Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            RidePlatform.entries.forEach { platform ->
                val count = metrics.platformCounts[platform] ?: 0
                val fraction = (count.toFloat() / total.toFloat()).coerceIn(0f, 1f)
                val accent = platform.accentColor()

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = platform.appTitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "$count requests (${(fraction * 100).toInt()}%)",
                            style = MaterialTheme.typography.labelSmall,
                            color = accent
                        )
                    }
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                    ) {
                        drawRoundRect(
                            color = accent.copy(alpha = 0.16f),
                            size = size,
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                        drawRoundRect(
                            color = accent,
                            size = Size(width = size.width * fraction, height = size.height),
                            cornerRadius = CornerRadius(12f, 12f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EligibilityBreakdownChartCard(metrics: DashboardMetrics) {
    val total = metrics.totalDetected.coerceAtLeast(1)
    val eligibleRatio = metrics.eligibleCount.toFloat() / total.toFloat()
    val filteredRatio = metrics.filteredCount.toFloat() / total.toFloat()
    val otherRatio = (1f - eligibleRatio - filteredRatio).coerceAtLeast(0f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Eligible vs Filtered Ratio",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(22.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                val w1 = size.width * eligibleRatio
                val w2 = size.width * filteredRatio
                val w3 = size.width * otherRatio

                drawRect(color = ElectricLime, topLeft = Offset(0f, 0f), size = Size(w1, size.height))
                drawRect(color = StatusFiltered, topLeft = Offset(w1, 0f), size = Size(w2, size.height))
                drawRect(color = StatusDanger, topLeft = Offset(w1 + w2, 0f), size = Size(w3, size.height))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendDotItem("Eligible (${metrics.eligibleCount})", ElectricLime)
                LegendDotItem("Filtered (${metrics.filteredCount})", StatusFiltered)
                LegendDotItem("Unknown/Failed (${metrics.failedOrUnknownCount})", StatusDanger)
            }
        }
    }
}

@Composable
private fun LegendDotItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FareDistributionChartCard(events: List<RideEventRecord>) {
    val fares = events.mapNotNull { it.fareInr }
    val bucketUnder100 = fares.count { it < 100.0 }
    val bucket100To250 = fares.count { it in 100.0..250.0 }
    val bucket251To450 = fares.count { it in 250.01..450.0 }
    val bucketOver450 = fares.count { it > 450.0 }
    val maxBucket = maxOf(bucketUnder100, bucket100To250, bucket251To450, bucketOver450, 1)

    val buckets = listOf(
        "< ₹100" to bucketUnder100,
        "₹100–250" to bucket100To250,
        "₹251–450" to bucket251To450,
        "> ₹450" to bucketOver450
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "INR Fare Distribution",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom
            ) {
                buckets.forEach { (label, count) ->
                    val ratio = (count.toFloat() / maxBucket.toFloat()).coerceIn(0.08f, 1f)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height((75 * ratio).dp)
                                .clip(RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RequestsTimelineChartCard(events: List<RideEventRecord>) {
    val chronological = events.take(12).reversed()
    val maxFare = (chronological.mapNotNull { it.fareInr }.maxOrNull() ?: 500.0).coerceAtLeast(100.0)

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
            Text(
                text = "Recent Requests Over Time (Last ${chronological.size})",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Green bars represent eligible fares; amber bars represent filtered or unknown requests.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                val count = chronological.size.coerceAtLeast(1)
                val slotWidth = size.width / count
                val barWidth = (slotWidth * 0.58f).coerceAtMost(42f)

                chronological.forEachIndexed { index, record ->
                    val fare = record.fareInr ?: 60.0
                    val normalizedHeight = ((fare / maxFare).toFloat().coerceIn(0.12f, 1f)) * size.height
                    val left = index * slotWidth + (slotWidth - barWidth) / 2f
                    val top = size.height - normalizedHeight
                    val barColor = if (record.isOverallEligible) ElectricLime else StatusFiltered

                    drawRoundRect(
                        color = barColor,
                        topLeft = Offset(left, top),
                        size = Size(barWidth, normalizedHeight),
                        cornerRadius = CornerRadius(8f, 8f)
                    )
                }
            }
        }
    }
}
