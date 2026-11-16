package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.database.RidePilotDatabase
import com.example.data.datastore.RidePilotPreferencesDataSource
import com.example.data.repository.RidePilotRepository
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.NormalizedNotificationEvent
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RidePlatform
import com.example.monitoring.processor.ProcessingOutcome
import com.example.monitoring.processor.RideEventProcessor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: RidePilotDatabase
    private lateinit var repository: RidePilotRepository
    private lateinit var processor: RideEventProcessor

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, RidePilotDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        val preferencesDataSource = RidePilotPreferencesDataSource(context)
        repository = RidePilotRepository(
            rideEventDao = database.rideEventDao(),
            monitoringSessionDao = database.monitoringSessionDao(),
            preferencesDataSource = preferencesDataSource
        )
        processor = RideEventProcessor(
            repository = repository,
            driverAlertNotifier = null
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app_name resource matches LOMAP`() {
        assertEquals("LOMAP", context.getString(R.string.app_name))
    }

    @Test
    fun `RideEventProcessor persists eligible ride, blocks duplicate within window, and updates driver confirmation`() = runBlocking {
        // Configure Uber ₹100 - ₹500
        repository.savePlatformFareConfig(
            PlatformFareConfig(
                platform = RidePlatform.UBER,
                platformEnabled = true,
                fareFilterEnabled = true,
                minimumFareInr = 100.0,
                maximumFareInr = 500.0
            )
        )

        val event = NormalizedNotificationEvent(
            notificationKey = "uber_1",
            packageName = RidePlatform.UBER.packageName,
            title = "Uber Driver Dispatch",
            text = "Fare: ₹260 | Trip: 6.5 km | Pickup: Indiranagar | Drop: Koramangala",
            bigText = null,
            subText = null,
            postedAtMillis = 10_000L,
            isDemo = true,
            demoScenarioName = "1. Eligible Fare"
        )

        val firstOutcome = processor.processIncomingNotification(event)
        assertTrue(firstOutcome is ProcessingOutcome.Processed)
        val savedRecord = (firstOutcome as ProcessingOutcome.Processed).record
        assertEquals(FareEvaluationResult.ELIGIBLE, savedRecord.fareEvaluationResult)
        assertTrue(savedRecord.isOverallEligible)
        assertEquals(AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION, savedRecord.acceptanceOutcome)
        assertTrue(processor.lastLatencyMs.value < 50.0)

        // Immediate duplicate within 15s deduplication window should be suppressed
        val duplicateOutcome = processor.processIncomingNotification(
            event.copy(postedAtMillis = 12_000L)
        )
        assertTrue(duplicateOutcome is ProcessingOutcome.IgnoredDuplicate)
        assertEquals(1, processor.duplicatesBlockedCount.value)

        // Confirm ride by driver and verify Room persistence
        processor.confirmRideByDriver(savedRecord.id)
        val storedEvents = repository.allRideEventsFlow.first()
        assertEquals(1, storedEvents.size)
        assertEquals(AcceptanceOutcome.DRIVER_CONFIRMED, storedEvents.first().acceptanceOutcome)
    }
}
