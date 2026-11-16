package com.example.monitoring.processor

import android.content.Context
import com.example.data.datastore.PersistedAppSettings
import com.example.data.repository.RidePilotRepository
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.MonitoringServiceState
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RideInfoAvailability
import com.example.domain.model.RidePlatform
import com.example.monitoring.service.MonitoringStateRegistry
import com.example.monitoring.service.RideMonitoringService

enum class DemoScenarioType(
    val id: Int,
    val title: String,
    val subtitle: String
) {
    ELIGIBLE_FARE(1, "1. Eligible Fare", "Generates a request comfortably inside the platform's min–max INR range."),
    BELOW_MINIMUM(2, "2. Fare Below Minimum", "Generates a short trip fare below the configured minimum threshold."),
    ABOVE_MAXIMUM(3, "3. Fare Above Maximum", "Generates a long outstation/airport trip exceeding maximum threshold."),
    UNKNOWN_FARE(4, "4. Unknown Fare", "Simulates a dispatch alert where the partner app omits the INR fare."),
    PLATFORM_DISABLED(5, "5. Platform Disabled", "Simulates an incoming notification for a disabled platform."),
    INVALID_CONFIGURATION(6, "6. Invalid Configuration", "Evaluates against an inverted min > max configuration state."),
    MISSING_NOTIFICATION_DETAILS(7, "7. Missing Notification Details", "Simulates a sparse notification missing distance and drop location."),
    DUPLICATE_NOTIFICATION(8, "8. Duplicate Notification", "Fires two identical notifications back-to-back to verify deduplication."),
    MONITORING_SERVICE_STOPPED(9, "9. Monitoring Service Stopped", "Stops RideMonitoringService and logs service stop diagnostic."),
    PERMISSION_UNAVAILABLE(10, "10. Permission Unavailable", "Simulates missing NotificationListener permission state."),
    ACCEPTANCE_INTEGRATION_UNAVAILABLE(11, "11. Acceptance Integration Unavailable", "Verifies eligible ride stays manual when official API is unavailable."),
    AUTHORIZED_ACCEPTANCE_SUCCESS(12, "12. Authorized Acceptance Success", "Simulates an eligible ride with authorized partner dispatch webhook.")
}

