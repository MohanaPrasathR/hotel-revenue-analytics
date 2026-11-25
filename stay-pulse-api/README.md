# 🏨 Stay Pulse — Spring Boot REST Microservice

A high-performance Spring Boot 3 REST API (Java 17+, built and tested on JDK 22 in CI) providing reservation management and financial revenue analytics.

---

## 🚀 Quick Start & Execution

```bash
# Build project
mvn clean package -DskipTests

# Run application JAR
java -jar target/stay-pulse-api-1.1.0.jar
```

- **API Base URL:** `http://localhost:8080/api`
- **Swagger Documentation:** `http://localhost:8080/swagger-ui/index.html`
- **H2 Web Console:** `http://localhost:8080/h2-console` (`jdbc:h2:mem:stay_pulse_db`)
- **Frontend Dashboard:** `http://localhost:4200` (from `../frontend/`)

---

## 📡 REST API Catalog

| Method | Path | Description | Status |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/bookings` | Create a booking | `201 Created` |
| `GET` | `/api/bookings` | Retrieve all bookings | `200 OK` |
| `GET` | `/api/bookings/{id}` | Find booking by ID | `200 OK / 404` |
| `PUT` | `/api/bookings/{id}` | Update booking | `200 OK / 400` |
| `DELETE` | `/api/bookings/{id}` | Delete booking | `204 No Content` |
| `GET` | `/api/bookings/hotel/{hotelName}` | Find bookings by hotel name | `200 OK` |
| `GET` | `/api/analytics/total-revenue` | Cumulative revenue | `200 OK` |
| `GET` | `/api/analytics/revenue-by-hotel` | Revenue grouped by hotel | `200 OK` |
| `GET` | `/api/analytics/revenue-by-month` | Monthly revenue trends | `200 OK` |
| `GET` | `/api/analytics/booking-count-by-status` | Status count breakdown | `200 OK` |
| `GET` | `/api/analytics/average-revenue` | Average revenue per booking | `200 OK` |
| `GET` | `/api/analytics/top-hotels?limit=5` | Top N ranked hotels | `200 OK` |

---

## 🗄️ Database Configuration

### H2 In-Memory (Default)
Seeded with 23 development sample reservations via `SampleDataLoader.java` on empty database startup (`count == 0`).

### Disabling Sample Data in Production
- Set `app.data.seed.enabled=false` in `application.properties`
- Or pass environment variable: `APP_SEED_ENABLED=false`
- Or run with active production profile: `--spring.profiles.active=prod`

### MySQL 8.0 (Production)
Configurable in `src/main/resources/application.properties`:
```properties
spring.datasource.url=jdbc:mysql://localhost:3306/stay_pulse_db?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=root
spring.datasource.password=your_password
```

---

## ❗ Error Response Format

All errors share one JSON shape. `validationErrors` is only present for validation failures and
`traceId` mirrors the `X-Correlation-Id` of the request when one was supplied.

```json
{
  "timestamp": "2026-11-12T10:15:30",
  "status": 400,
  "error": "Validation Failed",
  "errorCode": "CONSTRAINT_VIOLATION",
  "message": "One or more request parameters are invalid",
  "path": "/api/bookings/paged",
  "traceId": null,
  "validationErrors": { "size": "size cannot exceed 100" }
}
```

| `errorCode` | HTTP | Cause |
| :--- | :--- | :--- |
| `RESOURCE_NOT_FOUND` | 404 | Unknown booking ID |
| `INVALID_BOOKING_DATES` | 400 | Check-out is not after check-in |
| `VALIDATION_ERROR` | 400 | Invalid request body |
| `CONSTRAINT_VIOLATION` | 400 | Invalid query parameter |

---

## 🔐 Operational Notes

- Stack traces and exception class names are never included in error payloads.
- The container runs as a non-root user and ships a `HEALTHCHECK` on `/actuator/health/liveness`.
- The server drains in-flight requests for up to 20 seconds on shutdown.
- JSON and static responses larger than 1 KB are compressed.
