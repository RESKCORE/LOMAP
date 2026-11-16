package com.example.presentation.fares

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.datastore.PersistedAppSettings
import com.example.domain.evaluator.FareEvaluator
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RidePlatform
import com.example.presentation.components.EligibilityPill
import com.example.presentation.components.accentColor
import com.example.presentation.components.platformIcon
import com.example.utils.CurrencyFormatter

@Composable
fun FareSettingsScreen(
    settings: PersistedAppSettings,
    initialSelectedPlatform: RidePlatform? = null,
    onSavePlatformConfig: (PlatformFareConfig, (Boolean, String) -> Unit) -> Unit,
    onResetPlatformDefaults: (RidePlatform) -> Unit,
    onSaveAdditionalFilters: (AdditionalRideFilters) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedPlatform by rememberSaveable {
        mutableStateOf(initialSelectedPlatform ?: RidePlatform.UBER)
    }

    LaunchedEffect(initialSelectedPlatform) {
        if (initialSelectedPlatform != null) {
            selectedPlatform = initialSelectedPlatform
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Fare Filtering Engine",
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Configure independent minimum and maximum ride fares (₹ INR) for each platform plus secondary distance, ₹/km rate, and locality filters.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Platform Selector Tabs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RidePlatform.entries.forEach { platform ->
                    val isSelected = selectedPlatform == platform
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPlatform = platform },
                        label = {
                            Text(
                                text = platform.displayName,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = platform.platformIcon(),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("fare_tab_${platform.id}")
                    )
                }
            }
        }

        // Active Platform Fare Editor Card
        item {
            val activeConfig = settings.configFor(selectedPlatform)
            PlatformFareEditorCard(
                config = activeConfig,
                onSave = onSavePlatformConfig,
                onReset = { onResetPlatformDefaults(selectedPlatform) }
            )
        }

        // All Platforms Quick Comparison Table
        item {
            AllPlatformsFareComparisonCard(settings = settings)
        }

        // Additional Ride Filters Editor Card
        item {
            AdditionalFiltersEditorCard(
                filters = settings.additionalFilters,
                onSave = onSaveAdditionalFilters
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PlatformFareEditorCard(
    config: PlatformFareConfig,
    onSave: (PlatformFareConfig, (Boolean, String) -> Unit) -> Unit,
    onReset: () -> Unit
) {
    val platform = config.platform
    val accent = platform.accentColor()
    val fareEvaluator = remember { FareEvaluator() }

    var filterEnabled by remember(config) { mutableStateOf(config.fareFilterEnabled) }
    var minText by remember(config) { mutableStateOf(config.minimumFareInr.toInt().toString()) }
    var maxText by remember(config) { mutableStateOf(config.maximumFareInr.toInt().toString()) }
    var testFareText by remember(config.platform) {
        mutableStateOf(((config.minimumFareInr + config.maximumFareInr) / 2).toInt().toString())
    }
    var validationMessage by remember(config) { mutableStateOf<String?>(null) }

    val minVal = minText.toDoubleOrNull() ?: Double.NaN
    val maxVal = maxText.toDoubleOrNull() ?: Double.NaN

    val draftConfig = PlatformFareConfig(
        platform = platform,
        platformEnabled = config.platformEnabled,
        fareFilterEnabled = filterEnabled,
        minimumFareInr = minVal,
        maximumFareInr = maxVal,
        authorizedIntegrationSimulated = config.authorizedIntegrationSimulated
    )
    val validation = draftConfig.validate()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("fare_editor_card_${platform.id}"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f))
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
                Column {
                    Text(
                        text = "${platform.appTitle} Fare Filter",
                        style = MaterialTheme.typography.titleLarge,
                        color = accent,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Inclusive bounds: rideFare >= Min && rideFare <= Max",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = filterEnabled,
                    onCheckedChange = { filterEnabled = it },
                    modifier = Modifier.testTag("switch_fare_filter_${platform.id}")
                )
            }

            // Minimum Fare Row with +/- ₹10 Stepper
            FareInputWithStepper(
                label = "Minimum Fare (₹ INR)",
                valueText = minText,
                onValueChange = {
                    minText = it
                    validationMessage = null
                },
                onStepDelta = { delta ->
                    val current = minText.toDoubleOrNull() ?: platform.defaultMinFareInr
                    val updated = (current + delta).coerceAtLeast(0.0)
                    minText = updated.toInt().toString()
                },
                testTagPrefix = "min_fare_${platform.id}"
            )

            // Maximum Fare Row with +/- ₹25 Stepper
            FareInputWithStepper(
                label = "Maximum Fare (₹ INR)",
                valueText = maxText,
                onValueChange = {
                    maxText = it
                    validationMessage = null
                },
                onStepDelta = { delta ->
                    val current = maxText.toDoubleOrNull() ?: platform.defaultMaxFareInr
                    val updated = (current + delta).coerceAtLeast(10.0)
                    maxText = updated.toInt().toString()
                },
                testTagPrefix = "max_fare_${platform.id}"
            )

            if (!validation.isValid) {
                Surface(
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = validation.errorMessage ?: "Invalid configuration",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .testTag("fare_validation_error")
                    )
                }
            } else if (validationMessage != null) {
                Text(
                    text = validationMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Action Buttons: Save & Reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("reset_fare_${platform.id}"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset (₹${platform.defaultMinFareInr.toInt()}–₹${platform.defaultMaxFareInr.toInt()})")
                }

                Button(
                    onClick = {
                        onSave(draftConfig) { _, msg ->
                            validationMessage = msg
                        }
                    },
                    enabled = validation.isValid,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("save_fare_${platform.id}"),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Range")
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            // Interactive Live FareEvaluator Sandbox
            val sampleFare = testFareText.toDoubleOrNull()
            val liveEvalResult = fareEvaluator.evaluateRawFare(sampleFare, draftConfig)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Science,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Live FareEvaluator Check",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                EligibilityPill(
                    result = liveEvalResult,
                    isOverallEligible = liveEvalResult.isEligible
                )
            }

            OutlinedTextField(
                value = testFareText,
                onValueChange = { testFareText = it },
                label = { Text("Test Sample Ride Fare (₹ INR — leave blank for Unknown Fare)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("live_test_fare_input")
            )
        }
    }
}

@Composable
private fun FareInputWithStepper(
    label: String,
    valueText: String,
    onValueChange: (String) -> Unit,
    onStepDelta: (Double) -> Unit,
    testTagPrefix: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalIconButton(
                onClick = { onStepDelta(-10.0) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTagPrefix}_dec")
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease by ₹10")
            }

            OutlinedTextField(
                value = valueText,
                onValueChange = onValueChange,
                prefix = { Text("₹ ") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("${testTagPrefix}_input")
            )

            FilledTonalIconButton(
                onClick = { onStepDelta(10.0) },
                modifier = Modifier
                    .size(48.dp)
                    .testTag("${testTagPrefix}_inc")
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase by ₹10")
            }
        }
    }
}

@Composable
private fun AllPlatformsFareComparisonCard(settings: PersistedAppSettings) {
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
                text = "Active Platform Thresholds Matrix",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )

            listOf(settings.uberConfig, settings.rapidoConfig, settings.olaConfig).forEach { cfg ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = cfg.platform.appTitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cfg.platform.accentColor(),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = if (!cfg.platformEnabled) {
                            "Platform Disabled"
                        } else if (!cfg.fareFilterEnabled) {
                            "Filter Off (All Fares)"
                        } else {
                            "${CurrencyFormatter.formatInr(cfg.minimumFareInr)} – ${CurrencyFormatter.formatInr(cfg.maximumFareInr)}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalFiltersEditorCard(
    filters: AdditionalRideFilters,
    onSave: (AdditionalRideFilters) -> Unit
) {
    var minDistEnabled by remember(filters) { mutableStateOf(filters.minDistanceEnabled) }
    var minDistText by remember(filters) { mutableStateOf(filters.minDistanceKm.toString()) }

    var maxDistEnabled by remember(filters) { mutableStateOf(filters.maxDistanceEnabled) }
    var maxDistText by remember(filters) { mutableStateOf(filters.maxDistanceKm.toString()) }

    var minRateEnabled by remember(filters) { mutableStateOf(filters.minFarePerKmEnabled) }
    var minRateText by remember(filters) { mutableStateOf(filters.minFarePerKmInr.toString()) }

    var pickupEnabled by remember(filters) { mutableStateOf(filters.preferredPickupEnabled) }
    var pickupText by remember(filters) {
        mutableStateOf(filters.preferredPickupAreas.joinToString(", "))
    }

    var destEnabled by remember(filters) { mutableStateOf(filters.preferredDestinationEnabled) }
    var destText by remember(filters) {
        mutableStateOf(filters.preferredDestinationAreas.joinToString(", "))
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column {
                    Text(
                        text = "Additional Ride Filters",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Each filter works independently. If a filter is enabled and notification telemetry is missing, the condition is marked UNAVAILABLE (never guessed).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 1. Min Distance
            FilterToggleInputRow(
                title = "Minimum Trip Distance (km)",
                checked = minDistEnabled,
                onCheckedChange = { minDistEnabled = it },
                value = minDistText,
                onValueChange = { minDistText = it },
                keyboardType = KeyboardType.Decimal
            )

            // 2. Max Distance
            FilterToggleInputRow(
                title = "Maximum Trip Distance (km)",
                checked = maxDistEnabled,
                onCheckedChange = { maxDistEnabled = it },
                value = maxDistText,
                onValueChange = { maxDistText = it },
                keyboardType = KeyboardType.Decimal
            )

            // 3. Min Fare per km
            FilterToggleInputRow(
                title = "Minimum Estimated Fare / km (₹/km)",
                checked = minRateEnabled,
                onCheckedChange = { minRateEnabled = it },
                value = minRateText,
                onValueChange = { minRateText = it },
                keyboardType = KeyboardType.Decimal
            )

            // 4. Preferred Pickup Locations
            FilterToggleInputRow(
                title = "Preferred Pickup Locations (comma-separated)",
                checked = pickupEnabled,
                onCheckedChange = { pickupEnabled = it },
                value = pickupText,
                onValueChange = { pickupText = it },
                keyboardType = KeyboardType.Text
            )

            // 5. Preferred Destination Areas
            FilterToggleInputRow(
                title = "Preferred Destination Areas (comma-separated)",
                checked = destEnabled,
                onCheckedChange = { destEnabled = it },
                value = destText,
                onValueChange = { destText = it },
                keyboardType = KeyboardType.Text
            )

            Button(
                onClick = {
                    val updated = AdditionalRideFilters(
                        minDistanceEnabled = minDistEnabled,
                        minDistanceKm = minDistText.toDoubleOrNull() ?: 2.0,
                        maxDistanceEnabled = maxDistEnabled,
                        maxDistanceKm = maxDistText.toDoubleOrNull() ?: 25.0,
                        minFarePerKmEnabled = minRateEnabled,
                        minFarePerKmInr = minRateText.toDoubleOrNull() ?: 15.0,
                        preferredPickupEnabled = pickupEnabled,
                        preferredPickupAreas = pickupText.split(",").map { it.trim() }.filter { it.isNotEmpty() },
                        preferredDestinationEnabled = destEnabled,
                        preferredDestinationAreas = destText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    )
                    onSave(updated)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_additional_filters_button"),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Save Secondary Filters")
            }
        }
    }
}

@Composable
private fun FilterToggleInputRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    value: String,
    onValueChange: (String) -> Unit,
    keyboardType: KeyboardType
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
        if (checked) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
