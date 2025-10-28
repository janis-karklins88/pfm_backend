# Personal Finance Manager — Backend (Spring Boot)

Backend API for the PFM app.  
Handles authentication, categories (system + user preferences), transactions, expenses, budgets, savings goals, and analytics.

---

## Tech Stack
- Java 23, Spring Boot 3
- Spring Data JPA (Hibernate)
- MySQL (dev/perf), H2 (tests)
- JWT authentication
- Maven
- Lombok, Jakarta Validation, Spring Security

---

## Project Structure (Backend)
```
backend/
├─ src/main/java/JK/pfm/
│  ├─ bootstrap/               # App bootstrap (seeders/initializers)
│  │  └─ UserCategoryPreferenceInitializer.java
│  ├─ config/                  # Security, CORS, JWT filters, misc config
│  ├─ controller/              # REST controllers
│  ├─ dto/                     # Request/response DTOs
│  ├─ dto/filters/             # Query/filter DTOs
│  ├─ exception/               # Global handlers & custom exceptions
│  ├─ init/                    # (Additional init logic if present)
│  ├─ model/                   # JPA entities
│  ├─ repository/              # Spring Data repositories
│  ├─ security/                # Security utilities & config
│  ├─ service/                 # Business logic
│  ├─ specifications/          # JPA Specifications
│  └─ util/                    # Helpers (e.g., SecurityUtil)
│
├─ src/test/java/JK/pfm/
│  ├─ Util/                    # Test utilities
│  ├─ controller/              # Controller tests
│  ├─ e2e/                     # End-to-end tests
│  ├─ repository/              # Repository tests
│  ├─ service/                 # Service tests
│  └─ specification/            # Specification tests
│
├─ src/main/resources/
│  ├─ application.properties
│  └─ application-perf.properties    # Perf profile
│
├─ src/test/resources/
│  ├─ application-test.properties
│  └─ application.yml                # (test-only overrides if used)
│
└─ pom.xml

The app reads application.properties by default, with an optional perf profile via application-perf.properties.



spring.datasource.url=jdbc:mysql://localhost:3306/personal_finance_manager
spring.datasource.username=root
spring.datasource.password=CHANGE_ME
spring.jpa.hibernate.ddl-auto=update
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.MySQL8Dialect
Perf profile (src/main/resources/application-perf.properties)


```
# MySQL perf database
spring.datasource.url=jdbc:mysql://localhost:3306/pfm_perf?useSSL=false&serverTimezone=UTC&rewriteBatchedStatements=true
spring.datasource.username=pfm_perf_user
spring.datasource.password=CHANGE_ME
spring.datasource.hikari.maximum-pool-size=20

# Recreate schema on startup (drops on shutdown)
spring.jpa.hibernate.ddl-auto=create-drop

# Optional: SQL visibility for perf runs
spring.jpa.show-sql=true



# Maven run
mvn spring-boot:run -Dspring-boot.run.profiles=perf

# JAR run
java -jar target/pfm-backend-*.jar --spring.profiles.active=perf

# Environment variable
set SPRING_PROFILES_ACTIVE=perf   # Windows
export SPRING_PROFILES_ACTIVE=perf # macOS/Linux

From the backend folder:


# 1) Build
mvn clean package

# 2) Run (choose a profile)

# Default (normal development)
mvn spring-boot:run

# Performance profile
mvn spring-boot:run -Dspring-boot.run.profiles=perf

# or run the JAR directly
java -jar target/personalFinanceManager-*.jar --spring.profiles.active=perf
Default port:
http://localhost:8080

NetBeans / IDE usage
Project Properties → Run → VM Options →


-Dspring.profiles.active=perf
(Leave empty to run with the default configuration.)

When running tests (mvn test), the application automatically uses the H2 in-memory database defined in
src/test/resources/application-test.properties.

Endpoints

POST /api/users/register

POST /api/users/login → returns { "token": "jwt..." }

Use the token in all protected endpoints:


Authorization: Bearer <token>
Register example


curl -X POST http://localhost:8080/api/users/register \
  -H "Content-Type: application/json" \
  -d '{ "username":"User","password":"StrongPass123" }'
