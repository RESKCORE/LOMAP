package com.example.monitoring.processor

import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.ParsedField
import com.example.domain.model.ParsedRideRequest
import com.example.domain.model.RideInfoAvailability
import com.example.domain.model.RidePlatform

interface PlatformNotificationParser {
    val platform: RidePlatform
    fun parse(event: NormalizedNotificationEvent): ParsedRideRequest
}

/**
 * Shared extraction helpers that strictly distinguish between:
 *  - RideInfoAvailability.AVAILABLE
 *  - RideInfoAvailability.UNAVAILABLE
 *  - RideInfoAvailability.INVALID
 *
 * Never fabricates or guesses missing fields.
 */
object NotificationRegexExtractor {

    // Matches ₹245, ₹ 1,240.50, INR 185, Rs. 320, Rs 95
    private val FARE_REGEX = Regex(
        """(?:₹|INR|Rs\.?)\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)""",
        RegexOption.IGNORE_CASE
    )

    // Matches "Trip: 8.4 km", "Distance: 12.5 km", "8.4 km trip", "Trip 6 km"
    private val TRIP_DISTANCE_REGEX = Regex(
        """(?:trip|ride|dist(?:ance)?)\s*[:\-]?\s*(-?[0-9]+(?:\.[0-9]+)?)\s*km|(-?[0-9]+(?:\.[0-9]+)?)\s*km\s*(?:trip|ride)""",
        RegexOption.IGNORE_CASE
    )

    // Fallback single km value when labeled simply e.g. "• 7.2 km •"
    private val GENERIC_KM_REGEX = Regex(
        """(?<![a-zA-Z0-9])(-?[0-9]+(?:\.[0-9]+)?)\s*km\b""",
        RegexOption.IGNORE_CASE
    )

    // Matches "Pickup: Indiranagar" or "From: Koramangala 4th Block"
    private val PICKUP_REGEX = Regex(
        """(?:Pickup|From|Pick-up)\s*[:\-]\s*([^|•\n\r]+?)(?=\s*(?:\||•|Drop|To|Dest|$|\n))""",
        RegexOption.IGNORE_CASE
    )

    // Matches "Drop: Whitefield" or "To: Kempegowda Airport" or "Dest: HSR Layout"
    private val DROP_REGEX = Regex(
        """(?:Drop|To|Dest(?:ination)?)\s*[:\-]\s*([^|•\n\r]+?)(?=\s*(?:\||•|$|\n))""",
        RegexOption.IGNORE_CASE
    )

    fun extractFare(corpus: String): ParsedField<Double> {
        if (corpus.isBlank()) return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        // Check for explicitly malformed fare markers like "₹--" or "₹NaN" or "₹-50"
        if (corpus.contains("₹-") || corpus.contains("INR -", ignoreCase = true)) {
            return ParsedField(null, RideInfoAvailability.INVALID)
        }
        val match = FARE_REGEX.find(corpus) ?: return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val rawNumber = match.groupValues[1].replace(",", "").trim()
        val parsed = rawNumber.toDoubleOrNull() ?: return ParsedField(null, RideInfoAvailability.INVALID)
        return if (parsed <= 0.0) {
            ParsedField(parsed, RideInfoAvailability.INVALID)
        } else {
            ParsedField(parsed, RideInfoAvailability.AVAILABLE)
        }
    }

    fun extractTripDistance(corpus: String): ParsedField<Double> {
        if (corpus.isBlank()) return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val explicitMatch = TRIP_DISTANCE_REGEX.find(corpus)
        val rawValue = if (explicitMatch != null) {
            explicitMatch.groupValues[1].ifBlank { explicitMatch.groupValues[2] }
        } else {
            val genericMatch = GENERIC_KM_REGEX.find(corpus) ?: return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
            genericMatch.groupValues[1]
        }
        val value = rawValue.toDoubleOrNull() ?: return ParsedField(null, RideInfoAvailability.INVALID)
        return if (value <= 0.0 || value > 1000.0) {
            ParsedField(value, RideInfoAvailability.INVALID)
        } else {
            ParsedField(value, RideInfoAvailability.AVAILABLE)
        }
    }

    fun extractPickup(corpus: String): ParsedField<String> {
        if (corpus.isBlank()) return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val match = PICKUP_REGEX.find(corpus) ?: return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val location = match.groupValues[1].trim()
        return if (location.length < 2) {
            ParsedField(null, RideInfoAvailability.INVALID)
        } else {
            ParsedField(location, RideInfoAvailability.AVAILABLE)
        }
    }

    fun extractDestination(corpus: String): ParsedField<String> {
        if (corpus.isBlank()) return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val match = DROP_REGEX.find(corpus) ?: return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        val location = match.groupValues[1].trim()
        return if (location.length < 2) {
            ParsedField(null, RideInfoAvailability.INVALID)
        } else {
            ParsedField(location, RideInfoAvailability.AVAILABLE)
        }
    }

