# RidePilot — Android Integration & Security Architecture

## 1. Supported Ride-Hailing Packages
RidePilot centralizes supported driver package identifiers in `RidePlatform`:
- **Uber Driver**: `com.ubercab.driver`
- **Rapido Captain**: `com.rapido.rider`
- **Ola Driver**: `com.olacabs.oladriver`

Narrow `<queries>` entries are declared in `AndroidManifest.xml` to check whether partner APKs are installed without requesting broad `QUERY_ALL_PACKAGES` permission.

## 2. NotificationListenerService Privacy Boundary
`RideNotificationListener` inspects `StatusBarNotification.packageName` first and immediately discards any notification not matching one of the three supported driver packages. Personal messages, OTPs, banking alerts, and system notifications are never read or stored.

## 3. Authorized Acceptance Integration Contract
`PlatformAcceptanceAdapter` and `RideAcceptanceManager` define the clean boundary for partner ride acceptance:
- By default, eligible rides resolve to `AcceptanceOutcome.ACCEPTANCE_UNAVAILABLE` and present an actionable notification ("Confirm Ride" / "Reject") for manual driver confirmation.
- When an official partner dispatch webhook/SDK is authorized (or simulated via the per-platform Authorized Partner Sandbox toggle), the adapter resolves `AcceptanceOutcome.ACCEPTED_BY_AUTHORIZED_INTEGRATION`.
- Accessibility-based screen tapping (`AccessibilityService`) and undocumented API spoofing are strictly prohibited.
