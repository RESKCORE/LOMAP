package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalFilterResult
import com.example.domain.model.FareEvaluationResult
import com.example.domain.model.MonitoringSessionRecord
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RideInfoAvailability
import com.example.domain.model.RidePlatform
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "ride_events")
data class RideEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventHash: String,
    val platformId: String,
    val fareInr: Double?,
    val fareAvailability: String,
    val tripDistanceKm: Double?,
    val distanceAvailability: String,
    val estimatedFarePerKmInr: Double?,
    val pickupLocation: String?,
    val pickupAvailability: String,
    val destinationLocation: String?,
    val destinationAvailability: String,
    val timestampMillis: Long,
    val fareEvaluationResult: String,
    val additionalFilterResult: String,
    val isOverallEligible: Boolean,
    val driverAction: String,
    val acceptanceOutcome: String,
    val statusDetail: String,
    val rawNotificationSummary: String,
    val isDemo: Boolean,
    val demoScenarioLabel: String?
) {
    fun toDomainModel(): RideEventRecord {
        return RideEventRecord(
            id = id,
            eventHash = eventHash,
            platform = RidePlatform.fromId(platformId),
            fareInr = fareInr,
            fareAvailability = runCatching { RideInfoAvailability.valueOf(fareAvailability) }
                .getOrDefault(RideInfoAvailability.UNAVAILABLE),
            tripDistanceKm = tripDistanceKm,
            distanceAvailability = runCatching { RideInfoAvailability.valueOf(distanceAvailability) }
                .getOrDefault(RideInfoAvailability.UNAVAILABLE),
            estimatedFarePerKmInr = estimatedFarePerKmInr,
            pickupLocation = pickupLocation,
            pickupAvailability = runCatching { RideInfoAvailability.valueOf(pickupAvailability) }
                .getOrDefault(RideInfoAvailability.UNAVAILABLE),
            destinationLocation = destinationLocation,
            destinationAvailability = runCatching { RideInfoAvailability.valueOf(destinationAvailability) }
                .getOrDefault(RideInfoAvailability.UNAVAILABLE),
            timestampMillis = timestampMillis,
            fareEvaluationResult = runCatching { FareEvaluationResult.valueOf(fareEvaluationResult) }
                .getOrDefault(FareEvaluationResult.UNKNOWN_FARE),
            additionalFilterResult = runCatching { AdditionalFilterResult.valueOf(additionalFilterResult) }
                .getOrDefault(AdditionalFilterResult.PASSED),
            isOverallEligible = isOverallEligible,
            driverAction = driverAction,
            acceptanceOutcome = runCatching { AcceptanceOutcome.valueOf(acceptanceOutcome) }
                .getOrDefault(AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE),
            statusDetail = statusDetail,
            rawNotificationSummary = rawNotificationSummary,
            isDemo = isDemo,
            demoScenarioLabel = demoScenarioLabel
        )
    }

    companion object {
        fun fromDomainModel(record: RideEventRecord): RideEventEntity {
            return RideEventEntity(
                id = record.id,
                eventHash = record.eventHash,
                platformId = record.platform.id,
                fareInr = record.fareInr,
                fareAvailability = record.fareAvailability.name,
                tripDistanceKm = record.tripDistanceKm,
                distanceAvailability = record.distanceAvailability.name,
                estimatedFarePerKmInr = record.estimatedFarePerKmInr,
                pickupLocation = record.pickupLocation,
                pickupAvailability = record.pickupAvailability.name,
                destinationLocation = record.destinationLocation,
                destinationAvailability = record.destinationAvailability.name,
                timestampMillis = record.timestampMillis,
                fareEvaluationResult = record.fareEvaluationResult.name,
                additionalFilterResult = record.additionalFilterResult.name,
                isOverallEligible = record.isOverallEligible,
                driverAction = record.driverAction,
                acceptanceOutcome = record.acceptanceOutcome.name,
                statusDetail = record.statusDetail,
                rawNotificationSummary = record.rawNotificationSummary,
                isDemo = record.isDemo,
                demoScenarioLabel = record.demoScenarioLabel
            )
        }
    }
}

@Entity(tableName = "monitoring_sessions")
data class MonitoringSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val stopReason: String?,
    val requestsDetectedCount: Int = 0,
    val eligibleCount: Int = 0
) {
    fun toDomainModel(): MonitoringSessionRecord = MonitoringSessionRecord(
        id = id,
        startedAtMillis = startedAtMillis,
        endedAtMillis = endedAtMillis,
        stopReason = stopReason,
        requestsDetectedCount = requestsDetectedCount,
        eligibleCount = eligibleCount
    )
}

@Dao
interface RideEventDao {
    @Query("SELECT * FROM ride_events ORDER BY timestampMillis DESC")
    fun observeAllRideEvents(): Flow<List<RideEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRideEvent(entity: RideEventEntity): Long

    @Query(
        """
        UPDATE ride_events
        SET acceptanceOutcome = :outcome,
            driverAction = :driverAction,
            statusDetail = :statusDetail
        WHERE id = :id
        """
    )
    suspend fun updateAcceptanceOutcome(
        id: Long,
        outcome: String,
        driverAction: String,
        statusDetail: String
    )

    @Query("DELETE FROM ride_events WHERE id = :id")
    suspend fun deleteEventById(id: Long)

    @Query("DELETE FROM ride_events WHERE isDemo = 1")
    suspend fun clearDemoEventsOnly()

    @Query("DELETE FROM ride_events")
    suspend fun clearAllEvents()
}

@Dao
interface MonitoringSessionDao {
    @Query("SELECT * FROM monitoring_sessions ORDER BY startedAtMillis DESC LIMIT 25")
    fun observeRecentSessions(): Flow<List<MonitoringSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: MonitoringSessionEntity): Long

    @Query(
        """
        UPDATE monitoring_sessions
        SET endedAtMillis = :endedAtMillis,
            stopReason = :stopReason
        WHERE id = :sessionId
        """
    )
    suspend fun closeSession(sessionId: Long, endedAtMillis: Long, stopReason: String)

    @Query("DELETE FROM monitoring_sessions")
    suspend fun clearAllSessions()
}

@Database(
    entities = [RideEventEntity::class, MonitoringSessionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RidePilotDatabase : RoomDatabase() {
    abstract fun rideEventDao(): RideEventDao
    abstract fun monitoringSessionDao(): MonitoringSessionDao

    companion object {
        @Volatile
        private var INSTANCE: RidePilotDatabase? = null

        fun getInstance(context: Context): RidePilotDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    RidePilotDatabase::class.java,
                    "ridepilot_local.db"
                ).fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
