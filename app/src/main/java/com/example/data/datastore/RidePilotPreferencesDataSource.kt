package com.example.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.domain.model.AdditionalRideFilters
import com.example.domain.model.AppThemeMode
import com.example.domain.model.DriverProfileSettings
import com.example.domain.model.NotificationPreferences
import com.example.domain.model.PlatformFareConfig
import com.example.domain.model.RidePlatform
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.ridePilotDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ridepilot_preferences"
)

data class PersistedAppSettings(
    val onboardingCompleted: Boolean = false,
    val uberConfig: PlatformFareConfig = PlatformFareConfig(RidePlatform.UBER),
    val rapidoConfig: PlatformFareConfig = PlatformFareConfig(RidePlatform.RAPIDO),
    val olaConfig: PlatformFareConfig = PlatformFareConfig(RidePlatform.OLA),
    val additionalFilters: AdditionalRideFilters = AdditionalRideFilters(),
    val notificationPreferences: NotificationPreferences = NotificationPreferences(),
    val driverProfile: DriverProfileSettings = DriverProfileSettings(),
    val themeMode: AppThemeMode = AppThemeMode.DARK
) {
    fun configFor(platform: RidePlatform): PlatformFareConfig = when (platform) {
        RidePlatform.UBER -> uberConfig
        RidePlatform.RAPIDO -> rapidoConfig
        RidePlatform.OLA -> olaConfig
    }
}

