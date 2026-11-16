package com.example.presentation.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.CurrencyRupee
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.RidePlatform
import com.example.presentation.RidePilotViewModel
import com.example.presentation.activity.ActivityHistoryScreen
import com.example.presentation.analytics.AnalyticsScreen
import com.example.presentation.components.FeedbackBanner
import com.example.presentation.dashboard.DashboardScreen
import com.example.presentation.fares.FareSettingsScreen
import com.example.presentation.onboarding.OnboardingScreen
import com.example.presentation.permissions.DemoDiagnosticsScreen
import com.example.presentation.permissions.PermissionsScreen
import com.example.presentation.platforms.PlatformsScreen
import com.example.presentation.settings.SettingsScreen

enum class PrimaryDestination(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("home", "Home", Icons.Filled.Dashboard, Icons.Outlined.Dashboard),
    PLATFORMS("platforms", "Platforms", Icons.Filled.Layers, Icons.Outlined.Layers),
    FARES("fares", "Fares", Icons.Filled.CurrencyRupee, Icons.Outlined.CurrencyRupee),
    ACTIVITY("activity", "Activity", Icons.Filled.History, Icons.Outlined.History),
    SETTINGS("settings", "Account", Icons.Filled.Settings, Icons.Outlined.Settings)
}

enum class SecondaryOverlayScreen {
    NONE,
    PERMISSIONS,
    ANALYTICS,
    DEMO_DIAGNOSTICS
}

