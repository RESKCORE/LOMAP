package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class CloudSyncRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun upsertAndGetDriverProfile_authenticatedOwner_succeeds() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        val saveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.upsertDriverProfile(
                displayName = "Rajesh Kumar",
                email = ALICE_EMAIL,
                city = "Bengaluru, KA",
                autoAcceptMillisTarget = 3
            )
        }
        assertTrue(saveResult.isSuccess)

        val profileResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getDriverProfile(aliceUid)
        }
        assertTrue(profileResult.isSuccess)
        val profile = profileResult.getOrNull()
        assertNotNull(profile)
        assertEquals("Rajesh Kumar", profile?.displayName)
    }

    @Test
    fun syncAndObserveRideEvents_authenticatedOwner_emitsRealtimeUpdates() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = CloudSyncRepository(firestore)

        val eventIdResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.syncRideEventToCloud(
                platformId = "UBER",
                fareInr = 285.0,
                isEligible = true,
                acceptanceOutcome = "ACCEPTED_BY_AUTHORIZED_INTEGRATION",
                responseLatencyMs = 1.4,
                pickupLocation = "Indiranagar",
                destinationLocation = "Koramangala"
            )
        }
        assertTrue(eventIdResult.isSuccess)
        val eventId = eventIdResult.getOrThrow()

        val emittedEvents = withTimeout(FLOW_TIMEOUT_MS) {
            repository.observeCloudRideEvents().first { list -> list.any { it.id == eventId } }
        }
        assertTrue(emittedEvents.any { it.id == eventId && it.fareInr == 285.0 })
    }

    @Test
    fun getDriverProfile_crossUserAccess_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudSyncRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.upsertDriverProfile("Alice Driver", ALICE_EMAIL, "Bengaluru").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudSyncRepository(firestore)
        val crossReadResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.getDriverProfile(aliceUid)
        }
        assertTrue(crossReadResult.isFailure)
        val fsEx = crossReadResult.exceptionOrNull()?.unwrapFirestoreException()
        assertNotNull(fsEx)
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    @Test
    fun getCloudRideEvents_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        auth.signOut()
        val repository = CloudSyncRepository(firestore)

        val unauthResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.getCloudRideEvents(aliceUid)
        }
        assertTrue(unauthResult.isFailure)
        val fsEx = unauthResult.exceptionOrNull()?.unwrapFirestoreException()
        if (fsEx == null) {
            fail("Expected FirebaseFirestoreException, got ${unauthResult.exceptionOrNull()}")
        }
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsEx?.code)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_driver@test.com"
        const val BOB_EMAIL = "bob_driver@test.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 4000L
    }
}