class RidePilotPreferencesDataSource(
    private val dataStore: DataStore<Preferences>
) {
    constructor(context: Context) : this(context.applicationContext.ridePilotDataStore)

    private object Keys {
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val THEME_MODE = stringPreferencesKey("theme_mode")

        // Uber
        val UBER_ENABLED = booleanPreferencesKey("uber_enabled")
        val UBER_FILTER_ENABLED = booleanPreferencesKey("uber_filter_enabled")
        val UBER_MIN_FARE = doublePreferencesKey("uber_min_fare")
        val UBER_MAX_FARE = doublePreferencesKey("uber_max_fare")
        val UBER_AUTH_SIM = booleanPreferencesKey("uber_auth_sim")

        // Rapido
        val RAPIDO_ENABLED = booleanPreferencesKey("rapido_enabled")
        val RAPIDO_FILTER_ENABLED = booleanPreferencesKey("rapido_filter_enabled")
        val RAPIDO_MIN_FARE = doublePreferencesKey("rapido_min_fare")
        val RAPIDO_MAX_FARE = doublePreferencesKey("rapido_max_fare")
        val RAPIDO_AUTH_SIM = booleanPreferencesKey("rapido_auth_sim")

        // Ola
        val OLA_ENABLED = booleanPreferencesKey("ola_enabled")
        val OLA_FILTER_ENABLED = booleanPreferencesKey("ola_filter_enabled")
        val OLA_MIN_FARE = doublePreferencesKey("ola_min_fare")
        val OLA_MAX_FARE = doublePreferencesKey("ola_max_fare")
        val OLA_AUTH_SIM = booleanPreferencesKey("ola_auth_sim")

        // Additional Ride Filters
        val MIN_DIST_ENABLED = booleanPreferencesKey("min_dist_enabled")
        val MIN_DIST_KM = doublePreferencesKey("min_dist_km")
        val MAX_DIST_ENABLED = booleanPreferencesKey("max_dist_enabled")
        val MAX_DIST_KM = doublePreferencesKey("max_dist_km")
        val MIN_RATE_ENABLED = booleanPreferencesKey("min_rate_enabled")
        val MIN_RATE_INR = doublePreferencesKey("min_rate_inr")
        val PREF_PICKUP_ENABLED = booleanPreferencesKey("pref_pickup_enabled")
        val PREF_PICKUP_LIST = stringPreferencesKey("pref_pickup_list")
        val PREF_DEST_ENABLED = booleanPreferencesKey("pref_dest_enabled")
        val PREF_DEST_LIST = stringPreferencesKey("pref_dest_list")

        // Notification Preferences
        val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
        val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
        val NOTIFY_INCOMING = booleanPreferencesKey("notify_incoming")
        val NOTIFY_ELIGIBLE = booleanPreferencesKey("notify_eligible")
        val NOTIFY_FILTERED = booleanPreferencesKey("notify_filtered")
        val NOTIFY_UNKNOWN = booleanPreferencesKey("notify_unknown")
        val NOTIFY_LIFECYCLE = booleanPreferencesKey("notify_lifecycle")

        // Profile
        val DRIVER_NAME = stringPreferencesKey("driver_name")
        val DRIVER_PHONE = stringPreferencesKey("driver_phone")
        val DRIVER_CITY = stringPreferencesKey("driver_city")
    }

    val settingsFlow: Flow<PersistedAppSettings> = dataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { prefs ->
            val uber = PlatformFareConfig(
                platform = RidePlatform.UBER,
                platformEnabled = prefs[Keys.UBER_ENABLED] ?: true,
                fareFilterEnabled = prefs[Keys.UBER_FILTER_ENABLED] ?: true,
                minimumFareInr = prefs[Keys.UBER_MIN_FARE] ?: RidePlatform.UBER.defaultMinFareInr,
                maximumFareInr = prefs[Keys.UBER_MAX_FARE] ?: RidePlatform.UBER.defaultMaxFareInr,
                authorizedIntegrationSimulated = prefs[Keys.UBER_AUTH_SIM] ?: true
            )
            val rapido = PlatformFareConfig(
                platform = RidePlatform.RAPIDO,
                platformEnabled = prefs[Keys.RAPIDO_ENABLED] ?: true,
                fareFilterEnabled = prefs[Keys.RAPIDO_FILTER_ENABLED] ?: true,
                minimumFareInr = prefs[Keys.RAPIDO_MIN_FARE] ?: RidePlatform.RAPIDO.defaultMinFareInr,
                maximumFareInr = prefs[Keys.RAPIDO_MAX_FARE] ?: RidePlatform.RAPIDO.defaultMaxFareInr,
                authorizedIntegrationSimulated = prefs[Keys.RAPIDO_AUTH_SIM] ?: true
            )
            val ola = PlatformFareConfig(
                platform = RidePlatform.OLA,
                platformEnabled = prefs[Keys.OLA_ENABLED] ?: true,
                fareFilterEnabled = prefs[Keys.OLA_FILTER_ENABLED] ?: true,
                minimumFareInr = prefs[Keys.OLA_MIN_FARE] ?: RidePlatform.OLA.defaultMinFareInr,
                maximumFareInr = prefs[Keys.OLA_MAX_FARE] ?: RidePlatform.OLA.defaultMaxFareInr,
                authorizedIntegrationSimulated = prefs[Keys.OLA_AUTH_SIM] ?: true
            )

            val pickupAreasRaw = prefs[Keys.PREF_PICKUP_LIST]
            val pickupAreas = if (pickupAreasRaw == null) {
                listOf("Indiranagar", "Koramangala", "MG Road", "HSR Layout")
            } else {
                pickupAreasRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            val destAreasRaw = prefs[Keys.PREF_DEST_LIST]
            val destAreas = if (destAreasRaw == null) {
                listOf("Whitefield", "Electronic City", "Airport", "Bellandur")
            } else {
                destAreasRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            }

            val additionalFilters = AdditionalRideFilters(
                minDistanceEnabled = prefs[Keys.MIN_DIST_ENABLED] ?: false,
                minDistanceKm = prefs[Keys.MIN_DIST_KM] ?: 2.0,
                maxDistanceEnabled = prefs[Keys.MAX_DIST_ENABLED] ?: false,
                maxDistanceKm = prefs[Keys.MAX_DIST_KM] ?: 25.0,
                minFarePerKmEnabled = prefs[Keys.MIN_RATE_ENABLED] ?: false,
                minFarePerKmInr = prefs[Keys.MIN_RATE_INR] ?: 15.0,
                preferredPickupEnabled = prefs[Keys.PREF_PICKUP_ENABLED] ?: false,
                preferredPickupAreas = pickupAreas,
                preferredDestinationEnabled = prefs[Keys.PREF_DEST_ENABLED] ?: false,
                preferredDestinationAreas = destAreas
            )

            val notificationPreferences = NotificationPreferences(
                soundEnabled = prefs[Keys.SOUND_ENABLED] ?: true,
                vibrationEnabled = prefs[Keys.VIBRATION_ENABLED] ?: true,
                notifyIncomingDetected = prefs[Keys.NOTIFY_INCOMING] ?: true,
                notifyEligibleFare = prefs[Keys.NOTIFY_ELIGIBLE] ?: true,
                notifyFilteredFare = prefs[Keys.NOTIFY_FILTERED] ?: false,
                notifyUnknownFare = prefs[Keys.NOTIFY_UNKNOWN] ?: true,
                notifyServiceLifecycle = prefs[Keys.NOTIFY_LIFECYCLE] ?: true
            )

            val profile = DriverProfileSettings(
                driverName = prefs[Keys.DRIVER_NAME] ?: "Rajesh Kumar",
                phoneNumber = prefs[Keys.DRIVER_PHONE] ?: "+91 98450 74210",
                city = prefs[Keys.DRIVER_CITY] ?: "Bengaluru, KA"
            )

            val themeMode = runCatching {
                AppThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: AppThemeMode.DARK.name)
            }.getOrDefault(AppThemeMode.DARK)

            PersistedAppSettings(
                onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: false,
                uberConfig = uber,
                rapidoConfig = rapido,
                olaConfig = ola,
                additionalFilters = additionalFilters,
                notificationPreferences = notificationPreferences,
                driverProfile = profile,
                themeMode = themeMode
            )
        }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        dataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    suspend fun savePlatformFareConfig(config: PlatformFareConfig) {
        val validation = config.validate()
        require(validation.isValid) { validation.errorMessage ?: "Invalid fare configuration" }

        dataStore.edit { prefs ->
            when (config.platform) {
                RidePlatform.UBER -> {
                    prefs[Keys.UBER_ENABLED] = config.platformEnabled
                    prefs[Keys.UBER_FILTER_ENABLED] = config.fareFilterEnabled
                    prefs[Keys.UBER_MIN_FARE] = config.minimumFareInr
                    prefs[Keys.UBER_MAX_FARE] = config.maximumFareInr
                    prefs[Keys.UBER_AUTH_SIM] = config.authorizedIntegrationSimulated
                }
                RidePlatform.RAPIDO -> {
                    prefs[Keys.RAPIDO_ENABLED] = config.platformEnabled
                    prefs[Keys.RAPIDO_FILTER_ENABLED] = config.fareFilterEnabled
                    prefs[Keys.RAPIDO_MIN_FARE] = config.minimumFareInr
                    prefs[Keys.RAPIDO_MAX_FARE] = config.maximumFareInr
                    prefs[Keys.RAPIDO_AUTH_SIM] = config.authorizedIntegrationSimulated
                }
                RidePlatform.OLA -> {
                    prefs[Keys.OLA_ENABLED] = config.platformEnabled
                    prefs[Keys.OLA_FILTER_ENABLED] = config.fareFilterEnabled
                    prefs[Keys.OLA_MIN_FARE] = config.minimumFareInr
                    prefs[Keys.OLA_MAX_FARE] = config.maximumFareInr
                    prefs[Keys.OLA_AUTH_SIM] = config.authorizedIntegrationSimulated
                }
            }
        }
    }

    suspend fun resetPlatformFareToDefaults(platform: RidePlatform) {
        val defaultConfig = PlatformFareConfig(
            platform = platform,
            platformEnabled = true,
            fareFilterEnabled = true,
            minimumFareInr = platform.defaultMinFareInr,
            maximumFareInr = platform.defaultMaxFareInr,
            authorizedIntegrationSimulated = false
        )
        savePlatformFareConfig(defaultConfig)
    }

    suspend fun saveAdditionalFilters(filters: AdditionalRideFilters) {
        dataStore.edit { prefs ->
            prefs[Keys.MIN_DIST_ENABLED] = filters.minDistanceEnabled
            prefs[Keys.MIN_DIST_KM] = filters.minDistanceKm.coerceAtLeast(0.5)
            prefs[Keys.MAX_DIST_ENABLED] = filters.maxDistanceEnabled
            prefs[Keys.MAX_DIST_KM] = filters.maxDistanceKm.coerceAtLeast(1.0)
            prefs[Keys.MIN_RATE_ENABLED] = filters.minFarePerKmEnabled
            prefs[Keys.MIN_RATE_INR] = filters.minFarePerKmInr.coerceAtLeast(1.0)
            prefs[Keys.PREF_PICKUP_ENABLED] = filters.preferredPickupEnabled
            prefs[Keys.PREF_PICKUP_LIST] = filters.preferredPickupAreas.joinToString(",")
            prefs[Keys.PREF_DEST_ENABLED] = filters.preferredDestinationEnabled
            prefs[Keys.PREF_DEST_LIST] = filters.preferredDestinationAreas.joinToString(",")
        }
    }

    suspend fun saveNotificationPreferences(notificationPrefs: NotificationPreferences) {
        dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = notificationPrefs.soundEnabled
            prefs[Keys.VIBRATION_ENABLED] = notificationPrefs.vibrationEnabled
            prefs[Keys.NOTIFY_INCOMING] = notificationPrefs.notifyIncomingDetected
            prefs[Keys.NOTIFY_ELIGIBLE] = notificationPrefs.notifyEligibleFare
            prefs[Keys.NOTIFY_FILTERED] = notificationPrefs.notifyFilteredFare
            prefs[Keys.NOTIFY_UNKNOWN] = notificationPrefs.notifyUnknownFare
            prefs[Keys.NOTIFY_LIFECYCLE] = notificationPrefs.notifyServiceLifecycle
        }
    }

    suspend fun saveDriverProfile(profile: DriverProfileSettings) {
        dataStore.edit { prefs ->
            prefs[Keys.DRIVER_NAME] = profile.driverName.trim().ifBlank { "Driver Partner" }
            prefs[Keys.DRIVER_PHONE] = profile.phoneNumber.trim()
            prefs[Keys.DRIVER_CITY] = profile.city.trim()
        }
    }

    suspend fun saveThemeMode(mode: AppThemeMode) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME_MODE] = mode.name
        }
    }
}
