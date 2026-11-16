package com.example

import com.example.domain.evaluator.AdditionalFilterEvaluator
import com.example.domain.evaluator.FareEvaluator
import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RideInfoAvailability
import com.example.domain.model.RidePlatform
import com.example.monitoring.processor.OlaNotificationParser
import com.example.monitoring.processor.RapidoNotificationParser
import com.example.monitoring.processor.UberNotificationParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    private val fareEvaluator = FareEvaluator()
    private val additionalFilterEvaluator = AdditionalFilterEvaluator()
    private val uberParser = UberNotificationParser()
    private val rapidoParser = RapidoNotificationParser()
    private val olaParser = OlaNotificationParser()

    @Test
    fun `FareEvaluator inclusive boundaries return ELIGIBLE`() {
        val uberConfig = PlatformFareConfig(
            platform = RidePlatform.UBER,
            platformEnabled = true,
            fareFilterEnabled = true,
            minimumFareInr = 100.0,
            maximumFareInr = 500.0
        )

        assertEquals(FareEvaluationResult.ELIGIBLE, fareEvaluator.evaluateRawFare(100.0, uberConfig))
        assertEquals(FareEvaluationResult.ELIGIBLE, fareEvaluator.evaluateRawFare(275.0, uberConfig))
        assertEquals(FareEvaluationResult.ELIGIBLE, fareEvaluator.evaluateRawFare(500.0, uberConfig))
    }

    @Test
    fun `FareEvaluator returns BELOW_MINIMUM and ABOVE_MAXIMUM outside bounds`() {
        val rapidoConfig = PlatformFareConfig(
            platform = RidePlatform.RAPIDO,
            platformEnabled = true,
            fareFilterEnabled = true,
            minimumFareInr = 50.0,
            maximumFareInr = 300.0
        )

        assertEquals(FareEvaluationResult.BELOW_MINIMUM, fareEvaluator.evaluateRawFare(49.5, rapidoConfig))
        assertEquals(FareEvaluationResult.ABOVE_MAXIMUM, fareEvaluator.evaluateRawFare(300.5, rapidoConfig))
    }

    @Test
    fun `FareEvaluator never treats unknown or invalid fares as eligible`() {
        val olaConfig = PlatformFareConfig(
            platform = RidePlatform.OLA,
            platformEnabled = true,
            fareFilterEnabled = false, // Even when filter is off, unknown fare is never eligible
            minimumFareInr = 100.0,
            maximumFareInr = 600.0
        )

        assertEquals(FareEvaluationResult.UNKNOWN_FARE, fareEvaluator.evaluateRawFare(null, olaConfig))
        assertEquals(FareEvaluationResult.UNKNOWN_FARE, fareEvaluator.evaluateRawFare(-20.0, olaConfig))
        assertEquals(FareEvaluationResult.UNKNOWN_FARE, fareEvaluator.evaluateRawFare(0.0, olaConfig))
        assertEquals(FareEvaluationResult.UNKNOWN_FARE, fareEvaluator.evaluateRawFare(Double.NaN, olaConfig))
    }

    @Test
    fun `FareEvaluator checks PLATFORM_DISABLED and INVALID_CONFIGURATION`() {
        val disabledConfig = PlatformFareConfig(
            platform = RidePlatform.UBER,
            platformEnabled = false,
            minimumFareInr = 100.0,
            maximumFareInr = 500.0
        )
        assertEquals(FareEvaluationResult.PLATFORM_DISABLED, fareEvaluator.evaluateRawFare(250.0, disabledConfig))

        val invalidConfig = PlatformFareConfig(
            platform = RidePlatform.UBER,
            platformEnabled = true,
            minimumFareInr = 600.0,
            maximumFareInr = 200.0
        )
        assertFalse(invalidConfig.validate().isValid)
        assertEquals(FareEvaluationResult.INVALID_CONFIGURATION, fareEvaluator.evaluateRawFare(350.0, invalidConfig))

        val negativeMinConfig = PlatformFareConfig(
            platform = RidePlatform.RAPIDO,
            platformEnabled = true,
            minimumFareInr = -10.0,
            maximumFareInr = 200.0
        )
        assertFalse(negativeMinConfig.validate().isValid)
    }

    @Test
    fun `Platform parsers extract available INR fare, distance, pickup, and drop without inventing missing fields`() {
        val uberEvent = NormalizedNotificationEvent(
            notificationKey = "k1",
            packageName = RidePlatform.UBER.packageName,
            title = "Uber Driver • Trip Request",
            text = "Fare: ₹320 | Trip: 8.0 km | Pickup: Indiranagar | Drop: Whitefield",
            bigText = null,
            subText = null,
            postedAtMillis = 1000L
        )
        val parsedUber = uberParser.parse(uberEvent)
        assertEquals(RidePlatform.UBER, parsedUber.platform)
        assertEquals(320.0, parsedUber.fareInr.value ?: 0.0, 0.01)
        assertEquals(8.0, parsedUber.tripDistanceKm.value ?: 0.0, 0.01)
        assertEquals(40.0, parsedUber.estimatedFarePerKmInr.value ?: 0.0, 0.01)
        assertEquals("Indiranagar", parsedUber.pickupLocation.value)
        assertEquals("Whitefield", parsedUber.destinationLocation.value)

        // Sparse notification without fare or drop
        val sparseRapidoEvent = NormalizedNotificationEvent(
            notificationKey = "k2",
            packageName = RidePlatform.RAPIDO.packageName,
            title = "Rapido Captain Ping",
            text = "New ride request nearby • Tap to view",
            bigText = null,
            subText = null,
            postedAtMillis = 2000L
        )
        val parsedSparse = rapidoParser.parse(sparseRapidoEvent)
        assertNull(parsedSparse.fareInr.value)
        assertEquals(RideInfoAvailability.UNAVAILABLE, parsedSparse.fareInr.availability)
        assertNull(parsedSparse.tripDistanceKm.value)
        assertEquals(RideInfoAvailability.UNAVAILABLE, parsedSparse.tripDistanceKm.availability)
        assertNull(parsedSparse.pickupLocation.value)
        assertEquals(RideInfoAvailability.UNAVAILABLE, parsedSparse.pickupLocation.availability)
    }

    @Test
    fun `AdditionalFilterEvaluator distinguishes available vs unavailable ride telemetry`() {
        val olaEventWithFullDetails = olaParser.parse(
            NormalizedNotificationEvent(
                notificationKey = "k3",
                packageName = RidePlatform.OLA.packageName,
                title = "Ola Prime",
                text = "Fare: ₹240 | Trip: 10.0 km | Pickup: Koramangala | Drop: Airport",
                bigText = null,
                subText = null,
                postedAtMillis = 3000L
            )
        )

        val passingFilters = AdditionalRideFilters(
            minDistanceEnabled = true,
            minDistanceKm = 5.0,
            maxDistanceEnabled = true,
            maxDistanceKm = 15.0,
            minFarePerKmEnabled = true,
            minFarePerKmInr = 20.0,
            preferredPickupEnabled = true,
            preferredPickupAreas = listOf("Koramangala", "Indiranagar")
        )
        assertEquals(
            AdditionalFilterResult.PASSED,
            additionalFilterEvaluator.evaluate(olaEventWithFullDetails, passingFilters)
        )
        assertTrue(passingFilters.minDistanceEnabled)

        // Sparse event missing distance when distance filter is enabled -> DISTANCE_UNAVAILABLE
        val sparseEvent = olaParser.parse(
            NormalizedNotificationEvent(
                notificationKey = "k4",
                packageName = RidePlatform.OLA.packageName,
                title = "Ola Mini",
                text = "Fare: ₹180",
                bigText = null,
                subText = null,
                postedAtMillis = 4000L
            )
        )
        assertEquals(
            AdditionalFilterResult.DISTANCE_UNAVAILABLE,
            additionalFilterEvaluator.evaluate(sparseEvent, passingFilters)
        )
    }
}
