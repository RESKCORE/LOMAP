package com.example.domain.model

enum class RidePlatform(
    val id: String,
    val displayName: String,
    val appTitle: String,
    val packageName: String,
    val defaultMinFareInr: Double,
    val defaultMaxFareInr: Double,
    val vehicleTypes: String
) {
    UBER(
        id = "UBER",
        displayName = "Uber",
        appTitle = "Uber Driver",
        packageName = "com.ubercab.driver",
        defaultMinFareInr = 100.0,
        defaultMaxFareInr = 500.0,
        vehicleTypes = "Uber Go • Premier • XL • Moto"
    ),
    RAPIDO(
        id = "RAPIDO",
        displayName = "Rapido",
        appTitle = "Rapido Captain",
        packageName = "com.rapido.rider",
        defaultMinFareInr = 50.0,
        defaultMaxFareInr = 300.0,
        vehicleTypes = "Bike Taxi • Auto • Cab Economy"
    ),
    OLA(
        id = "OLA",
        displayName = "Ola",
        appTitle = "Ola Driver",
        packageName = "com.olacabs.oladriver",
        defaultMinFareInr = 100.0,
        defaultMaxFareInr = 600.0,
        vehicleTypes = "Mini • Prime Sedan • Auto • Bike"
    );

    companion object {
        fun fromPackageName(packageName: String): RidePlatform? =
            entries.firstOrNull { it.packageName.equals(packageName.trim(), ignoreCase = true) }

        fun fromId(id: String): RidePlatform =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: UBER
    }
}

enum class MonitoringServiceState(val label: String, val description: String) {
    INACTIVE("Inactive", "Monitoring service is currently stopped."),
    STARTING("Starting", "Initializing foreground service & listener handshake..."),
    ACTIVE("Active", "Native foreground service is actively monitoring selected platforms."),
    PAUSED("Paused", "Monitoring is temporarily paused by driver."),
    PERMISSION_REQUIRED("Permission Required", "Notification listener or foreground notification permission is missing."),
    SERVICE_ERROR("Service Error", "The monitoring service encountered an unexpected lifecycle error.")
}

enum class PlatformIntegrationStatus(val label: String, val description: String) {
    NOT_CONFIGURED("Not configured", "Platform monitoring is disabled in RidePilot."),
    PERMISSION_REQUIRED("Permission required", "Notification Listener access is required to observe incoming requests."),
    NOTIFICATION_ACCESS_UNAVAILABLE("Notification access unavailable", "Android OS notification listener binding is inactive."),
    READY_FOR_PERMITTED_MONITORING("Ready for permitted monitoring", "Permitted notification monitoring is active; driver confirms rides manually."),
    INTEGRATION_UNAVAILABLE("Integration unavailable", "Official automatic acceptance API is not authorized by this platform."),
    CONNECTED_AUTHORIZED("Connected through an authorized integration", "Authorized partner dispatch webhook/SDK is verified and active.")
}

enum class FareEvaluationResult(val label: String, val isEligible: Boolean) {
    ELIGIBLE("Eligible", true),
    BELOW_MINIMUM("Below Minimum Fare", false),
    ABOVE_MAXIMUM("Above Maximum Fare", false),
    UNKNOWN_FARE("Unknown Fare", false),
    PLATFORM_DISABLED("Platform Disabled", false),
    INVALID_CONFIGURATION("Invalid Configuration", false)
}

enum class RideInfoAvailability {
    AVAILABLE,
    UNAVAILABLE,
    INVALID
}

data class ParsedField<T>(
    val value: T? = null,
    val availability: RideInfoAvailability = RideInfoAvailability.UNAVAILABLE
) {
    val isAvailable: Boolean
        get() = availability == RideInfoAvailability.AVAILABLE && value != null
}

enum class AdditionalFilterResult(val label: String, val passed: Boolean) {
    PASSED("All Enabled Filters Passed", true),
    DISTANCE_BELOW_MIN("Below Min Distance", false),
    DISTANCE_ABOVE_MAX("Above Max Distance", false),
    DISTANCE_UNAVAILABLE("Trip Distance Unavailable", false),
    DISTANCE_INVALID("Trip Distance Invalid", false),
    RATE_BELOW_MIN("Below Min ₹/km", false),
    RATE_UNAVAILABLE("₹/km Rate Unavailable", false),
    PICKUP_NOT_PREFERRED("Pickup Outside Preferred Area", false),
    PICKUP_UNAVAILABLE("Pickup Location Unavailable", false),
    DESTINATION_NOT_PREFERRED("Drop Outside Preferred Area", false),
    DESTINATION_UNAVAILABLE("Drop Location Unavailable", false)
}

