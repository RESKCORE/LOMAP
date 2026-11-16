package com.example.presentation.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.RidePlatform
import com.example.presentation.components.accentColor
import com.example.presentation.components.platformIcon
import com.example.ui.theme.ElectricLime
import com.example.utils.CurrencyFormatter
import com.example.utils.PermissionHelper

@Composable
fun OnboardingScreen(
    onFinishOnboarding: (
        selectedPlatforms: Set<RidePlatform>,
        uberMin: Double,
        uberMax: Double,
        rapidoMin: Double,
        rapidoMax: Double,
        olaMin: Double,
        olaMax: Double
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentStep by rememberSaveable { mutableIntStateOf(1) }

    var uberSelected by rememberSaveable { mutableStateOf(true) }
    var rapidoSelected by rememberSaveable { mutableStateOf(true) }
    var olaSelected by rememberSaveable { mutableStateOf(true) }

    var uberMinText by rememberSaveable { mutableStateOf("100") }
    var uberMaxText by rememberSaveable { mutableStateOf("500") }
    var rapidoMinText by rememberSaveable { mutableStateOf("50") }
    var rapidoMaxText by rememberSaveable { mutableStateOf("300") }
    var olaMinText by rememberSaveable { mutableStateOf("100") }
    var olaMaxText by rememberSaveable { mutableStateOf("600") }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Updated on resume/check */ }
    )

    val selectedSet = remember(uberSelected, rapidoSelected, olaSelected) {
        buildSet {
            if (uberSelected) add(RidePlatform.UBER)
            if (rapidoSelected) add(RidePlatform.RAPIDO)
            if (olaSelected) add(RidePlatform.OLA)
        }
    }

    val uberMin = uberMinText.toDoubleOrNull() ?: 100.0
    val uberMax = uberMaxText.toDoubleOrNull() ?: 500.0
    val rapidoMin = rapidoMinText.toDoubleOrNull() ?: 50.0
    val rapidoMax = rapidoMaxText.toDoubleOrNull() ?: 300.0
    val olaMin = olaMinText.toDoubleOrNull() ?: 100.0
    val olaMax = olaMaxText.toDoubleOrNull() ?: 600.0

    val fareStepValid = uberMin >= 0 && uberMax > uberMin &&
        rapidoMin >= 0 && rapidoMax > rapidoMin &&
        olaMin >= 0 && olaMax > olaMin

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(20.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Step Progress Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = Icons.Default.Radar,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp)
                    )
                }
                Text(
                    text = "RIDEPILOT SETUP",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                text = "Step $currentStep of 5",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Step Progress Indicator Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (step in 1..5) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            if (step <= currentStep) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant
                        )
                )
            }
        }

        // Main Animated Step Content
        AnimatedContent(
            targetState = currentStep,
            label = "onboarding_step",
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (step) {
                    1 -> WelcomeStepContent()
                    2 -> SelectPlatformsStepContent(
                        uberSelected = uberSelected,
                        onUberToggle = { uberSelected = it },
                        rapidoSelected = rapidoSelected,
                        onRapidoToggle = { rapidoSelected = it },
                        olaSelected = olaSelected,
                        onOlaToggle = { olaSelected = it }
                    )
                    3 -> ConfigureFaresStepContent(
                        uberMinText = uberMinText,
                        onUberMinChange = { uberMinText = it },
                        uberMaxText = uberMaxText,
                        onUberMaxChange = { uberMaxText = it },
                        rapidoMinText = rapidoMinText,
                        onRapidoMinChange = { rapidoMinText = it },
                        rapidoMaxText = rapidoMaxText,
                        onRapidoMaxChange = { rapidoMaxText = it },
                        olaMinText = olaMinText,
                        onOlaMinChange = { olaMinText = it },
                        olaMaxText = olaMaxText,
                        onOlaMaxChange = { olaMaxText = it },
                        isValid = fareStepValid
                    )
                    4 -> PermissionsIntroStepContent(
                        onOpenListenerSettings = {
                            runCatching {
                                context.startActivity(PermissionHelper.buildNotificationListenerSettingsIntent())
                            }
                        },
                        onRequestPostNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    )
                    5 -> CompleteSetupStepContent(
                        selectedPlatforms = selectedSet,
                        uberMin = uberMin,
                        uberMax = uberMax,
                        rapidoMin = rapidoMin,
                        rapidoMax = rapidoMax,
                        olaMin = olaMin,
                        olaMax = olaMax
                    )
                }
            }
        }

        // Bottom Navigation Controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (currentStep > 1) {
                OutlinedButton(
                    onClick = { currentStep -= 1 },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .testTag("onboarding_back_button"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Back")
                }
            }

            Button(
                onClick = {
                    if (currentStep < 5) {
                        currentStep += 1
                    } else {
                        onFinishOnboarding(
                            selectedSet.ifEmpty { setOf(RidePlatform.UBER) },
                            uberMin,
                            uberMax,
                            rapidoMin,
                            rapidoMax,
                            olaMin,
                            olaMax
                        )
                    }
                },
                enabled = when (currentStep) {
                    2 -> selectedSet.isNotEmpty()
                    3 -> fareStepValid
                    else -> true
                },
                modifier = Modifier
                    .weight(if (currentStep > 1) 1.5f else 1f)
                    .height(52.dp)
                    .testTag("onboarding_next_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = when (currentStep) {
                        1 -> "Get Started"
                        5 -> "Launch LOMAP Cockpit"
                        else -> "Continue"
                    },
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
            }
        }
    }
}