@Composable
fun RidePilotAppShell(
    viewModel: RidePilotViewModel,
    googleUserEmail: String? = null,
    onSignOutGoogle: () -> Unit = {},
    forceSkipOnboarding: Boolean = false
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val settings by viewModel.settingsState.collectAsStateWithLifecycle()
    val monitoringState by viewModel.monitoringState.collectAsStateWithLifecycle()
    val elapsedSeconds by viewModel.monitoringElapsedSeconds.collectAsStateWithLifecycle()
    val listenerConnected by viewModel.listenerConnectedState.collectAsStateWithLifecycle()
    val lastServiceError by viewModel.lastServiceError.collectAsStateWithLifecycle()
    val permissionSnapshot by viewModel.permissionSnapshot.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetricsState.collectAsStateWithLifecycle()
    val allEvents by viewModel.allEventsState.collectAsStateWithLifecycle()
    val filteredEvents by viewModel.filteredEventsState.collectAsStateWithLifecycle()
    val activityFilter by viewModel.activityFilterState.collectAsStateWithLifecycle()
    val diagnostics by viewModel.diagnosticsState.collectAsStateWithLifecycle()
    val duplicatesBlocked by viewModel.duplicatesBlockedCount.collectAsStateWithLifecycle()
    val recentSessions by viewModel.recentSessionsState.collectAsStateWithLifecycle()
    val bannerMessage by viewModel.bannerMessage.collectAsStateWithLifecycle()
    val lastLatencyMs by viewModel.lastLatencyMs.collectAsStateWithLifecycle()
    val lastProcessedRecord by viewModel.lastProcessedRecord.collectAsStateWithLifecycle()

    var currentTab by rememberSaveable { mutableStateOf(PrimaryDestination.HOME) }
    var overlayScreen by rememberSaveable { mutableStateOf(SecondaryOverlayScreen.NONE) }
    var focusedFarePlatform by rememberSaveable { mutableStateOf<RidePlatform?>(null) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissions(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!settings.onboardingCompleted && !forceSkipOnboarding) {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            OnboardingScreen(
                onFinishOnboarding = { platforms, uMin, uMax, rMin, rMax, oMin, oMax ->
                    viewModel.completeOnboarding(
                        selectedPlatforms = platforms,
                        uberMin = uMin,
                        uberMax = uMax,
                        rapidoMin = rMin,
                        rapidoMax = rMax,
                        olaMin = oMin,
                        olaMax = oMax
                    )
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
        return
    }

    BackHandler(enabled = overlayScreen != SecondaryOverlayScreen.NONE || currentTab != PrimaryDestination.HOME) {
        if (overlayScreen != SecondaryOverlayScreen.NONE) {
            overlayScreen = SecondaryOverlayScreen.NONE
        } else {
            currentTab = PrimaryDestination.HOME
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                PrimaryDestination.entries.forEach { dest ->
                    val selected = overlayScreen == SecondaryOverlayScreen.NONE && currentTab == dest
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            overlayScreen = SecondaryOverlayScreen.NONE
                            currentTab = dest
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                contentDescription = dest.label
                            )
                        },
                        label = {
                            Text(
                                text = dest.label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.testTag("bottom_nav_${dest.route}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FeedbackBanner(
                message = bannerMessage,
                onDismiss = { viewModel.dismissBanner() }
            )

            when (overlayScreen) {
                SecondaryOverlayScreen.PERMISSIONS -> {
                    PermissionsScreen(
                        permissionSnapshot = permissionSnapshot,
                        listenerConnected = listenerConnected,
                        onRefreshPermissions = { viewModel.refreshPermissions(context) },
                        onBack = { overlayScreen = SecondaryOverlayScreen.NONE }
                    )
                }

                SecondaryOverlayScreen.ANALYTICS -> {
                    AnalyticsScreen(
                        metrics = metrics,
                        events = allEvents,
                        onBack = { overlayScreen = SecondaryOverlayScreen.NONE },
                        onOpenDemoLab = { overlayScreen = SecondaryOverlayScreen.DEMO_DIAGNOSTICS }
                    )
                }

                SecondaryOverlayScreen.DEMO_DIAGNOSTICS -> {
                    DemoDiagnosticsScreen(
                        diagnostics = diagnostics,
                        duplicatesBlocked = duplicatesBlocked,
                        recentSessions = recentSessions,
                        onRunScenario = { platform, scenario ->
                            viewModel.runDemoScenario(context, platform, scenario)
                        },
                        onClearDemoEvents = { viewModel.clearDemoEventsOnly() },
                        onBack = { overlayScreen = SecondaryOverlayScreen.NONE }
                    )
                }

                SecondaryOverlayScreen.NONE -> {
                    when (currentTab) {
                        PrimaryDestination.HOME -> {
                            DashboardScreen(
                                settings = settings,
                                googleUserEmail = googleUserEmail,
                                monitoringState = monitoringState,
                                elapsedSeconds = elapsedSeconds,
                                listenerConnected = listenerConnected,
                                lastServiceError = lastServiceError,
                                permissionSnapshot = permissionSnapshot,
                                metrics = metrics,
                                recentEvents = allEvents,
                                lastLatencyMs = lastLatencyMs,
                                lastProcessedRecord = lastProcessedRecord,
                                onStartMonitoring = { viewModel.startMonitoringService(context) },
                                onPauseMonitoring = { viewModel.pauseMonitoringService(context) },
                                onResumeMonitoring = { viewModel.resumeMonitoringService(context) },
                                onStopMonitoring = { viewModel.stopMonitoringService(context) },
                                onConfirmRide = { viewModel.confirmRideEvent(it) },
                                onRejectRide = { viewModel.rejectRideEvent(it) },
                                onSimulateInstantAutoAccept = { platform ->
                                    viewModel.simulateInstantAutoAcceptInRange(platform)
                                },
                                onConfigurePlatformFares = { platform ->
                                    focusedFarePlatform = platform
                                    currentTab = PrimaryDestination.FARES
                                },
                                onQuickDemoScenario = { platform, scenario ->
                                    viewModel.runDemoScenario(context, platform, scenario)
                                },
                                onOpenPermissions = { overlayScreen = SecondaryOverlayScreen.PERMISSIONS },
                                onOpenAnalytics = { overlayScreen = SecondaryOverlayScreen.ANALYTICS },
                                onOpenDemoLab = { overlayScreen = SecondaryOverlayScreen.DEMO_DIAGNOSTICS },
                                onOpenActivity = { currentTab = PrimaryDestination.ACTIVITY },
                                onOpenSettings = { currentTab = PrimaryDestination.SETTINGS }
                            )
                        }

                        PrimaryDestination.PLATFORMS -> {
                            PlatformsScreen(
                                settings = settings,
                                permissionSnapshot = permissionSnapshot,
                                listenerConnected = listenerConnected,
                                onTogglePlatform = { p, enabled ->
                                    viewModel.togglePlatformEnabled(p, enabled)
                                },
                                onToggleAuthorizedSim = { p, enabled ->
                                    viewModel.toggleAuthorizedIntegrationSimulation(p, enabled)
                                },
                                onConfigurePlatformFares = { platform ->
                                    focusedFarePlatform = platform
                                    currentTab = PrimaryDestination.FARES
                                },
                                onOpenPermissions = { overlayScreen = SecondaryOverlayScreen.PERMISSIONS }
                            )
                        }

                        PrimaryDestination.FARES -> {
                            FareSettingsScreen(
                                settings = settings,
                                initialSelectedPlatform = focusedFarePlatform,
                                onSavePlatformConfig = { cfg, cb ->
                                    viewModel.savePlatformFareConfig(cfg, cb)
                                },
                                onResetPlatformDefaults = { platform ->
                                    viewModel.resetPlatformFareDefaults(platform)
                                },
                                onSaveAdditionalFilters = { filters ->
                                    viewModel.saveAdditionalFilters(filters)
                                }
                            )
                        }

                        PrimaryDestination.ACTIVITY -> {
                            ActivityHistoryScreen(
                                events = filteredEvents,
                                filterState = activityFilter,
                                onUpdateFilter = { viewModel.updateActivityFilter(it) },
                                onConfirmRide = { viewModel.confirmRideEvent(it) },
                                onRejectRide = { viewModel.rejectRideEvent(it) },
                                onDeleteEvent = { viewModel.deleteRideEvent(it) },
                                onClearDemoEvents = { viewModel.clearDemoEventsOnly() },
                                onClearAllHistory = { viewModel.clearAllHistory() },
                                onOpenDemoLab = { overlayScreen = SecondaryOverlayScreen.DEMO_DIAGNOSTICS }
                            )
                        }

                        PrimaryDestination.SETTINGS -> {
                            SettingsScreen(
                                settings = settings,
                                googleUserEmail = googleUserEmail,
                                monitoringState = monitoringState,
                                permissionSnapshot = permissionSnapshot,
                                onSaveProfile = { viewModel.saveDriverProfile(it) },
                                onSaveThemeMode = { viewModel.saveThemeMode(it) },
                                onSaveNotifications = { viewModel.saveNotificationPreferences(it) },
                                onClearHistory = { viewModel.clearAllHistory() },
                                onSignOutGoogle = onSignOutGoogle,
                                onOpenPermissions = { overlayScreen = SecondaryOverlayScreen.PERMISSIONS },
                                onOpenDemoLab = { overlayScreen = SecondaryOverlayScreen.DEMO_DIAGNOSTICS }
                            )
                        }
                    }
                }
            }
        }
    }
}
