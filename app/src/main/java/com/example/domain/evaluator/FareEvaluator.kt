package com.example.domain.evaluator

import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.ParsedField
import com.example.domain.model.ParsedRideRequest
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RideInfoAvailability

/**
 * Pure, stateless, reusable FareEvaluator for evaluating incoming ride fares
 * against platform-specific minimum and maximum INR configurations.
 *
 * Boundary values are inclusive:
 *   rideFare >= minimumFare && rideFare <= maximumFare
 *
 * Unknown or invalid fares are NEVER treated as eligible.
 */
class FareEvaluator {

    fun evaluate(
        fareField: ParsedField<Double>,
        config: PlatformFareConfig
    ): FareEvaluationResult {
        if (!config.platformEnabled) {
            return FareEvaluationResult.PLATFORM_DISABLED
        }

        val validation = config.validate()
        if (!validation.isValid) {
            return FareEvaluationResult.INVALID_CONFIGURATION
        }

        if (fareField.availability != RideInfoAvailability.AVAILABLE || fareField.value == null) {
            return FareEvaluationResult.UNKNOWN_FARE
        }

        val rideFare = fareField.value
        if (rideFare.isNaN() || rideFare.isInfinite() || rideFare <= 0.0) {
            return FareEvaluationResult.UNKNOWN_FARE
        }

        // If the driver explicitly disabled fare range filtering for an enabled platform,
        // any valid positive fare is eligible.
        if (!config.fareFilterEnabled) {
            return FareEvaluationResult.ELIGIBLE
        }

        return when {
            rideFare < config.minimumFareInr -> FareEvaluationResult.BELOW_MINIMUM
            rideFare > config.maximumFareInr -> FareEvaluationResult.ABOVE_MAXIMUM
            else -> FareEvaluationResult.ELIGIBLE
        }
    }

    fun evaluateRawFare(
        rideFare: Double?,
        config: PlatformFareConfig
    ): FareEvaluationResult {
        val field: ParsedField<Double> = when {
            rideFare == null -> ParsedField<Double>(null, RideInfoAvailability.UNAVAILABLE)
            rideFare.isNaN() || rideFare <= 0.0 -> ParsedField<Double>(rideFare, RideInfoAvailability.INVALID)
            else -> ParsedField<Double>(rideFare, RideInfoAvailability.AVAILABLE)
        }
        return evaluate(field, config)
    }
}

/**
 * Evaluates optional secondary ride filters (distance, ₹/km rate, pickup area, drop area).
 * Never invents missing ride information: if a filter is enabled and the required ride
 * telemetry is unavailable or invalid, it explicitly flags that condition.
 */
class AdditionalFilterEvaluator {

    fun evaluate(
        request: ParsedRideRequest,
        filters: AdditionalRideFilters
    ): AdditionalFilterResult {
        // 1. Minimum Trip Distance
        if (filters.minDistanceEnabled) {
            when (request.tripDistanceKm.availability) {
                RideInfoAvailability.UNAVAILABLE -> return AdditionalFilterResult.DISTANCE_UNAVAILABLE
                RideInfoAvailability.INVALID -> return AdditionalFilterResult.DISTANCE_INVALID
                RideInfoAvailability.AVAILABLE -> {
                    val dist = request.tripDistanceKm.value ?: return AdditionalFilterResult.DISTANCE_UNAVAILABLE
                    if (dist < filters.minDistanceKm) {
                        return AdditionalFilterResult.DISTANCE_BELOW_MIN
                    }
                }
            }
        }

        // 2. Maximum Trip Distance
        if (filters.maxDistanceEnabled) {
            when (request.tripDistanceKm.availability) {
                RideInfoAvailability.UNAVAILABLE -> return AdditionalFilterResult.DISTANCE_UNAVAILABLE
                RideInfoAvailability.INVALID -> return AdditionalFilterResult.DISTANCE_INVALID
                RideInfoAvailability.AVAILABLE -> {
                    val dist = request.tripDistanceKm.value ?: return AdditionalFilterResult.DISTANCE_UNAVAILABLE
                    if (dist > filters.maxDistanceKm) {
                        return AdditionalFilterResult.DISTANCE_ABOVE_MAX
                    }
                }
            }
        }

        // 3. Minimum Estimated Fare Per Kilometre (₹/km)
        if (filters.minFarePerKmEnabled) {
            val rateField = request.estimatedFarePerKmInr
            if (!rateField.isAvailable || rateField.value == null) {
                return AdditionalFilterResult.RATE_UNAVAILABLE
            }
            if (rateField.value < filters.minFarePerKmInr) {
                return AdditionalFilterResult.RATE_BELOW_MIN
            }
        }

        // 4. Preferred Pickup Locations
        if (filters.preferredPickupEnabled && filters.preferredPickupAreas.isNotEmpty()) {
            val pickupField = request.pickupLocation
            if (!pickupField.isAvailable || pickupField.value.isNullOrBlank()) {
                return AdditionalFilterResult.PICKUP_UNAVAILABLE
            }
            val matchesPickup = filters.preferredPickupAreas.any { preferred ->
                pickupField.value.contains(preferred.trim(), ignoreCase = true)
            }
            if (!matchesPickup) {
                return AdditionalFilterResult.PICKUP_NOT_PREFERRED
            }
        }

        // 5. Preferred Destination Areas
        if (filters.preferredDestinationEnabled && filters.preferredDestinationAreas.isNotEmpty()) {
            val destField = request.destinationLocation
            if (!destField.isAvailable || destField.value.isNullOrBlank()) {
                return AdditionalFilterResult.DESTINATION_UNAVAILABLE
            }
            val matchesDest = filters.preferredDestinationAreas.any { preferred ->
                destField.value.contains(preferred.trim(), ignoreCase = true)
            }
            if (!matchesDest) {
                return AdditionalFilterResult.DESTINATION_NOT_PREFERRED
            }
        }

        return AdditionalFilterResult.PASSED
    }
}
