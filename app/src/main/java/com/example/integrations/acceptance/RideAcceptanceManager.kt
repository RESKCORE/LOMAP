package com.example.integrations.acceptance

import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.ParsedRideRequest
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RidePlatform
import java.util.Locale

interface PlatformAcceptanceAdapter {
    val platform: RidePlatform
    fun isAuthorizedIntegrationAvailable(config: PlatformFareConfig): Boolean
    suspend fun requestAuthorizedAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        elapsedMs: Double
    ): AcceptanceResolution
}

data class AcceptanceResolution(
    val outcome: AcceptanceOutcome,
    val driverActionLabel: String,
    val detailMessage: String,
    val responseLatencyMs: Double = 1.2
)

class UberAcceptanceAdapter : PlatformAcceptanceAdapter {
    override val platform: RidePlatform = RidePlatform.UBER

    override fun isAuthorizedIntegrationAvailable(config: PlatformFareConfig): Boolean {
        return config.authorizedIntegrationSimulated
    }

    override suspend fun requestAuthorizedAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        elapsedMs: Double
    ): AcceptanceResolution {
        val formattedMs = String.format(Locale.US, "%.2f", elapsedMs)
        return if (isAuthorizedIntegrationAvailable(config)) {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION,
                driverActionLabel = "Instant Auto-Accepted ($formattedMs ms)",
                detailMessage = "Auto-accepted in $formattedMs ms — Fare ₹${request.fareInr.value?.toInt() ?: 0} is within Uber range (₹${config.minimumFareInr.toInt()}–₹${config.maximumFareInr.toInt()}).",
                responseLatencyMs = elapsedMs
            )
        } else {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE,
                driverActionLabel = "Awaiting Driver Confirmation",
                detailMessage = "Auto-accept toggle is off for Uber. Driver notified in $formattedMs ms to confirm manually.",
                responseLatencyMs = elapsedMs
            )
        }
    }
}

class RapidoAcceptanceAdapter : PlatformAcceptanceAdapter {
    override val platform: RidePlatform = RidePlatform.RAPIDO

    override fun isAuthorizedIntegrationAvailable(config: PlatformFareConfig): Boolean {
        return config.authorizedIntegrationSimulated
    }

    override suspend fun requestAuthorizedAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        elapsedMs: Double
    ): AcceptanceResolution {
        val formattedMs = String.format(Locale.US, "%.2f", elapsedMs)
        return if (isAuthorizedIntegrationAvailable(config)) {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION,
                driverActionLabel = "Instant Auto-Accepted ($formattedMs ms)",
                detailMessage = "Auto-accepted in $formattedMs ms — Fare ₹${request.fareInr.value?.toInt() ?: 0} is within Rapido range (₹${config.minimumFareInr.toInt()}–₹${config.maximumFareInr.toInt()}).",
                responseLatencyMs = elapsedMs
            )
        } else {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE,
                driverActionLabel = "Awaiting Driver Confirmation",
                detailMessage = "Auto-accept toggle is off for Rapido. Driver notified in $formattedMs ms to confirm manually.",
                responseLatencyMs = elapsedMs
            )
        }
    }
}

class OlaAcceptanceAdapter : PlatformAcceptanceAdapter {
    override val platform: RidePlatform = RidePlatform.OLA

    override fun isAuthorizedIntegrationAvailable(config: PlatformFareConfig): Boolean {
        return config.authorizedIntegrationSimulated
    }

    override suspend fun requestAuthorizedAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        elapsedMs: Double
    ): AcceptanceResolution {
        val formattedMs = String.format(Locale.US, "%.2f", elapsedMs)
        return if (isAuthorizedIntegrationAvailable(config)) {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION,
                driverActionLabel = "Instant Auto-Accepted ($formattedMs ms)",
                detailMessage = "Auto-accepted in $formattedMs ms — Fare ₹${request.fareInr.value?.toInt() ?: 0} is within Ola range (₹${config.minimumFareInr.toInt()}–₹${config.maximumFareInr.toInt()}).",
                responseLatencyMs = elapsedMs
            )
        } else {
            AcceptanceResolution(
                outcome = AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE,
                driverActionLabel = "Awaiting Driver Confirmation",
                detailMessage = "Auto-accept toggle is off for Ola. Driver notified in $formattedMs ms to confirm manually.",
                responseLatencyMs = elapsedMs
            )
        }
    }
}

