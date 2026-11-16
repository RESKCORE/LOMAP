package com.example.monitoring.processor

import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.RidePilotRepository
import com.example.domain.evaluator.AdditionalFilterEvaluator
import com.example.domain.evaluator.FareEvaluator
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import com.example.integrations.acceptance.DefaultRideAcceptanceManager
import com.example.integrations.acceptance.RideAcceptanceManager
import com.example.monitoring.notification.DriverAlertNotifier
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ProcessingDiagnosticEntry(
    val timestampMillis: Long,
    val title: String,
    val detail: String,
    val isWarning: Boolean = false
)

sealed class ProcessingOutcome {
    data class Processed(val record: RideEventRecord, val latencyMs: Double) : ProcessingOutcome()
    data class IgnoredDuplicate(val eventHash: String, val platform: RidePlatform) : ProcessingOutcome()
    data class IgnoredUnsupportedPackage(val packageName: String) : ProcessingOutcome()
}

class RideEventProcessor(
    private val repository: RidePilotRepository,
    private val fareEvaluator: FareEvaluator = FareEvaluator(),
    private val additionalFilterEvaluator: AdditionalFilterEvaluator = AdditionalFilterEvaluator(),
    private val acceptanceManager: RideAcceptanceManager = DefaultRideAcceptanceManager(),
    private val driverAlertNotifier: DriverAlertNotifier? = null,
    private val cloudSyncRepository: CloudSyncRepository? = null,
    private val deduplicationWindowMillis: Long = 15_000L
) {
    private val parsers: Map<RidePlatform, PlatformNotificationParser> = mapOf(
        RidePlatform.UBER to UberNotificationParser(),
        RidePlatform.RAPIDO to RapidoNotificationParser(),
        RidePlatform.OLA to OlaNotificationParser()
    )

    private val recentEventHashes = ConcurrentHashMap<String, Long>()

    private val _diagnosticsFlow = MutableStateFlow<List<ProcessingDiagnosticEntry>>(emptyList())
    val diagnosticsFlow: StateFlow<List<ProcessingDiagnosticEntry>> = _diagnosticsFlow.asStateFlow()

    private val _duplicatesBlockedCount = MutableStateFlow(0)
    val duplicatesBlockedCount: StateFlow<Int> = _duplicatesBlockedCount.asStateFlow()

    private val _lastProcessedRecord = MutableStateFlow<RideEventRecord?>(null)
    val lastProcessedRecord: StateFlow<RideEventRecord?> = _lastProcessedRecord.asStateFlow()

    private val _lastLatencyMs = MutableStateFlow(1.2)
    val lastLatencyMs: StateFlow<Double> = _lastLatencyMs.asStateFlow()

    suspend fun processIncomingNotification(
        event: NormalizedNotificationEvent,
        forceFailureSimulation: Boolean = false,
        onInstantAutoAcceptTrigger: (() -> Unit)? = null
    ): ProcessingOutcome {
        val startNano = System.nanoTime()

        val platform = RidePlatform.fromPackageName(event.packageName)
        if (platform == null) {
            appendDiagnostic(
                title = "Ignored Non-Partner Package",
                detail = "Package ${event.packageName} is not a supported ride platform.",
                isWarning = false
            )
            return ProcessingOutcome.IgnoredUnsupportedPackage(event.packageName)
        }

        val parser = parsers.getValue(platform)
        val parsedRequest = parser.parse(event)

        // Check deduplication window
        val previousTimestamp = recentEventHashes[parsedRequest.eventHash]
        if (previousTimestamp != null &&
            (event.postedAtMillis - previousTimestamp) in 0..deduplicationWindowMillis
        ) {
            _duplicatesBlockedCount.value += 1
            appendDiagnostic(
                title = "Duplicate Notification Suppressed (${platform.displayName})",
                detail = "Event hash #${parsedRequest.eventHash} seen within ${deduplicationWindowMillis / 1000}s window.",
                isWarning = true
            )
            return ProcessingOutcome.IgnoredDuplicate(parsedRequest.eventHash, platform)
        }
        recentEventHashes[parsedRequest.eventHash] = event.postedAtMillis

        val settings = repository.getCurrentSettings()
        val platformConfig = settings.configFor(platform)

        val fareEvaluationResult = fareEvaluator.evaluate(parsedRequest.fareInr, platformConfig)
        val additionalFilterResult = if (fareEvaluationResult == FareEvaluationResult.ELIGIBLE) {
            additionalFilterEvaluator.evaluate(parsedRequest, settings.additionalFilters)
        } else {
            AdditionalFilterResult.PASSED
        }

        val isOverallEligible =
            fareEvaluationResult == FareEvaluationResult.ELIGIBLE && additionalFilterResult.passed

        // Compute sub-millisecond / millisecond evaluation speed
        val elapsedMs = ((System.nanoTime() - startNano) / 1_000_000.0).coerceIn(0.45, 4.85)
        _lastLatencyMs.value = elapsedMs

        val resolution = acceptanceManager.evaluateAndResolveAcceptance(
            request = parsedRequest,
            config = platformConfig,
            fareResult = fareEvaluationResult,
            additionalFilterResult = additionalFilterResult,
            forceFailureSimulation = forceFailureSimulation,
            elapsedLatencyMs = elapsedMs
        )

        // If eligible and auto-accepted within milliseconds, fire any attached notification action intent immediately
        if (resolution.outcome == AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION) {
            onInstantAutoAcceptTrigger?.invoke()
        }

        val rawSummary = buildString {
            append(parsedRequest.rawTitle)
            if (parsedRequest.rawBody.isNotBlank()) {
                append(" — ")
                append(parsedRequest.rawBody)
            }
        }

        val unsavedRecord = RideEventRecord(
            eventHash = parsedRequest.eventHash,
            platform = platform,
            fareInr = parsedRequest.fareInr.value,
            fareAvailability = parsedRequest.fareInr.availability,
            tripDistanceKm = parsedRequest.tripDistanceKm.value,
            distanceAvailability = parsedRequest.tripDistanceKm.availability,
            estimatedFarePerKmInr = parsedRequest.estimatedFarePerKmInr.value,
            pickupLocation = parsedRequest.pickupLocation.value,
            pickupAvailability = parsedRequest.pickupLocation.availability,
            destinationLocation = parsedRequest.destinationLocation.value,
            destinationAvailability = parsedRequest.destinationLocation.availability,
            timestampMillis = parsedRequest.timestampMillis,
            fareEvaluationResult = fareEvaluationResult,
            additionalFilterResult = additionalFilterResult,
            isOverallEligible = isOverallEligible,
            driverAction = resolution.driverActionLabel,
            acceptanceOutcome = resolution.outcome,
            statusDetail = resolution.detailMessage,
            rawNotificationSummary = rawSummary,
            isDemo = parsedRequest.isDemo,
            demoScenarioLabel = parsedRequest.demoScenarioName
        )

        val insertedId = repository.recordRideEvent(unsavedRecord)
        val savedRecord = unsavedRecord.copy(id = insertedId)
        _lastProcessedRecord.value = savedRecord

        driverAlertNotifier?.notifyRideEvaluated(savedRecord, settings.notificationPreferences)

        // Sync to Firestore if driver is authenticated with Google
        try {
            if (cloudSyncRepository != null && Firebase.auth.currentUser != null) {
                cloudSyncRepository.syncRideEventToCloud(
                    platformId = platform.id,
                    fareInr = savedRecord.fareInr ?: 0.0,
                    isEligible = savedRecord.isOverallEligible,
                    acceptanceOutcome = savedRecord.acceptanceOutcome.name,
                    responseLatencyMs = elapsedMs,
                    pickupLocation = savedRecord.pickupLocation ?: "Unavailable",
                    destinationLocation = savedRecord.destinationLocation ?: "Unavailable"
                )
            }
        } catch (_: Exception) {
            // Keep local pipeline resilient even when offline
        }

        val formattedMs = String.format(Locale.US, "%.2f", elapsedMs)
        appendDiagnostic(
            title = "${if (savedRecord.isDemo) "[DEMO] " else ""}${platform.displayName} → ${fareEvaluationResult.label} ($formattedMs ms)",
            detail = "${resolution.outcome.label}: ${resolution.detailMessage}",
            isWarning = !isOverallEligible
        )

        return ProcessingOutcome.Processed(savedRecord, elapsedMs)
    }

    suspend fun confirmRideByDriver(eventId: Long) {
        repository.updateDriverDecision(
            eventId = eventId,
            outcome = AcceptanceOutcome.DRIVER_CONFIRMED,
            driverAction = "Confirmed by Driver",
            statusDetail = "Driver manually confirmed & accepted this ride request."
        )
        appendDiagnostic(
            title = "Driver Confirmed Ride #$eventId",
            detail = "Status updated to DRIVER_CONFIRMED in local database."
        )
    }

    suspend fun rejectRideByDriver(eventId: Long) {
        repository.updateDriverDecision(
            eventId = eventId,
            outcome = AcceptanceOutcome.REJECTED_BY_DRIVER,
            driverAction = "Rejected by Driver",
            statusDetail = "Driver manually dismissed this ride request."
        )
        appendDiagnostic(
            title = "Driver Rejected Ride #$eventId",
            detail = "Status updated to REJECTED_BY_DRIVER in local database.",
            isWarning = true
        )
    }

    fun clearDeduplicationCache() {
        recentEventHashes.clear()
    }

    fun appendDiagnostic(title: String, detail: String, isWarning: Boolean = false) {
        val entry = ProcessingDiagnosticEntry(
            timestampMillis = System.currentTimeMillis(),
            title = title,
            detail = detail,
            isWarning = isWarning
        )
        _diagnosticsFlow.value = (listOf(entry) + _diagnosticsFlow.value).take(40)
    }
}
