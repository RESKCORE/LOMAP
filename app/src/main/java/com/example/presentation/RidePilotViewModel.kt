package com.example.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.datastore.PersistedAppSettings
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.RidePilotRepository
import com.example.di.AppContainer
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DriverProfileSettings
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.MonitoringSessionRecord
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import com.example.monitoring.processor.DemoScenarioSimulator
import com.example.monitoring.processor.DemoScenarioType
import com.example.monitoring.processor.ProcessingDiagnosticEntry
import com.example.monitoring.processor.ProcessingOutcome
import com.example.monitoring.processor.RideEventProcessor
import com.example.monitoring.service.MonitoringStateRegistry
import com.example.monitoring.service.RideMonitoringService
import com.example.utils.PermissionHelper
import com.example.utils.PermissionStatusSnapshot
import java.util.Locale
import java.util.UUID
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ActivityHistoryTab(val label: String) {
    ALL("All"),
    ELIGIBLE("Eligible"),
    FILTERED("Filtered"),
    CONFIRMED("Confirmed"),
    FAILED("Failed")
}

enum class DateRangeFilter(val label: String) {
    ALL_TIME("All Time"),
    TODAY("Today"),
    LAST_24H("Last 24h"),
    DEMO_ONLY("Demo Only"),
    LIVE_ONLY("Live Only")
}

data class ActivityFilterState(
    val selectedTab: ActivityHistoryTab = ActivityHistoryTab.ALL,
    val searchQuery: String = "",
    val selectedPlatform: RidePlatform? = null,
    val dateRangeFilter: DateRangeFilter = DateRangeFilter.ALL_TIME,
    val sortNewestFirst: Boolean = true
)

data class DashboardMetrics(
    val totalDetected: Int = 0,
    val eligibleCount: Int = 0,
    val filteredCount: Int = 0,
    val driverConfirmedCount: Int = 0,
    val failedOrUnknownCount: Int = 0,
    val todayDetected: Int = 0,
    val todayEligible: Int = 0,
    val averageEligibleFareInr: Double? = null,
    val acceptanceSuccessRatePercent: Double? = null,
    val platformCounts: Map<RidePlatform, Int> = emptyMap()
)

