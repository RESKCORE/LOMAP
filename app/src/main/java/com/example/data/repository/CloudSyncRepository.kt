package com.example.data.repository

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@IgnoreExtraProperties
data class CloudDriverProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val city: String = "Bengaluru, KA",
    val autoAcceptMillisTarget: Long = 5L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudRideEvent(
    @DocumentId val id: String = "",
    val userId: String = "",
    val platformId: String = "UBER",
    val fareInr: Double = 0.0,
    val isEligible: Boolean = false,
    val acceptanceOutcome: String = "ACCEPTED_BY_AUTHORIZED_INTEGRATION",
    val responseLatencyMs: Double = 1.2,
    val pickupLocation: String = "",
    val destinationLocation: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

class CloudSyncRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Driver must be signed in with Google before accessing Firestore.")
    }

    suspend fun upsertDriverProfile(
        displayName: String,
        email: String?,
        city: String,
        autoAcceptMillisTarget: Int = 5
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
        val cleanName = displayName.trim().ifBlank { "RidePilot Partner" }.take(80)
        val cleanCity = city.trim().ifBlank { "Bengaluru, KA" }.take(80)
        val cleanTarget = autoAcceptMillisTarget.coerceIn(1, 5000)

        try {
            val existing = docRef.get().await()
            if (existing.exists()) {
                val updatePayload = mutableMapOf<String, Any>(
                    "displayName" to cleanName,
                    "city" to cleanCity,
                    "autoAcceptMillisTarget" to cleanTarget,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    updatePayload["email"] = email.trim().take(120)
                }
                docRef.update(updatePayload).await()
            } else {
                val createPayload = mutableMapOf<String, Any>(
                    "userId" to uid,
                    "displayName" to cleanName,
                    "city" to cleanCity,
                    "autoAcceptMillisTarget" to cleanTarget,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) {
                    createPayload["email"] = email.trim().take(120)
                }
                docRef.set(createPayload).await()
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun getDriverProfile(userId: String = requireUserId()): Result<CloudDriverProfile?> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            val snapshot = docRef.get().await()
            if (snapshot.exists()) {
                snapshot.toObject(
                    CloudDriverProfile::class.java,
                    DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                )
            } else {
                null
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun syncRideEventToCloud(
        platformId: String,
        fareInr: Double,
        isEligible: Boolean,
        acceptanceOutcome: String,
        responseLatencyMs: Double,
        pickupLocation: String,
        destinationLocation: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val eventId = "evt_${UUID.randomUUID().toString().replace("-", "").take(20)}"
        val docRef = db.collection("users").document(uid).collection("ride_events").document(eventId)
        val payload = mapOf(
            "userId" to uid,
            "platformId" to platformId,
            "fareInr" to fareInr.coerceIn(0.0, 100_000.0),
            "isEligible" to isEligible,
            "acceptanceOutcome" to acceptanceOutcome.take(64),
            "responseLatencyMs" to responseLatencyMs.coerceIn(0.0, 60_000.0),
            "pickupLocation" to pickupLocation.take(160),
            "destinationLocation" to destinationLocation.take(160),
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
            eventId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun getCloudRideEvents(userId: String = requireUserId()): Result<List<CloudRideEvent>> = runCatching {
        val colRef = db.collection("users").document(userId).collection("ride_events")
        try {
            val snapshot = colRef.get().await()
            snapshot.toObjects(
                CloudRideEvent::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, "users/$userId/ride_events")
            throw e
        }
    }

    fun observeCloudRideEvents(): Flow<List<CloudRideEvent>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/ride_events"
        emitAll(
            db.collection("users").document(uid).collection("ride_events")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudRideEvent::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }
}