@Composable
private fun WelcomeStepContent() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Box(modifier = Modifier.height(190.dp)) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_banner_1794828310356),
                contentDescription = "LOMAP urban telemetry map",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Surface(
                color = MaterialTheme.colorScheme.background.copy(alpha = 0.78f),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(14.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_launcher_fg_1794828310301),
                        contentDescription = "LOMAP Logo",
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column {
                        Text(
                            text = "LOMAP India",
                            style = MaterialTheme.typography.titleMedium,
                            color = ElectricLime,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Uber Driver • Rapido Captain • Ola Driver",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    Text(
        text = "Smart Multi-Platform Ride Filtering for Indian Drivers",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )

    Text(
        text = "LOMAP monitors incoming ride dispatch notifications across Uber Driver, Rapido Captain, and Ola Driver, evaluates INR fares against your custom thresholds in real time, and auto-accepts profitable trips in milliseconds.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeatureHighlightRow(
                icon = Icons.Default.Tune,
                title = "Independent INR Fare Ranges",
                desc = "Set custom minimum and maximum ₹ thresholds separately for Uber, Rapido, and Ola."
            )
            FeatureHighlightRow(
                icon = Icons.Default.NotificationsActive,
                title = "Real-Time Dispatch Evaluation",
                desc = "Instant alerts for eligible fares, plus distance, ₹/km rate, and area filtering."
            )
            FeatureHighlightRow(
                icon = Icons.Default.Security,
                title = "100% Authorized & Transparent",
                desc = "Zero unauthorized screen tapping. All ride history stays private in your local database."
            )
        }
    }
}