class RidePilotViewModel(
    private val repository: RidePilotRepository,
    private val processor: RideEventProcessor,
    private val cloudSyncRepository: CloudSyncRepository? = null
) : ViewModel() {

    private val demoSimulator = DemoScenarioSimulator(repository, processor)

    val settingsState: StateFlow<PersistedAppSettings> = repository.settingsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PersistedAppSettings()
        )

    val allEventsState: StateFlow<List<RideEventRecord>> = repository.allRideEventsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val recentSessionsState: StateFlow<List<MonitoringSessionRecord>> = repository.recentSessionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val monitoringState: StateFlow<MonitoringServiceState> = MonitoringStateRegistry.serviceState
    val monitoringElapsedSeconds: StateFlow<Long> = MonitoringStateRegistry.elapsedSeconds
    val listenerConnectedState: StateFlow<Boolean> = MonitoringStateRegistry.listenerConnected
    val lastServiceError: StateFlow<String?> = MonitoringStateRegistry.lastErrorDetail

    val diagnosticsState: StateFlow<List<ProcessingDiagnosticEntry>> = processor.diagnosticsFlow
    val duplicatesBlockedCount: StateFlow<Int> = processor.duplicatesBlockedCount
    val lastLatencyMs: StateFlow<Double> = processor.lastLatencyMs
    val lastProcessedRecord: StateFlow<RideEventRecord?> = processor.lastProcessedRecord

    private val _activityFilterState = MutableStateFlow(ActivityFilterState())
    val activityFilterState: StateFlow<ActivityFilterState> = _activityFilterState.asStateFlow()

    private val _permissionSnapshot = MutableStateFlow(
        PermissionStatusSnapshot(
            notificationListenerGranted = false,
            postNotificationsGranted = false,
            foregroundServicePermitted = true,
            uberInstalled = false,
            rapidoInstalled = false,
            olaInstalled = false
        )
    )
    val permissionSnapshot: StateFlow<PermissionStatusSnapshot> = _permissionSnapshot.asStateFlow()

    private val _bannerMessage = MutableStateFlow<String?>(null)
    val bannerMessage: StateFlow<String?> = _bannerMessage.asStateFlow()

    val filteredEventsState: StateFlow<List<RideEventRecord>> = combine(
        allEventsState,
        _activityFilterState
    ) { events, filter ->
        val now = System.currentTimeMillis()
        val startOfDayMillis = now - (now % 86_400_000L)

        events.filter { event ->
            val matchesTab = when (filter.selectedTab) {
                ActivityHistoryTab.ALL -> true
                ActivityHistoryTab.ELIGIBLE -> event.isOverallEligible
                ActivityHistoryTab.FILTERED ->
                    event.acceptanceOutcome == AcceptanceOutcome.FILTERED_OUT
                ActivityHistoryTab.CONFIRMED ->
                    event.acceptanceOutcome == AcceptanceOutcome.DRIVER_CONFIRMED ||
                        event.acceptanceOutcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION
                ActivityHistoryTab.FAILED ->
                    event.acceptanceOutcome == AcceptanceOutcome.FAILED ||
                        event.acceptanceOutcome == AcceptanceOutcome.UNKNOWN_FARE ||
                        event.acceptanceOutcome == AcceptanceOutcome.REJECTED_BY_DRIVER
            }

            val matchesPlatform = filter.selectedPlatform == null || event.platform == filter.selectedPlatform

            val matchesDate = when (filter.dateRangeFilter) {
                DateRangeFilter.ALL_TIME -> true
                DateRangeFilter.TODAY -> event.timestampMillis >= startOfDayMillis
                DateRangeFilter.LAST_24H -> (now - event.timestampMillis) <= 86_400_000L
                DateRangeFilter.DEMO_ONLY -> event.isDemo
                DateRangeFilter.LIVE_ONLY -> !event.isDemo
            }

            val q = filter.searchQuery.trim()
            val matchesSearch = q.isEmpty() ||
                event.platform.displayName.contains(q, ignoreCase = true) ||
                (event.pickupLocation?.contains(q, ignoreCase = true) == true) ||
                (event.destinationLocation?.contains(q, ignoreCase = true) == true) ||
                event.statusDetail.contains(q, ignoreCase = true) ||
                event.fareEvaluationResult.label.contains(q, ignoreCase = true) ||
                (event.fareInr?.toInt()?.toString()?.contains(q) == true)

            matchesTab && matchesPlatform && matchesDate && matchesSearch
        }.let { matched ->
            if (filter.sortNewestFirst) {
                matched.sortedByDescending { it.timestampMillis }
            } else {
                matched.sortedBy { it.timestampMillis }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val dashboardMetricsState: StateFlow<DashboardMetrics> = allEventsState
        .combine(_activityFilterState) { events, _ ->
            val now = System.currentTimeMillis()
            val startOfToday = now - (now % 86_400_000L)

            val total = events.size
            val eligibleEvents = events.filter { it.isOverallEligible }
            val filteredCount = events.count { it.acceptanceOutcome == AcceptanceOutcome.FILTERED_OUT }
            val confirmedCount = events.count {
                it.acceptanceOutcome == AcceptanceOutcome.DRIVER_CONFIRMED ||
                    it.acceptanceOutcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION
            }
            val failedOrUnknown = events.count {
                it.acceptanceOutcome == AcceptanceOutcome.FAILED ||
                    it.acceptanceOutcome == AcceptanceOutcome.UNKNOWN_FARE
            }

            val todayEvents = events.filter { it.timestampMillis >= startOfToday }
            val todayEligible = todayEvents.count { it.isOverallEligible }

            val validEligibleFares = eligibleEvents.mapNotNull { it.fareInr }
            val avgEligibleFare = if (validEligibleFares.isNotEmpty()) {
                validEligibleFares.average()
            } else {
                null
            }

            val actionableDecisionEvents = events.filter {
                it.acceptanceOutcome == AcceptanceOutcome.DRIVER_CONFIRMED ||
                    it.acceptanceOutcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION ||
                    it.acceptanceOutcome == AcceptanceOutcome.REJECTED_BY_DRIVER ||
                    it.acceptanceOutcome == AcceptanceOutcome.FAILED
            }
            val successRate = if (actionableDecisionEvents.isNotEmpty()) {
                (confirmedCount.toDouble() / actionableDecisionEvents.size.toDouble()) * 100.0
            } else {
                null
            }

            val platformCounts = RidePlatform.entries.associateWith { platform ->
                events.count { it.platform == platform }
            }

            DashboardMetrics(
                totalDetected = total,
                eligibleCount = eligibleEvents.size,
                filteredCount = filteredCount,
                driverConfirmedCount = confirmedCount,
                failedOrUnknownCount = failedOrUnknown,
                todayDetected = todayEvents.size,
                todayEligible = todayEligible,
                averageEligibleFareInr = avgEligibleFare,
                acceptanceSuccessRatePercent = successRate,
                platformCounts = platformCounts
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DashboardMetrics()
        )

    fun refreshPermissions(context: Context) {
        _permissionSnapshot.value = PermissionHelper.getPermissionSnapshot(context)
    }

    fun dismissBanner() {
        _bannerMessage.value = null
    }

    fun showBanner(message: String) {
        _bannerMessage.value = message
    }

    fun syncAuthenticatedGoogleUser(displayName: String?, email: String?) {
        viewModelScope.launch {
            val currentSettings = repository.settingsFlow.first()
            val cleanName = displayName?.trim()?.takeIf { it.isNotEmpty() }
            if (cleanName != null &&
                (currentSettings.driverProfile.driverName == "Rajesh Kumar" ||
                    currentSettings.driverProfile.driverName.isBlank())
            ) {
                repository.saveDriverProfile(
                    currentSettings.driverProfile.copy(driverName = cleanName)
                )
            }
            val profileToSync = repository.settingsFlow.first().driverProfile
            cloudSyncRepository?.upsertDriverProfile(
                displayName = cleanName ?: profileToSync.driverName,
                email = email,
                city = profileToSync.city,
                autoAcceptMillisTarget = 3
            )
        }
    }

    fun completeOnboarding(
        selectedPlatforms: Set<RidePlatform>,
        uberMin: Double,
        uberMax: Double,
        rapidoMin: Double,
        rapidoMax: Double,
        olaMin: Double,
        olaMax: Double
    ) {
        viewModelScope.launch {
            val uberCfg = PlatformFareConfig(
                platform = RidePlatform.UBER,
                platformEnabled = RidePlatform.UBER in selectedPlatforms,
                fareFilterEnabled = true,
                minimumFareInr = uberMin,
                maximumFareInr = uberMax,
                authorizedIntegrationSimulated = true
            )
            val rapidoCfg = PlatformFareConfig(
                platform = RidePlatform.RAPIDO,
                platformEnabled = RidePlatform.RAPIDO in selectedPlatforms,
                fareFilterEnabled = true,
                minimumFareInr = rapidoMin,
                maximumFareInr = rapidoMax,
                authorizedIntegrationSimulated = true
            )
            val olaCfg = PlatformFareConfig(
                platform = RidePlatform.OLA,
                platformEnabled = RidePlatform.OLA in selectedPlatforms,
                fareFilterEnabled = true,
                minimumFareInr = olaMin,
                maximumFareInr = olaMax,
                authorizedIntegrationSimulated = true
            )

            if (uberCfg.validate().isValid) repository.savePlatformFareConfig(uberCfg)
            if (rapidoCfg.validate().isValid) repository.savePlatformFareConfig(rapidoCfg)
            if (olaCfg.validate().isValid) repository.savePlatformFareConfig(olaCfg)
            repository.setOnboardingCompleted(true)
            _bannerMessage.value = "LOMAP ready. Rides inside your INR range auto-accept in milliseconds."
        }
    }

    fun startMonitoringService(context: Context) {
        refreshPermissions(context)
        val perms = _permissionSnapshot.value
        if (!perms.postNotificationsGranted) {
            MonitoringStateRegistry.updateServiceState(
                MonitoringServiceState.PERMISSION_REQUIRED,
                "Notification permission is required so LOMAP can display its persistent foreground service status."
            )
            _bannerMessage.value = "Grant Notification permission first to run foreground monitoring."
            return
        }
        RideMonitoringService.startMonitoring(context, allowDemoWithoutListener = true)
    }

    fun pauseMonitoringService(context: Context) {
        RideMonitoringService.pauseMonitoring(context)
    }

    fun resumeMonitoringService(context: Context) {
        RideMonitoringService.resumeMonitoring(context)
    }

    fun stopMonitoringService(context: Context) {
        RideMonitoringService.stopMonitoring(context)
    }

    fun togglePlatformEnabled(platform: RidePlatform, enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsState.value.configFor(platform)
            repository.savePlatformFareConfig(current.copy(platformEnabled = enabled))
            _bannerMessage.value = "${platform.appTitle} monitoring ${if (enabled) "enabled" else "disabled"}."
        }
    }

    fun toggleAuthorizedIntegrationSimulation(platform: RidePlatform, enabled: Boolean) {
        viewModelScope.launch {
            val current = settingsState.value.configFor(platform)
            repository.savePlatformFareConfig(current.copy(authorizedIntegrationSimulated = enabled))
            _bannerMessage.value = if (enabled) {
                "${platform.displayName} Millisecond Auto-Accept enabled for in-range fares."
            } else {
                "${platform.displayName} set to manual driver confirmation."
            }
        }
    }

    fun savePlatformFareConfig(config: PlatformFareConfig, onResult: (Boolean, String) -> Unit) {
        val validation = config.validate()
        if (!validation.isValid) {
            onResult(false, validation.errorMessage ?: "Invalid fare configuration.")
            return
        }
        viewModelScope.launch {
            try {
                repository.savePlatformFareConfig(config)
                val msg = "Saved ${config.platform.displayName} Auto-Accept range: ₹${config.minimumFareInr.toInt()} – ₹${config.maximumFareInr.toInt()}"
                _bannerMessage.value = msg
                onResult(true, msg)
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to save configuration.")
            }
        }
    }

    fun resetPlatformFareDefaults(platform: RidePlatform) {
        viewModelScope.launch {
            repository.resetPlatformFareToDefaults(platform)
            _bannerMessage.value = "Reset ${platform.displayName} fare settings to ₹${platform.defaultMinFareInr.toInt()} – ₹${platform.defaultMaxFareInr.toInt()}."
        }
    }

    fun saveAdditionalFilters(filters: AdditionalRideFilters) {
        viewModelScope.launch {
            repository.saveAdditionalFilters(filters)
            _bannerMessage.value = "Updated secondary ride filters."
        }
    }

    fun saveNotificationPreferences(preferences: NotificationPreferences) {
        viewModelScope.launch {
            repository.saveNotificationPreferences(preferences)
        }
    }

    fun saveDriverProfile(profile: DriverProfileSettings) {
        viewModelScope.launch {
            repository.saveDriverProfile(profile)
            cloudSyncRepository?.upsertDriverProfile(
                displayName = profile.driverName,
                email = null,
                city = profile.city,
                autoAcceptMillisTarget = 3
            )
            _bannerMessage.value = "Driver profile saved & synced to cloud."
        }
    }

    fun saveThemeMode(mode: AppThemeMode) {
        viewModelScope.launch {
            repository.saveThemeMode(mode)
        }
    }

    fun confirmRideEvent(eventId: Long) {
        viewModelScope.launch {
            processor.confirmRideByDriver(eventId)
            _bannerMessage.value = "Ride #$eventId marked as Driver Confirmed."
        }
    }

    fun rejectRideEvent(eventId: Long) {
        viewModelScope.launch {
            processor.rejectRideByDriver(eventId)
            _bannerMessage.value = "Ride #$eventId marked as Rejected."
        }
    }

    fun deleteRideEvent(eventId: Long) {
        viewModelScope.launch {
            repository.deleteRideEvent(eventId)
            _bannerMessage.value = "Deleted ride record #$eventId."
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAllRideHistory()
            _bannerMessage.value = "Cleared all local ride event history."
        }
    }

    fun clearDemoEventsOnly() {
        viewModelScope.launch {
            repository.clearDemoEventsOnly()
            _bannerMessage.value = "Cleared all [DEMO] simulation events."
        }
    }

    fun updateActivityFilter(transform: (ActivityFilterState) -> ActivityFilterState) {
        _activityFilterState.value = transform(_activityFilterState.value)
    }

    fun runDemoScenario(
        context: Context,
        platform: RidePlatform,
        scenario: DemoScenarioType
    ) {
        viewModelScope.launch {
            val summary = demoSimulator.executeScenario(context, platform, scenario)
            _bannerMessage.value = summary
        }
    }

    /**
     * Dispatches an incoming ride request whose INR fare is guaranteed to be inside the user's
     * configured [minimumFareInr..maximumFareInr] range for [platform], demonstrating millisecond
     * automatic ride acceptance.
     */
    fun simulateInstantAutoAcceptInRange(platform: RidePlatform) {
        viewModelScope.launch {
            val settings = repository.settingsFlow.first()
            val config = settings.configFor(platform)
            if (!config.platformEnabled || !config.authorizedIntegrationSimulated) {
                repository.savePlatformFareConfig(
                    config.copy(
                        platformEnabled = true,
                        authorizedIntegrationSimulated = true
                    )
                )
            }
            val targetFare = ((config.minimumFareInr + config.maximumFareInr) / 2.0)
                .coerceIn(config.minimumFareInr, config.maximumFareInr)
                .toInt()

            val event = NormalizedNotificationEvent(
                notificationKey = "instant_${platform.name}_${UUID.randomUUID().toString().take(8)}",
                packageName = platform.packageName,
                title = "${platform.appTitle} • Priority Dispatch",
                text = "Fare: ₹$targetFare | Pickup: 1.4 km (Indiranagar 100ft Rd) | Trip: 6.2 km | Drop: MG Road Metro | 16 min | Rating: 4.9★",
                bigText = null,
                subText = "Instant Auto-Accept Range ₹${config.minimumFareInr.toInt()}–₹${config.maximumFareInr.toInt()}",
                postedAtMillis = System.currentTimeMillis(),
                isDemo = true,
                demoScenarioName = "Instant Millisecond Auto-Accept"
            )

            val outcome = processor.processIncomingNotification(event)
            if (outcome is ProcessingOutcome.Processed) {
                val msStr = String.format(Locale.US, "%.2f", processor.lastLatencyMs.value)
                _bannerMessage.value =
                    "⚡ Auto-Accepted ${platform.displayName} ₹$targetFare in $msStr ms (Range ₹${config.minimumFareInr.toInt()}–₹${config.maximumFareInr.toInt()})"
            }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val container = AppContainer.getInstance(context.applicationContext)
            return RidePilotViewModel(
                repository = container.repository,
                processor = container.rideEventProcessor,
                cloudSyncRepository = container.cloudSyncRepository
            ) as T
        }
    }
}