Login example


curl -X POST http://localhost:8080/api/users/login \
  -H "Content-Type: application/json" \
  -d '{ "username":"User","password":"StrongPass123" }'
Response


{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}

The JWT secret and expiration time are currently defined directly inside
JWTUtil.java:


private static final String SECRET_KEY = "yourSuperSecretKey1234567890123456";
Algorithm: HMAC-SHA256

Expiration: 24 hours (hardcoded in generateToken())

For production, externalize the secret key in environment variables or config files.

Users & Auth – JWT-based registration and login

System Categories – Predefined, visible to all users

User Category Preferences – Activate/deactivate, list per user

Transactions – CRUD, filtering & sorting

Budgets – Create and track per category/period

Savings Goals – Create, update, total balance tracking

Dashboard – Summary endpoints for analytics

SystemCategoryInitializer runs on startup and ensures baseline categories exist (visible to every user), e.g.:

Food, Housing, Transportation, Household supplies, Rent, Eating out, Entertainment, Trips, Parties, Subscriptions, …

If you don’t want seeding on every boot, wrap the logic with a guard (e.g., check if the table is empty) or disable the CommandLineRunner bean.

UserCategoryPreferenceInitializer (JK.pfm.bootstrap)
Bootstraps user category preferences and base category visibility at startup.

## REST Endpoints Overview

| **Area**              | **Method** | **Path**                                     | **Description** |
|------------------------|------------|----------------------------------------------|-----------------|
| **Authentication**     | POST       | `/api/users/register`                        | Register a new user |
|                        | POST       | `/api/users/login`                           | Authenticate user and receive JWT token |
| **Categories**         | GET        | `/api/categories`                            | Get list of active categories for the current user |
|                        | GET        | `/api/categories/all`                        | Get all base (system) categories |
| **User Preferences**   | GET        | `/api/user-categories`                       | List user category preferences (active/inactive) |
|                        | POST       | `/api/user-categories/{categoryId}/activate` | Activate category for user |
|                        | POST       | `/api/user-categories/{categoryId}/deactivate` | Deactivate category for user |
| **Transactions**       | GET        | `/api/transactions`                          | Get user transactions (supports filters & sorting) |
|                        | POST       | `/api/transactions`                          | Create a new transaction |
|                        | PUT        | `/api/transactions/{id}`                     | Update existing transaction |
|                        | DELETE     | `/api/transactions/{id}`                     | Delete transaction |
| **Budgets**            | GET        | `/api/budgets`                               | Get list of budgets |
|                        | POST       | `/api/budgets`                               | Create new budget |
| **Savings Goals**      | GET        | `/api/savings`                               | Get list of user savings goals |
|                        | GET        | `/api/savings/total`                         | Get total savings balance for user |
|                        | POST       | `/api/savings`                               | Create new savings goal |
|                        | PUT        | `/api/savings/{id}`                          | Update existing savings goal |
|                        | DELETE     | `/api/savings/{id}`                          | Delete savings goal |
| **Dashboard / Stats**  | GET        | `/api/dashboard/summary`                     | Fetch overview data for dashboard charts |


## Error Handling and Response Format

All errors are centralized via `GlobalExceptionHandler` and returned as JSON:

```json
{
  "message": "Short human-readable error",
  "path": "/api/endpoint",
  "timestamp": "2025-10-27T08:30:00"
}

Status Codes and Sources
HTTP Status	When it happens	Source in code
400 Bad Request	Bean validation fails (@Valid); multiple field messages are joined with ;	handleValidation(MethodArgumentNotValidException)
409 Conflict	Manual conflicts (e.g., username taken) or optimistic locking	handleStatusExc(ResponseStatusException), handleOptimisticLock(ObjectOptimisticLockingFailureException)
500 Internal Server Error	Any unhandled exception	handleAll(Exception)

Always send JWT via Authorization: Bearer <token>

Backend enforces per-user resource access (IDs resolved from token)

CORS restricted to your frontend origin via pfm.cors.allowed-origins

For portfolio and educational use.
