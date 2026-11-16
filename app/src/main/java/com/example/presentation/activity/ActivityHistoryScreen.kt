package com.example.presentation.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import com.example.presentation.ActivityFilterState
import com.example.presentation.ActivityHistoryTab
import com.example.presentation.DateRangeFilter
import com.example.presentation.components.RideEventCard

@Composable
fun ActivityHistoryScreen(
    events: List<RideEventRecord>,
    filterState: ActivityFilterState,
    onUpdateFilter: ((ActivityFilterState) -> ActivityFilterState) -> Unit,
    onConfirmRide: (Long) -> Unit,
    onRejectRide: (Long) -> Unit,
    onDeleteEvent: (Long) -> Unit,
    onClearDemoEvents: () -> Unit,
    onClearAllHistory: () -> Unit,
    onOpenDemoLab: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Ride Activity History",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${events.size} matching records in local Room database",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = {
                        onUpdateFilter { it.copy(sortNewestFirst = !it.sortNewestFirst) }
                    },
                    modifier = Modifier.testTag("toggle_sort_order_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SwapVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (filterState.sortNewestFirst) "Newest" else "Oldest")
                }
            }
        }

        // 5 Status Tabs: All, Eligible, Filtered, Confirmed, Failed
        ScrollableTabRow(
            selectedTabIndex = ActivityHistoryTab.entries.indexOf(filterState.selectedTab),
            edgePadding = 0.dp,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            ActivityHistoryTab.entries.forEach { tab ->
                val selected = filterState.selectedTab == tab
                Tab(
                    selected = selected,
                    onClick = { onUpdateFilter { it.copy(selectedTab = tab) } },
                    text = {
                        Text(
                            text = tab.label,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    modifier = Modifier.testTag("activity_tab_${tab.name}")
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = filterState.searchQuery,
            onValueChange = { q -> onUpdateFilter { it.copy(searchQuery = q) } },
            placeholder = { Text("Search pickup, drop, fare (₹), or platform...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (filterState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onUpdateFilter { it.copy(searchQuery = "") } }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("activity_search_input")
        )

        // Platform & Date Filter Chips
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterState.selectedPlatform == null,
                onClick = { onUpdateFilter { it.copy(selectedPlatform = null) } },
                label = { Text("All Platforms") }
            )
            RidePlatform.entries.forEach { platform ->
                FilterChip(
                    selected = filterState.selectedPlatform == platform,
                    onClick = {
                        onUpdateFilter {
                            it.copy(
                                selectedPlatform = if (it.selectedPlatform == platform) null else platform
                            )
                        }
                    },
                    label = { Text(platform.displayName) }
                )
            }

            DateRangeFilter.entries.forEach { dateRange ->
                FilterChip(
                    selected = filterState.dateRangeFilter == dateRange,
                    onClick = { onUpdateFilter { it.copy(dateRangeFilter = dateRange) } },
                    label = { Text(dateRange.label) }
                )
            }
        }

        // History Management Bar
        if (events.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onClearDemoEvents,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_demo_events_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Clear DEMO Only", style = MaterialTheme.typography.labelSmall)
                }
                OutlinedButton(
                    onClick = onClearAllHistory,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("clear_all_history_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Clear All", style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // Event List or Empty State
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No Matching Ride Records",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Adjust your search/filters above or open the Demo Lab to generate sample Uber, Rapido, and Ola events.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            OutlinedButton(
                                onClick = onOpenDemoLab,
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Generate Sample Events in Demo Lab")
                            }
                        }
                    }
                }
            } else {
                items(events, key = { it.id }) { event ->
                    RideEventCard(
                        event = event,
                        onConfirm = onConfirmRide,
                        onReject = onRejectRide,
                        onDelete = onDeleteEvent
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