    fun computeRatePerKm(
        fare: ParsedField<Double>,
        distance: ParsedField<Double>
    ): ParsedField<Double> {
        if (fare.availability == RideInfoAvailability.INVALID || distance.availability == RideInfoAvailability.INVALID) {
            return ParsedField(null, RideInfoAvailability.INVALID)
        }
        val f = fare.value
        val d = distance.value
        if (!fare.isAvailable || !distance.isAvailable || f == null || d == null || d <= 0.0) {
            return ParsedField(null, RideInfoAvailability.UNAVAILABLE)
        }
        val rate = kotlin.math.round((f / d) * 10.0) / 10.0
        return ParsedField(rate, RideInfoAvailability.AVAILABLE)
    }

    fun computeEventHash(platform: RidePlatform, title: String, body: String): String {
        val normalized = "${platform.id}|${title.trim().lowercase()}|${body.trim().lowercase()}"
        return normalized.hashCode().toUInt().toString(16)
    }
}

class UberNotificationParser : PlatformNotificationParser {
    override val platform: RidePlatform = RidePlatform.UBER

    override fun parse(event: NormalizedNotificationEvent): ParsedRideRequest {
        val title = event.title?.trim().orEmpty()
        val body = (event.bigText?.takeIf { it.isNotBlank() } ?: event.text).orEmpty().trim()
        val corpus = "$title | $body"

        val fare = NotificationRegexExtractor.extractFare(corpus)
        val distance = NotificationRegexExtractor.extractTripDistance(corpus)
        val pickup = NotificationRegexExtractor.extractPickup(corpus)
        val destination = NotificationRegexExtractor.extractDestination(corpus)
        val rate = NotificationRegexExtractor.computeRatePerKm(fare, distance)

        return ParsedRideRequest(
            eventHash = NotificationRegexExtractor.computeEventHash(platform, title, body),
            platform = platform,
            rawTitle = title.ifBlank { "Uber Driver Dispatch" },
            rawBody = body,
            fareInr = fare,
            tripDistanceKm = distance,
            pickupDistanceKm = ParsedField(null, RideInfoAvailability.UNAVAILABLE),
            estimatedFarePerKmInr = rate,
            pickupLocation = pickup,
            destinationLocation = destination,
            timestampMillis = event.postedAtMillis,
            isDemo = event.isDemo,
            demoScenarioName = event.demoScenarioName
        )
    }
}

class RapidoNotificationParser : PlatformNotificationParser {
    override val platform: RidePlatform = RidePlatform.RAPIDO

    override fun parse(event: NormalizedNotificationEvent): ParsedRideRequest {
        val title = event.title?.trim().orEmpty()
        val body = (event.bigText?.takeIf { it.isNotBlank() } ?: event.text).orEmpty().trim()
        val corpus = "$title | $body"

        val fare = NotificationRegexExtractor.extractFare(corpus)
        val distance = NotificationRegexExtractor.extractTripDistance(corpus)
        val pickup = NotificationRegexExtractor.extractPickup(corpus)
        val destination = NotificationRegexExtractor.extractDestination(corpus)
        val rate = NotificationRegexExtractor.computeRatePerKm(fare, distance)

        return ParsedRideRequest(
            eventHash = NotificationRegexExtractor.computeEventHash(platform, title, body),
            platform = platform,
            rawTitle = title.ifBlank { "Rapido Captain Alert" },
            rawBody = body,
            fareInr = fare,
            tripDistanceKm = distance,
            pickupDistanceKm = ParsedField(null, RideInfoAvailability.UNAVAILABLE),
            estimatedFarePerKmInr = rate,
            pickupLocation = pickup,
            destinationLocation = destination,
            timestampMillis = event.postedAtMillis,
            isDemo = event.isDemo,
            demoScenarioName = event.demoScenarioName
        )
    }
}

class OlaNotificationParser : PlatformNotificationParser {
    override val platform: RidePlatform = RidePlatform.OLA

    override fun parse(event: NormalizedNotificationEvent): ParsedRideRequest {
        val title = event.title?.trim().orEmpty()
        val body = (event.bigText?.takeIf { it.isNotBlank() } ?: event.text).orEmpty().trim()
        val corpus = "$title | $body"

        val fare = NotificationRegexExtractor.extractFare(corpus)
        val distance = NotificationRegexExtractor.extractTripDistance(corpus)
        val pickup = NotificationRegexExtractor.extractPickup(corpus)
        val destination = NotificationRegexExtractor.extractDestination(corpus)
        val rate = NotificationRegexExtractor.computeRatePerKm(fare, distance)

        return ParsedRideRequest(
            eventHash = NotificationRegexExtractor.computeEventHash(platform, title, body),
            platform = platform,
            rawTitle = title.ifBlank { "Ola Driver Booking" },
            rawBody = body,
            fareInr = fare,
            tripDistanceKm = distance,
            pickupDistanceKm = ParsedField(null, RideInfoAvailability.UNAVAILABLE),
            estimatedFarePerKmInr = rate,
            pickupLocation = pickup,
            destinationLocation = destination,
            timestampMillis = event.postedAtMillis,
            isDemo = event.isDemo,
            demoScenarioName = event.demoScenarioName
        )
    }
}
