package com.example.data.repository

import com.example.data.database.MonitoringSessionDao
import com.example.data.database.MonitoringSessionEntity
import com.example.data.database.RideEventDao
import com.example.data.database.RideEventEntity
import com.example.data.datastore.PersistedAppSettings
import com.example.data.datastore.RidePilotPreferencesDataSource
import com.example.domain.model.AcceptanceOutcome
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DriverProfileSettings
import com.example.domain.model.MonitoringSessionRecord
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RideEventRecord
import com.example.domain.model.RidePlatform
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RidePilotRepository(
    private val rideEventDao: RideEventDao,
    private val monitoringSessionDao: MonitoringSessionDao,
    private val preferencesDataSource: RidePilotPreferencesDataSource
) {
    val settingsFlow: Flow<PersistedAppSettings> = preferencesDataSource.settingsFlow

    val allRideEventsFlow: Flow<List<RideEventRecord>> =
        rideEventDao.observeAllRideEvents().map { list ->
            list.map { it.toDomainModel() }
        }

    val recentSessionsFlow: Flow<List<MonitoringSessionRecord>> =
        monitoringSessionDao.observeRecentSessions().map { list ->
            list.map { it.toDomainModel() }
        }

    suspend fun getCurrentSettings(): PersistedAppSettings = settingsFlow.first()

    suspend fun setOnboardingCompleted(completed: Boolean) {
        preferencesDataSource.setOnboardingCompleted(completed)
    }

    suspend fun savePlatformFareConfig(config: PlatformFareConfig) {
        preferencesDataSource.savePlatformFareConfig(config)
    }

    suspend fun resetPlatformFareToDefaults(platform: RidePlatform) {
        preferencesDataSource.resetPlatformFareToDefaults(platform)
    }

    suspend fun saveAdditionalFilters(filters: AdditionalRideFilters) {
        preferencesDataSource.saveAdditionalFilters(filters)
    }

    suspend fun saveNotificationPreferences(notificationPreferences: NotificationPreferences) {
        preferencesDataSource.saveNotificationPreferences(notificationPreferences)
    }

    suspend fun saveDriverProfile(profile: DriverProfileSettings) {
        preferencesDataSource.saveDriverProfile(profile)
    }

    suspend fun saveThemeMode(themeMode: AppThemeMode) {
        preferencesDataSource.saveThemeMode(themeMode)
    }

    suspend fun recordRideEvent(event: RideEventRecord): Long {
        return rideEventDao.insertRideEvent(RideEventEntity.fromDomainModel(event))
    }

    suspend fun updateDriverDecision(
        eventId: Long,
        outcome: AcceptanceOutcome,
        driverAction: String,
        statusDetail: String
    ) {
        rideEventDao.updateAcceptanceOutcome(
            id = eventId,
            outcome = outcome.name,
            driverAction = driverAction,
            statusDetail = statusDetail
        )
    }

    suspend fun deleteRideEvent(eventId: Long) {
        rideEventDao.deleteEventById(eventId)
    }

    suspend fun clearDemoEventsOnly() {
        rideEventDao.clearDemoEventsOnly()
    }

    suspend fun clearAllRideHistory() {
        rideEventDao.clearAllEvents()
    }

    suspend fun startMonitoringSession(startedAtMillis: Long): Long {
        return monitoringSessionDao.insertSession(
            MonitoringSessionEntity(
                startedAtMillis = startedAtMillis,
                endedAtMillis = null,
                stopReason = null
            )
        )
    }

    suspend fun endMonitoringSession(sessionId: Long, endedAtMillis: Long, stopReason: String) {
        monitoringSessionDao.closeSession(sessionId, endedAtMillis, stopReason)
    }
}