@Composable
private fun FeatureHighlightRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    desc: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(8.dp)
                    .size(20.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = desc,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SelectPlatformsStepContent(
    uberSelected: Boolean,
    onUberToggle: (Boolean) -> Unit,
    rapidoSelected: Boolean,
    onRapidoToggle: (Boolean) -> Unit,
    olaSelected: Boolean,
    onOlaToggle: (Boolean) -> Unit
) {
    Text(
        text = "Select Your Active Driver Platforms",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Choose which driver apps you use on the road. You can toggle or reconfigure these anytime.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    SelectablePlatformCard(
        platform = RidePlatform.UBER,
        selected = uberSelected,
        onToggle = onUberToggle
    )
    SelectablePlatformCard(
        platform = RidePlatform.RAPIDO,
        selected = rapidoSelected,
        onToggle = onRapidoToggle
    )
    SelectablePlatformCard(
        platform = RidePlatform.OLA,
        selected = olaSelected,
        onToggle = onOlaToggle
    )
}

@Composable
private fun SelectablePlatformCard(
    platform: RidePlatform,
    selected: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val accent = platform.accentColor()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle(!selected) }
            .testTag("onboarding_platform_${platform.id}"),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                accent.copy(alpha = 0.10f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outline
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = accent.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(14.dp)
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
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = platform.vehicleTypes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Default range: ₹${platform.defaultMinFareInr.toInt()} – ₹${platform.defaultMaxFareInr.toInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent
                    )
                }
            }
            Checkbox(
                checked = selected,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun ConfigureFaresStepContent(
    uberMinText: String,
    onUberMinChange: (String) -> Unit,
    uberMaxText: String,
    onUberMaxChange: (String) -> Unit,
    rapidoMinText: String,
    onRapidoMinChange: (String) -> Unit,
    rapidoMaxText: String,
    onRapidoMaxChange: (String) -> Unit,
    olaMinText: String,
    onOlaMinChange: (String) -> Unit,
    olaMaxText: String,
    onOlaMaxChange: (String) -> Unit,
    isValid: Boolean
) {
    Text(
        text = "Configure Initial INR Fare Preferences",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "Set your preferred minimum and maximum ride fares (₹). Rides inside this inclusive window are marked ELIGIBLE.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    OnboardingFareRangeCard(
        platform = RidePlatform.UBER,
        minText = uberMinText,
        onMinChange = onUberMinChange,
        maxText = uberMaxText,
        onMaxChange = onUberMaxChange
    )
    OnboardingFareRangeCard(
        platform = RidePlatform.RAPIDO,
        minText = rapidoMinText,
        onMinChange = onRapidoMinChange,
        maxText = rapidoMaxText,
        onMaxChange = onRapidoMaxChange
    )
    OnboardingFareRangeCard(
        platform = RidePlatform.OLA,
        minText = olaMinText,
        onMinChange = onOlaMinChange,
        maxText = olaMaxText,
        onMaxChange = onOlaMaxChange
    )

    if (!isValid) {
        Text(
            text = "Ensure all minimum fares are ≥ ₹0 and every maximum fare is greater than its minimum fare.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
private fun OnboardingFareRangeCard(
    platform: RidePlatform,
    minText: String,
    onMinChange: (String) -> Unit,
    maxText: String,
    onMaxChange: (String) -> Unit
) {
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
            Text(
                text = "${platform.appTitle} (INR ₹)",
                style = MaterialTheme.typography.titleMedium,
                color = platform.accentColor(),
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = minText,
                    onValueChange = onMinChange,
                    label = { Text("Min ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = maxText,
                    onValueChange = onMaxChange,
                    label = { Text("Max ₹") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PermissionsIntroStepContent(
    onOpenListenerSettings: () -> Unit,
    onRequestPostNotification: () -> Unit
) {
    Text(
        text = "Transparent Permission Architecture",
        style = MaterialTheme.typography.headlineLarge,
        color = MaterialTheme.colorScheme.onBackground
    )
    Text(
        text = "LOMAP never requests permissions without explaining why they are needed. You can grant them now or later from the Permissions screen.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    PermissionExplanationCard(
        title = "1. Notification Listener Access",
        purpose = "Required to observe incoming dispatch notifications from Uber Driver, Rapido Captain, and Ola Driver. Unrelated apps, OTPs, and personal messages are strictly ignored.",
        actionLabel = "Open Notification Access Settings",
        onAction = onOpenListenerSettings
    )

    PermissionExplanationCard(
        title = "2. Post Notifications Permission",
        purpose = "Required on Android 13+ so LOMAP can alert you immediately when an eligible ride fare is detected and display its active monitoring indicator.",
        actionLabel = "Grant Notification Permission",
        onAction = onRequestPostNotification
    )

    PermissionExplanationCard(
        title = "3. Foreground Service Execution",
        purpose = "Ensures Android keeps LOMAP's fare evaluator active while you are driving, with a visible status bar notification you can stop anytime.",
        actionLabel = null,
        onAction = null
    )
}

@Composable
private fun PermissionExplanationCard(
    title: String,
    purpose: String,
    actionLabel: String?,
    onAction: (() -> Unit)?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = purpose,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (actionLabel != null && onAction != null) {
                OutlinedButton(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun CompleteSetupStepContent(
    selectedPlatforms: Set<RidePlatform>,
    uberMin: Double,
    uberMax: Double,
    rapidoMin: Double,
    rapidoMax: Double,
    olaMin: Double,
    olaMax: Double
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = ElectricLime,
            modifier = Modifier.size(32.dp)
        )
        Text(
            text = "Ready to Roll",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }

    Text(
        text = "Review your selected platforms and INR fare filters below. Your preferences will be persisted locally in DataStore.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            SummaryPlatformRow(
                platform = RidePlatform.UBER,
                enabled = RidePlatform.UBER in selectedPlatforms,
                min = uberMin,
                max = uberMax
            )
            SummaryPlatformRow(
                platform = RidePlatform.RAPIDO,
                enabled = RidePlatform.RAPIDO in selectedPlatforms,
                min = rapidoMin,
                max = rapidoMax
            )
            SummaryPlatformRow(
                platform = RidePlatform.OLA,
                enabled = RidePlatform.OLA in selectedPlatforms,
                min = olaMin,
                max = olaMax
            )
        }
    }
}

@Composable
private fun SummaryPlatformRow(
    platform: RidePlatform,
    enabled: Boolean,
    min: Double,
    max: Double
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = platform.appTitle,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = if (enabled) "Monitoring Enabled" else "Disabled",
                style = MaterialTheme.typography.bodySmall,
                color = if (enabled) ElectricLime else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = "${CurrencyFormatter.formatInr(min)} – ${CurrencyFormatter.formatInr(max)}",
            style = MaterialTheme.typography.labelLarge,
            color = platform.accentColor(),
            fontWeight = FontWeight.Bold
        )
    }
}
