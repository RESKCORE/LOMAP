# RidePilot — Native Android Driver Companion

**RidePilot** is a native Android application built with **Kotlin** and **Jetpack Compose (Material 3)** for multi-platform ride-hailing drivers in India operating across **Uber Driver**, **Rapido Captain**, and **Ola Driver**.

## Core Capabilities
1. **Unified Light Theme (`Daylight Map & Electric Volt`)**:
   - Single enforced **light** Material 3 color scheme across the entire app (`RidePilotTheme`), with a near-white green-tinted canvas, white surfaces, and a darkened Electric Volt (`#4D7C0F`) signature accent for readable contrast on light backgrounds.
   - Light vector map & radar viewport with recolored street grid, arterial roads, and glassmorphic pills.
2. **Multi-Platform Fare Filtering Engine (`FareEvaluator`)**:
   - Independent minimum and maximum ride fare configurations in **INR (₹)** for Uber (`₹100–₹500`), Rapido (`₹50–₹300`), and Ola (`₹100–₹600`).
   - Inclusive boundary evaluation (`rideFare >= minimumFare && rideFare <= maximumFare`).
   - Unknown or missing fares are strictly marked `UNKNOWN_FARE` and never automatically treated as eligible.
3. **Secondary Ride Filters (`AdditionalFilterEvaluator`)**:
   - Independently switchable filters for Minimum Trip Distance (`km`), Maximum Trip Distance (`km`), Minimum Estimated Rate (`₹/km`), Preferred Pickup Locations, and Preferred Destination Areas.
   - Explicitly distinguishes between `AVAILABLE`, `UNAVAILABLE`, and `INVALID` notification telemetry without inventing missing fields.
4. **Native Android Background Monitoring**:
   - `RideMonitoringService`: User-visible Foreground Service (`FOREGROUND_SERVICE_TYPE_DATA_SYNC`) with real elapsed session timer and instant stop action.
   - `RideNotificationListener`: Permitted `NotificationListenerService` scoped strictly to `com.ubercab.driver`, `com.rapido.rider`, and `com.olacabs.oladriver`.
   - `RideEventProcessor`: Deduplicates notification updates, parses platform-specific strings, evaluates filters, persists records to Room, and dispatches actionable notifications.
5. **Authorized Acceptance Architecture (`RideAcceptanceManager`)**:
   - Modular platform adapters (`UberAcceptanceAdapter`, `RapidoAcceptanceAdapter`, `OlaAcceptanceAdapter`).
   - Zero unauthorized accessibility clicking or UI injection.
6. **Local Persistence & Analytics**:
   - **Room SQLite Database** (`RidePilotDatabase`) for ride history and monitoring sessions.
   - **Jetpack DataStore Preferences** (`RidePilotPreferencesDataSource`) for fare thresholds, filters, notification settings, profile, and theme mode.
7. **12-Scenario Demo & Diagnostics Lab**:
   - Clearly labeled `[DEMO]` simulation suite for testing all 12 edge cases and inspecting live processor diagnostics.

## Building & Testing
- Compile debug APK: `gradle assembleDebug`
- Run JVM Unit & Robolectric tests: `gradle :app:testDebugUnitTest`