enum class AcceptanceOutcome(val label: String, val badgeText: String) {
    DRIVER_CONFIRMED("Driver Confirmed", "CONFIRMED"),
    ACCEPTED_BY_AUTHORIZED_INTEGRATION("Accepted by Authorized Integration", "AUTO-ACCEPTED"),
    REJECTED_BY_DRIVER("Rejected by Driver", "REJECTED"),
    FILTERED_OUT("Filtered Out", "FILTERED"),
    UNKNOWN_FARE("Unknown Fare — Manual Check", "UNKNOWN FARE"),
    ACCEPTANCE_UNAVAILABLE("Acceptance Integration Unavailable", "MANUAL ONLY"),
    EXPIRED("Request Expired", "EXPIRED"),
    FAILED("Acceptance Failed", "FAILED")
}

data class PlatformFareConfig(
    val platform: RidePlatform,
    val platformEnabled: Boolean = true,
    val fareFilterEnabled: Boolean = true,
    val minimumFareInr: Double = platform.defaultMinFareInr,
    val maximumFareInr: Double = platform.defaultMaxFareInr,
    val authorizedIntegrationSimulated: Boolean = true
) {
    fun validate(): FareConfigValidationResult {
        if (minimumFareInr.isNaN() || maximumFareInr.isNaN()) {
            return FareConfigValidationResult(false, "Fare values must be valid numbers.")
        }
        if (minimumFareInr < 0.0) {
            return FareConfigValidationResult(false, "Minimum fare cannot be negative (₹0 or above required).")
        }
        if (maximumFareInr <= minimumFareInr) {
            return FareConfigValidationResult(false, "Maximum fare (₹${maximumFareInr.toInt()}) must be strictly greater than minimum fare (₹${minimumFareInr.toInt()}).")
        }
        return FareConfigValidationResult(true, null)
    }
}

data class FareConfigValidationResult(
    val isValid: Boolean,
    val errorMessage: String? = null
)

data class AdditionalRideFilters(
    val minDistanceEnabled: Boolean = false,
    val minDistanceKm: Double = 2.0,
    val maxDistanceEnabled: Boolean = false,
    val maxDistanceKm: Double = 25.0,
    val minFarePerKmEnabled: Boolean = false,
    val minFarePerKmInr: Double = 15.0,
    val preferredPickupEnabled: Boolean = false,
    val preferredPickupAreas: List<String> = listOf("Indiranagar", "Koramangala", "MG Road", "HSR Layout"),
    val preferredDestinationEnabled: Boolean = false,
    val preferredDestinationAreas: List<String> = listOf("Whitefield", "Electronic City", "Airport", "Bellandur")
)

enum class AppThemeMode(val label: String) {
    DARK("Dark Cockpit"),
    LIGHT("Daylight High-Contrast"),
    SYSTEM("Follow System")
}

data class NotificationPreferences(
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val notifyIncomingDetected: Boolean = true,
    val notifyEligibleFare: Boolean = true,
    val notifyFilteredFare: Boolean = false,
    val notifyUnknownFare: Boolean = true,
    val notifyServiceLifecycle: Boolean = true
)

data class DriverProfileSettings(
    val driverName: String = "Rajesh Kumar",
    val phoneNumber: String = "+91 98450 •••••",
    val city: String = "Bengaluru, KA",
    val currencySymbol: String = "₹",
    val currencyCode: String = "INR"
)

data class NormalizedNotificationEvent(
    val notificationKey: String,
    val packageName: String,
    val title: String?,
    val text: String?,
    val bigText: String?,
    val subText: String?,
    val postedAtMillis: Long,
    val isDemo: Boolean = false,
    val demoScenarioName: String? = null
)

data class ParsedRideRequest(
    val eventHash: String,
    val platform: RidePlatform,
    val rawTitle: String,
    val rawBody: String,
    val fareInr: ParsedField<Double>,
    val tripDistanceKm: ParsedField<Double>,
    val pickupDistanceKm: ParsedField<Double>,
    val estimatedFarePerKmInr: ParsedField<Double>,
    val pickupLocation: ParsedField<String>,
    val destinationLocation: ParsedField<String>,
    val timestampMillis: Long,
    val isDemo: Boolean,
    val demoScenarioName: String? = null
)

data class RideEventRecord(
    val id: Long = 0L,
    val eventHash: String,
    val platform: RidePlatform,
    val fareInr: Double?,
    val fareAvailability: RideInfoAvailability,
    val tripDistanceKm: Double?,
    val distanceAvailability: RideInfoAvailability,
    val estimatedFarePerKmInr: Double?,
    val pickupLocation: String?,
    val pickupAvailability: RideInfoAvailability,
    val destinationLocation: String?,
    val destinationAvailability: RideInfoAvailability,
    val timestampMillis: Long,
    val fareEvaluationResult: FareEvaluationResult,
    val additionalFilterResult: AdditionalFilterResult,
    val isOverallEligible: Boolean,
    val driverAction: String,
    val acceptanceOutcome: AcceptanceOutcome,
    val statusDetail: String,
    val rawNotificationSummary: String,
    val isDemo: Boolean,
    val demoScenarioLabel: String? = null
)

data class MonitoringSessionRecord(
    val id: Long = 0L,
    val startedAtMillis: Long,
    val endedAtMillis: Long?,
    val stopReason: String?,
    val requestsDetectedCount: Int = 0,
    val eligibleCount: Int = 0
)