class DemoScenarioSimulator(
    private val repository: RidePilotRepository,
    private val processor: RideEventProcessor
) {
    suspend fun executeScenario(
        context: Context,
        platform: RidePlatform,
        scenario: DemoScenarioType
    ): String {
        val settings: PersistedAppSettings = repository.getCurrentSettings()
        val config = settings.configFor(platform)
        val now = System.currentTimeMillis()

        return when (scenario) {
            DemoScenarioType.ELIGIBLE_FARE -> {
                val targetFare = ((config.minimumFareInr + config.maximumFareInr) / 2.0).coerceAtLeast(60.0)
                val event = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • New Trip Request",
                    body = "Fare: ₹${targetFare.toInt()} | Trip: 6.4 km | Pickup: Indiranagar 100ft Rd | Drop: Koramangala Sony World",
                    timestamp = now,
                    scenario = scenario.title
                )
                processor.processIncomingNotification(event)
                "Dispatched [DEMO] eligible ${platform.displayName} request at ₹${targetFare.toInt()}."
            }

            DemoScenarioType.BELOW_MINIMUM -> {
                val lowFare = (config.minimumFareInr - 35.0).coerceAtLeast(25.0)
                val event = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • Short Hop Request",
                    body = "Fare: ₹${lowFare.toInt()} | Trip: 1.8 km | Pickup: MG Road Metro | Drop: Brigade Road",
                    timestamp = now,
                    scenario = scenario.title
                )
                processor.processIncomingNotification(event)
                "Dispatched [DEMO] below-minimum ${platform.displayName} request at ₹${lowFare.toInt()} (Min ₹${config.minimumFareInr.toInt()})."
            }

            DemoScenarioType.ABOVE_MAXIMUM -> {
                val highFare = config.maximumFareInr + 180.0
                val event = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • Long Trip Dispatch",
                    body = "Fare: ₹${highFare.toInt()} | Trip: 34.5 km | Pickup: HSR Layout Sector 2 | Drop: Kempegowda Airport T2",
                    timestamp = now,
                    scenario = scenario.title
                )
                processor.processIncomingNotification(event)
                "Dispatched [DEMO] above-maximum ${platform.displayName} request at ₹${highFare.toInt()} (Max ₹${config.maximumFareInr.toInt()})."
            }

            DemoScenarioType.UNKNOWN_FARE -> {
                val event = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • Priority Ping Nearby",
                    body = "Trip: 5.2 km | Pickup: Jayanagar 4th Block | Drop: JP Nagar Central (Fare shown on acceptance)",
                    timestamp = now,
                    scenario = scenario.title
                )
                processor.processIncomingNotification(event)
                "Dispatched [DEMO] unknown-fare ${platform.displayName} notification. Verified never auto-accepted."
            }

            DemoScenarioType.PLATFORM_DISABLED -> {
                val record = RideEventRecord(
                    eventHash = "demo_disabled_${now.toString(16)}",
                    platform = platform,
                    fareInr = 210.0,
                    fareAvailability = RideInfoAvailability.AVAILABLE,
                    tripDistanceKm = 7.0,
                    distanceAvailability = RideInfoAvailability.AVAILABLE,
                    estimatedFarePerKmInr = 30.0,
                    pickupLocation = "Malleshwaram Circle",
                    pickupAvailability = RideInfoAvailability.AVAILABLE,
                    destinationLocation = "Rajajinagar Metro",
                    destinationAvailability = RideInfoAvailability.AVAILABLE,
                    timestampMillis = now,
                    fareEvaluationResult = FareEvaluationResult.PLATFORM_DISABLED,
                    additionalFilterResult = AdditionalFilterResult.PASSED,
                    isOverallEligible = false,
                    driverAction = "Ignored (Platform Disabled)",
                    acceptanceOutcome = AcceptanceOutcome.FILTERED_OUT,
                    statusDetail = "[DEMO] Simulated event while ${platform.appTitle} is disabled.",
                    rawNotificationSummary = "${platform.appTitle} • ₹210 | Pickup: Malleshwaram Circle",
                    isDemo = true,
                    demoScenarioLabel = scenario.title
                )
                repository.recordRideEvent(record)
                processor.appendDiagnostic(
                    title = "[DEMO] ${platform.displayName} → Platform Disabled",
                    detail = "Event recorded as PLATFORM_DISABLED and filtered out.",
                    isWarning = true
                )
                "Recorded [DEMO] ${platform.displayName} request evaluated under PLATFORM_DISABLED."
            }

            DemoScenarioType.INVALID_CONFIGURATION -> {
                val record = RideEventRecord(
                    eventHash = "demo_invalid_cfg_${now.toString(16)}",
                    platform = platform,
                    fareInr = 180.0,
                    fareAvailability = RideInfoAvailability.AVAILABLE,
                    tripDistanceKm = 5.5,
                    distanceAvailability = RideInfoAvailability.AVAILABLE,
                    estimatedFarePerKmInr = 32.7,
                    pickupLocation = "BTM Layout 2nd Stage",
                    pickupAvailability = RideInfoAvailability.AVAILABLE,
                    destinationLocation = "Bellandur Gate",
                    destinationAvailability = RideInfoAvailability.AVAILABLE,
                    timestampMillis = now,
                    fareEvaluationResult = FareEvaluationResult.INVALID_CONFIGURATION,
                    additionalFilterResult = AdditionalFilterResult.PASSED,
                    isOverallEligible = false,
                    driverAction = "Blocked (Invalid Config)",
                    acceptanceOutcome = AcceptanceOutcome.FAILED,
                    statusDetail = "[DEMO] Min fare (₹500) > Max fare (₹200) rejected by FareEvaluator.",
                    rawNotificationSummary = "${platform.appTitle} • Invalid Config Guard Test",
                    isDemo = true,
                    demoScenarioLabel = scenario.title
                )
                repository.recordRideEvent(record)
                processor.appendDiagnostic(
                    title = "[DEMO] Invalid Configuration Guard Triggered",
                    detail = "FareEvaluator returned INVALID_CONFIGURATION; ride blocked safely.",
                    isWarning = true
                )
                "Simulated [DEMO] INVALID_CONFIGURATION evaluation guard on ${platform.displayName}."
            }

            DemoScenarioType.MISSING_NOTIFICATION_DETAILS -> {
                val targetFare = ((config.minimumFareInr + config.maximumFareInr) / 2.0).coerceAtLeast(80.0)
                val event = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • Express Ping",
                    body = "Estimated payout ₹${targetFare.toInt()} • Tap to open partner app for route details",
                    timestamp = now,
                    scenario = scenario.title
                )
                processor.processIncomingNotification(event)
                "Dispatched [DEMO] sparse notification (Fare ₹${targetFare.toInt()} available; distance & locations marked UNAVAILABLE)."
            }

            DemoScenarioType.DUPLICATE_NOTIFICATION -> {
                val duplicateBody = "Fare: ₹240 | Trip: 7.5 km | Pickup: Domlur Flyover | Drop: Whitefield ITPL"
                val firstEvent = buildEvent(
                    platform = platform,
                    title = "${platform.appTitle} • Ride Ping",
                    body = duplicateBody,
                    timestamp = now,
                    scenario = scenario.title
                )
                val secondEvent = firstEvent.copy(postedAtMillis = now + 400L)
                processor.processIncomingNotification(firstEvent)
                val secondOutcome = processor.processIncomingNotification(secondEvent)
                if (secondOutcome is ProcessingOutcome.IgnoredDuplicate) {
                    "Dispatched 2 identical ${platform.displayName} notifications: 1st processed, 2nd suppressed by deduplication cache."
                } else {
                    "Dispatched duplicate test pair for ${platform.displayName}."
                }
            }

            DemoScenarioType.MONITORING_SERVICE_STOPPED -> {
                RideMonitoringService.stopMonitoring(context)
                "Stopped RideMonitoringService and transitioned state to INACTIVE."
            }

            DemoScenarioType.PERMISSION_UNAVAILABLE -> {
                MonitoringStateRegistry.updateServiceState(
                    MonitoringServiceState.PERMISSION_REQUIRED,
                    "[DEMO] Simulated missing Notification Listener & POST_NOTIFICATIONS access."
                )
                processor.appendDiagnostic(
                    title = "[DEMO] Permission Required State",
                    detail = "Monitoring blocked until Android confirms Notification Access.",
                    isWarning = true
                )
                "Transitioned monitoring card to PERMISSION_REQUIRED state."
            }

            DemoScenarioType.ACCEPTANCE_INTEGRATION_UNAVAILABLE -> {
                val targetFare = ((config.minimumFareInr + config.maximumFareInr) / 2.0).coerceAtLeast(110.0)
                val record = RideEventRecord(
                    eventHash = "demo_unavail_${now.toString(16)}",
                    platform = platform,
                    fareInr = targetFare,
                    fareAvailability = RideInfoAvailability.AVAILABLE,
                    tripDistanceKm = 6.2,
                    distanceAvailability = RideInfoAvailability.AVAILABLE,
                    estimatedFarePerKmInr = kotlin.math.round((targetFare / 6.2) * 10.0) / 10.0,
                    pickupLocation = "Ulsoor Lake",
                    pickupAvailability = RideInfoAvailability.AVAILABLE,
                    destinationLocation = "Manyata Tech Park",
                    destinationAvailability = RideInfoAvailability.AVAILABLE,
                    timestampMillis = now,
                    fareEvaluationResult = FareEvaluationResult.ELIGIBLE,
                    additionalFilterResult = AdditionalFilterResult.PASSED,
                    isOverallEligible = true,
                    driverAction = "Awaiting Driver Confirmation",
                    acceptanceOutcome = AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE,
                    statusDetail = "[DEMO] Official ${platform.displayName} auto-accept API unavailable; presented manual confirmation.",
                    rawNotificationSummary = "${platform.appTitle} • ₹${targetFare.toInt()} | Pickup: Ulsoor Lake → Manyata Tech Park",
                    isDemo = true,
                    demoScenarioLabel = scenario.title
                )
                val id = repository.recordRideEvent(record)
                processor.appendDiagnostic(
                    title = "[DEMO] Acceptance Integration Unavailable (#$id)",
                    detail = "Eligible ride requires manual driver confirmation."
                )
                "Recorded [DEMO] eligible ride (#$id) with ACCEPTANCE_UNAVAILABLE (manual driver confirmation)."
            }

            DemoScenarioType.AUTHORIZED_ACCEPTANCE_SUCCESS -> {
                val targetFare = ((config.minimumFareInr + config.maximumFareInr) / 2.0).coerceAtLeast(130.0)
                val record = RideEventRecord(
                    eventHash = "demo_auth_ok_${now.toString(16)}",
                    platform = platform,
                    fareInr = targetFare,
                    fareAvailability = RideInfoAvailability.AVAILABLE,
                    tripDistanceKm = 8.0,
                    distanceAvailability = RideInfoAvailability.AVAILABLE,
                    estimatedFarePerKmInr = kotlin.math.round((targetFare / 8.0) * 10.0) / 10.0,
                    pickupLocation = "Indiranagar Metro",
                    pickupAvailability = RideInfoAvailability.AVAILABLE,
                    destinationLocation = "Electronic City Phase 1",
                    destinationAvailability = RideInfoAvailability.AVAILABLE,
                    timestampMillis = now,
                    fareEvaluationResult = FareEvaluationResult.ELIGIBLE,
                    additionalFilterResult = AdditionalFilterResult.PASSED,
                    isOverallEligible = true,
                    driverAction = "Auto-Accepted (Authorized Sandbox)",
                    acceptanceOutcome = AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION,
                    statusDetail = "[DEMO] Accepted via simulated ${platform.displayName} Authorized Partner Dispatch API.",
                    rawNotificationSummary = "${platform.appTitle} • ₹${targetFare.toInt()} | Pickup: Indiranagar Metro → Electronic City Phase 1",
                    isDemo = true,
                    demoScenarioLabel = scenario.title
                )
                val id = repository.recordRideEvent(record)
                processor.appendDiagnostic(
                    title = "[DEMO] Authorized Acceptance Simulation (#$id)",
                    detail = "Simulated official partner dispatch acceptance for ${platform.displayName}."
                )
                "Simulated [DEMO] ACCEPTED_BY_AUTHORIZED_INTEGRATION for ${platform.displayName} at ₹${targetFare.toInt()}."
            }
        }
    }

    private fun buildEvent(
        platform: RidePlatform,
        title: String,
        body: String,
        timestamp: Long,
        scenario: String
    ): NormalizedNotificationEvent {
        return NormalizedNotificationEvent(
            notificationKey = "demo_${platform.id}_$timestamp",
            packageName = platform.packageName,
            title = title,
            text = body,
            bigText = body,
            subText = "DEMO SIMULATION",
            postedAtMillis = timestamp,
            isDemo = true,
            demoScenarioName = scenario
        )
    }
}