interface RideAcceptanceManager {
    suspend fun evaluateAndResolveAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        fareResult: FareEvaluationResult,
        additionalFilterResult: AdditionalFilterResult,
        forceFailureSimulation: Boolean = false,
        elapsedLatencyMs: Double = 1.2
    ): AcceptanceResolution

    fun getAdapter(platform: RidePlatform): PlatformAcceptanceAdapter
}

class DefaultRideAcceptanceManager(
    uberAdapter: PlatformAcceptanceAdapter = UberAcceptanceAdapter(),
    rapidoAdapter: PlatformAcceptanceAdapter = RapidoAcceptanceAdapter(),
    olaAdapter: PlatformAcceptanceAdapter = OlaAcceptanceAdapter()
) : RideAcceptanceManager {

    private val adapters: Map<RidePlatform, PlatformAcceptanceAdapter> = mapOf(
        RidePlatform.UBER to uberAdapter,
        RidePlatform.RAPIDO to rapidoAdapter,
        RidePlatform.OLA to olaAdapter
    )

    override fun getAdapter(platform: RidePlatform): PlatformAcceptanceAdapter {
        return adapters.getValue(platform)
    }

    override suspend fun evaluateAndResolveAcceptance(
        request: ParsedRideRequest,
        config: PlatformFareConfig,
        fareResult: FareEvaluationResult,
        additionalFilterResult: AdditionalFilterResult,
        forceFailureSimulation: Boolean,
        elapsedLatencyMs: Double
    ): AcceptanceResolution {
        val safeLatency = elapsedLatencyMs.coerceIn(0.4, 9.9)
        if (forceFailureSimulation) {
            return AcceptanceResolution(
                outcome = AcceptanceOutcome.FAILED,
                driverActionLabel = "Integration Handshake Failed",
                detailMessage = "Partner dispatch endpoint timed out before confirmation.",
                responseLatencyMs = safeLatency
            )
        }

        return when (fareResult) {
            FareEvaluationResult.PLATFORM_DISABLED -> AcceptanceResolution(
                outcome = AcceptanceOutcome.FILTERED_OUT,
                driverActionLabel = "Ignored (Platform Disabled)",
                detailMessage = "${request.platform.appTitle} monitoring is disabled in RidePilot.",
                responseLatencyMs = safeLatency
            )
            FareEvaluationResult.INVALID_CONFIGURATION -> AcceptanceResolution(
                outcome = AcceptanceOutcome.FAILED,
                driverActionLabel = "Blocked (Invalid Config)",
                detailMessage = "Fare range configuration for ${request.platform.displayName} is invalid.",
                responseLatencyMs = safeLatency
            )
            FareEvaluationResult.UNKNOWN_FARE -> AcceptanceResolution(
                outcome = AcceptanceOutcome.UNKNOWN_FARE,
                driverActionLabel = "Manual Review Needed",
                detailMessage = "Notification did not expose a valid INR fare. Never auto-accepted.",
                responseLatencyMs = safeLatency
            )
            FareEvaluationResult.BELOW_MINIMUM -> AcceptanceResolution(
                outcome = AcceptanceOutcome.FILTERED_OUT,
                driverActionLabel = "Filtered Out (Below Min)",
                detailMessage = "Fare is below minimum threshold of ₹${config.minimumFareInr.toInt()}.",
                responseLatencyMs = safeLatency
            )
            FareEvaluationResult.ABOVE_MAXIMUM -> AcceptanceResolution(
                outcome = AcceptanceOutcome.FILTERED_OUT,
                driverActionLabel = "Filtered Out (Above Max)",
                detailMessage = "Fare exceeds maximum threshold of ₹${config.maximumFareInr.toInt()}.",
                responseLatencyMs = safeLatency
            )
            FareEvaluationResult.ELIGIBLE -> {
                if (!additionalFilterResult.passed) {
                    AcceptanceResolution(
                        outcome = AcceptanceOutcome.FILTERED_OUT,
                        driverActionLabel = "Filtered (${additionalFilterResult.label})",
                        detailMessage = "Fare matched range, but secondary filter blocked: ${additionalFilterResult.label}.",
                        responseLatencyMs = safeLatency
                    )
                } else {
                    val adapter = getAdapter(request.platform)
                    adapter.requestAuthorizedAcceptance(request, config, safeLatency)
                }
            }
        }
    }
}
