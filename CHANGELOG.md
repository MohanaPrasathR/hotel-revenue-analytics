# Changelog

## Unreleased — Stay Pulse rename and hardening

### Renamed
- Project is now **Stay Pulse** (previously "Hotel Revenue Analytics").
- Java base package `com.mohana.hotelanalytics` -> `com.mohana.staypulse`; application class is `StayPulseApplication`.
- Maven artifact `stay-pulse-api`; Angular project `stay-pulse-frontend`; compose project `stay-pulse`.

### Fixed
- Frontend production build failed: `OperationalMetricsResponse` was not imported in the analytics page.
- Frontend Docker image copied from a dist folder that did not exist.
- `/api/bookings/paged` accepted arbitrary sort fields and unbounded page sizes.
- `/api/analytics/top-hotels` accepted unbounded `limit` values.
- Rate limiter window rollover was racy and tracked clients were never evicted.
- CSV export did not neutralise spreadsheet formulas in free-text fields.
- Blank hotel lookups no longer throw.
- Documentation links pointed at machine-specific `file:///` paths.
