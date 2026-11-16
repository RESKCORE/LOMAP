package com.example.di

import android.content.Context
import com.example.data.database.RidePilotDatabase
import com.example.data.datastore.RidePilotPreferencesDataSource
import com.example.data.repository.CloudSyncRepository
import com.example.data.repository.RidePilotRepository
import com.example.domain.evaluator.AdditionalFilterEvaluator
import com.example.domain.evaluator.FareEvaluator
import com.example.integrations.acceptance.DefaultRideAcceptanceManager
import com.example.integrations.acceptance.RideAcceptanceManager
import com.example.monitoring.notification.DriverAlertNotifier
import com.example.monitoring.processor.RideEventProcessor

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: RidePilotDatabase by lazy {
        RidePilotDatabase.getInstance(appContext)
    }

    val preferencesDataSource: RidePilotPreferencesDataSource by lazy {
        RidePilotPreferencesDataSource(appContext)
    }

    val repository: RidePilotRepository by lazy {
        RidePilotRepository(
            rideEventDao = database.rideEventDao(),
            monitoringSessionDao = database.monitoringSessionDao(),
            preferencesDataSource = preferencesDataSource
        )
    }

    // ponytail: Firebase-only; null when google-services.json is absent (fresh clones) — cloud sync is dormant without sign-in anyway
    val cloudSyncRepository: CloudSyncRepository? by lazy {
        runCatching { CloudSyncRepository(appContext) }.getOrNull()
    }

    val fareEvaluator: FareEvaluator by lazy {
        FareEvaluator()
    }

    val additionalFilterEvaluator: AdditionalFilterEvaluator by lazy {
        AdditionalFilterEvaluator()
    }

    val acceptanceManager: RideAcceptanceManager by lazy {
        DefaultRideAcceptanceManager()
    }

    val driverAlertNotifier: DriverAlertNotifier by lazy {
        DriverAlertNotifier(appContext)
    }

    val rideEventProcessor: RideEventProcessor by lazy {
        RideEventProcessor(
            repository = repository,
            fareEvaluator = fareEvaluator,
            additionalFilterEvaluator = additionalFilterEvaluator,
            acceptanceManager = acceptanceManager,
            driverAlertNotifier = driverAlertNotifier,
            cloudSyncRepository = cloudSyncRepository
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: AppContainer? = null

        fun getInstance(context: Context): AppContainer {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppContainer(context).also { INSTANCE = it }
            }
        }
    }
}
