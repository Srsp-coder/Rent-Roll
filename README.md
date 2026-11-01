# RentRoll — Modern Property and Rent Management Web Application

> **A complete, production-grade Spring Boot 3 web application crafted for Indian landlords and tenants.**  
> Built with simple, readable Java code, clean layered architecture, and a modern financial dashboard design.

---

## 1. What RentRoll Does

In India, small landlords manage properties, tenant records, rent payments, and repair requests using physical notebooks, Excel sheets, and WhatsApp chats. Receipts get lost, due dates are forgotten, and late fees cause friction.

**RentRoll** unifies rental property operations into a single platform:
- **Property Management:** Catalog residential flats, independent houses/villas, and commercial spaces with city, PIN code, and monthly asking rent.
- **Lease Agreements:** Bind tenants to properties with defined start/end dates, rent due day (1–28), and security deposit tracking. Prevents overlapping active leases.
- **Automated Rent Schedules:** When a lease is created, monthly rent ledger records are automatically generated for each month in the lease term.
- **5% Late Fee Engine:** If an overdue rent payment is settled, a 5% late fee is automatically calculated and added to the base rent using `BigDecimal`.
- **Maintenance Ticketing:** Tenants can raise repair requests (Plumbing, Electrical, Appliances, etc.) for properties they actively lease. Landlords update progress and log resolution notes.
- **Role-Based Financial Dashboards:** Real-time statistics, monthly rent collection trends (Chart.js), upcoming dues, and 60-day lease expiration alerts.
- **CSV Ledger Export:** Instant download of complete rent collection ledgers.

---

## 2. User Roles & Capabilities

| Capability | Administrator (`ADMIN`) | Landlord (`LANDLORD`) | Tenant (`TENANT`) |
| :--- | :---: | :---: | :---: |
| Access Login & Profile | Yes | Yes | Yes |
| Role-Specific Dashboard | Yes (System-wide) | Yes (Portfolio KPIs) | Yes (My Tenancy) |
| Manage User Accounts | **Yes** | No | No |
| Add / Edit Properties | Yes (All properties) | **Yes (Own only)** | No |
| Create Lease Agreements | Yes | **Yes (Own properties)** | No |
| View Rent Schedule & Ledger | Yes | Yes | Yes (Own payments) |
| Record Payment Settlement | Yes | **Yes (Applies 5% fee)** | **Forbidden** |
| Export Payments CSV | Yes | Yes | Yes |
| Raise Maintenance Ticket | Yes | Yes | **Yes (Leased unit)** |
| Update Ticket Resolution | Yes | **Yes** | No |
| View Financial Reports | Yes | **Yes (Portfolio)** | No |

---

## 3. Technology Stack

- **Backend:** Java 17, Spring Boot 3.3.4
- **Security:** Spring Security 6 (Session-based authentication, BCrypt password hashing, CSRF protection, method & URL authorization)
- **Persistence:** Spring Data JPA, Hibernate 6
- **Databases:**
  - **H2 Database (Default):** Zero-setup in-memory database with PostgreSQL compatibility mode for instant local development and testing.
  - **PostgreSQL (Production profile):** Configurable via environment variables or `--spring.profiles.active=postgres`.
- **Validation:** Jakarta Bean Validation (`@NotBlank`, `@NotNull`, `@DecimalMin`, `@Pattern`)
- **Frontend / UI:** Thymeleaf 3, Bootstrap 5.3.3, Bootstrap Icons 1.11.3, Chart.js 4.4, Custom CSS
- **Testing:** JUnit 5, Mockito, Spring Boot Test (`MockMvc`)
- **Build Tool:** Apache Maven 3.9+

---

## 4. Prerequisites

1. **Java Development Kit (JDK):** Version 17 or higher (`java -version`)
2. **Apache Maven:** Version 3.8 or higher (`mvn -version`)
3. **Web Browser:** Any modern browser (Chrome, Edge, Firefox, Safari)

---

## 5. Development Login Accounts (Local Dev Only)

RentRoll automatically seeds realistic sample Indian real estate data upon first boot:

| Role | Name | Email | Password | Pre-seeded Data |
| :--- | :--- | :--- | :--- | :--- |
| **Administrator** | Anita Sharma | `admin@rentroll.in` | `admin123` | System oversight & user management |
| **Landlord** | Rajesh Verma | `rajesh.landlord@rentroll.in` | `landlord123` | 3 properties in Bengaluru & Mumbai, 2 leases |
| **Landlord** | Priya Kulkarni | `priya.landlord@rentroll.in` | `landlord123` | 2 properties in Pune & Hyderabad, 1 lease |
| **Tenant** | Amit Kumar | `amit.tenant@rentroll.in` | `tenant123` | Leases Prestige Shantiniketan Flat 702 |
| **Tenant** | Sneha Nair | `sneha.tenant@rentroll.in` | `tenant123` | Leases Hiranandani Powai 301 (has 1 overdue payment) |
| **Tenant** | Vikram Malhotra | `vikram.tenant@rentroll.in` | `tenant123` | Leases Amanora Park Town Flat 1104 in Pune |

> *Note: On the login page, 1-click **"Fill"** buttons are provided so you can instantly log in as any role without typing.*

---

## 6. How to Run the Application Locally

### Option A: Standard Run on Default Port (8080)

```bash
# In the project root directory:
mvn spring-boot:run
```

Once started, open your browser and navigate to:
```
http://localhost:8080
```

### Option B: Running on a Custom Port (e.g., 8085 if 8080 is busy)

```bash
# Using Maven:
mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"

# Or using the packaged executable JAR:
mvn package -DskipTests
java -jar target/rentroll-1.0.0.jar --server.port=8085
```

### Option C: Running with PostgreSQL

1. Ensure PostgreSQL is running and create a database named `rentrolldb`:
   ```sql
   CREATE DATABASE rentrolldb;
   ```
2. Set environment variables (or provide them via command line):
   ```bash
   # Windows PowerShell:
   $env:DB_URL="jdbc:postgresql://localhost:5432/rentrolldb"
   $env:DB_USERNAME="postgres"
   $env:DB_PASSWORD="your_postgres_password"

   mvn spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

### Accessing the H2 Database Console

When running with H2 in local development mode, you can inspect the database tables directly:
- **URL:** `http://localhost:8080/h2-console` (or `http://localhost:8085/h2-console`)
- **JDBC URL:** `jdbc:h2:mem:rentrolldb`
- **User Name:** `sa`
- **Password:** *(leave blank)*

---

## 7. How to Run the Tests

Execute the automated test suite with Maven:

```bash
mvn test
```

### Test Suite Summary (22 Automated Tests)
- `PropertyServiceTest`: Property creation, landlord ownership verification, deletion safety checks.
- `LeaseServiceTest`: Start/End date validations, due day boundaries (1–28), overlapping active lease detection, 6-month rent schedule generation, duplicate payment prevention, safe termination logic.
- `RentPaymentServiceTest`: 5% late fee calculation with `BigDecimal`, late fee assessment on overdue payments, duplicate settlement prevention, tenant payment authorization blocks.
- `MaintenanceRequestServiceTest`: Active lease checks for tenants submitting tickets, landlord status updating and resolution notes, tenant update restrictions.
- `RentRollIntegrationTest`: Unauthenticated redirects, public login access, protected URL enforcement, Admin vs. Landlord access control on `/users`.

To run the end-to-end workflow verification script:
```bash
python verify_workflows.py
```

---

## 8. Database Entity Relationships

RentRoll uses 5 JPA entities with clean relational associations:

```
+------------------+             +----------------------+
|       User       | 1         * |       Property       |
|------------------|<------------|----------------------|
| id (PK)          |             | id (PK)              |
| full_name        |             | property_name        |
| email (Unique)   |             | address, city, state |
| password (BCrypt)|             | pin_code             |
| role             |             | property_type        |
| active           |             | monthly_rent         |
+------------------+             | landlord_id (FK)     |
      ^                          +----------------------+
      |                                    ^
      | tenant_id                          | property_id
      |                                    |
+-------------------------------------------------------+
|                         Lease                         |
|-------------------------------------------------------|
| id (PK)                                               |
| property_id (FK -> Property)                          |
| tenant_id   (FK -> User)                              |
| start_date, end_date                                  |
| monthly_rent                                          |
| due_day_of_month (1-28)                               |
| security_deposit                                      |
| status (ACTIVE, EXPIRED, TERMINATED)                  |
+-------------------------------------------------------+
      ^
      | lease_id
      |
+-------------------------------------------------------+
|                      RentPayment                      |
|-------------------------------------------------------|
| id (PK)                                               |
| lease_id    (FK -> Lease)                             |
| property_id (FK -> Property)                          |
| tenant_id   (FK -> User)                              |
| rental_period ("October 2026")                        |
| due_date                                              |
| base_rent                                             |
| late_fee (5% if overdue upon payment)                 |
| total_amount (base_rent + late_fee)                   |
| status (UPCOMING, PENDING, LATE, PAID, CANCELLED)     |
| payment_date, payment_method, notes                   |
+-------------------------------------------------------+

+-------------------------------------------------------+
|                  MaintenanceRequest                   |
|-------------------------------------------------------|
| id (PK)                                               |
| property_id (FK -> Property)                          |
| tenant_id   (FK -> User)                              |
| title, description                                    |
| category (PLUMBING, ELECTRICAL, APPLIANCE, etc.)      |
| priority (LOW, MEDIUM, HIGH)                          |
| status (OPEN, IN_PROGRESS, RESOLVED)                  |
| created_at, resolved_at, resolution_notes             |
+-------------------------------------------------------+
```

---

## 9. Project Folder Structure

```
RentRoll/
├── pom.xml                               # Maven project definition and dependencies
├── src/
│   ├── main/
│   │   ├── java/com/rentroll/
│   │   │   ├── RentRollApplication.java  # Application entry point
│   │   │   ├── config/                   # SecurityConfig, DataInitializer
│   │   │   ├── controller/               # Spring MVC Controllers (Auth, Dashboard, Property, Lease, Payment, Maintenance, User, Report)
│   │   │   ├── dto/                      # Form DTOs (PropertyFormDto, LeaseFormDto, etc.) and Dashboard KPIs
│   │   │   ├── entity/                   # JPA Entities (User, Property, Lease, RentPayment, MaintenanceRequest) and Enums
│   │   │   ├── exception/                # BusinessRuleException, ResourceNotFoundException, GlobalExceptionHandler
│   │   │   ├── repository/               # Spring Data JPA interfaces
│   │   │   ├── security/                 # CustomUserDetails, CustomUserDetailsService, SecurityUtils
│   │   │   └── service/                  # Business logic (PropertyService, LeaseService, RentPaymentService, etc.)
│   │   └── resources/
│   │       ├── application.properties    # Default H2 in-memory configuration
│   │       ├── application-postgres.properties # Production PostgreSQL profile
│   │       ├── static/
│   │       │   ├── css/style.css         # Modern Navy & Teal SaaS dashboard stylesheet
│   │       │   └── js/app.js             # Mobile drawer toggle, password visibility, quick fill
│   │       └── templates/                # Server-rendered Thymeleaf HTML pages
│   │           ├── login.html
│   │           ├── dashboard/            # admin.html, landlord.html, tenant.html
│   │           ├── properties/           # list.html, details.html, form.html
│   │           ├── leases/               # list.html, details.html, form.html
│   │           ├── payments/             # list.html, details.html
│   │           ├── maintenance/          # list.html, details.html, form.html
│   │           ├── users/                # list.html, details.html, form.html
│   │           ├── reports/              # index.html
│   │           ├── error/                # 403.html, 404.html, 500.html
│   │           └── fragments/            # sidebar.html, navbar.html, alerts.html, footer.html
│   └── test/java/com/rentroll/           # Unit and Integration test suite
│       ├── RentRollIntegrationTest.java
│       └── service/                      # Service layer unit tests
├── LEARNING_GUIDE.md                     # Beginner guide explaining Spring Boot concepts
└── verify_workflows.py                   # Automated end-to-end Python test script
```

---

## 10. How a Request Flows Through RentRoll

Here is an example of what happens when a landlord clicks **"Confirm & Mark as Paid"** for an overdue rent payment:

1. **HTTP Request:** The browser sends a `POST /payments/22/pay` with the payment date, method, and CSRF token.
2. **Spring Security Filter:** Spring Security validates the session cookie and verifies the user is authenticated with role `LANDLORD` or `ADMIN`.
3. **Controller (`RentPaymentController`):**
   - Intercepts the request.
   - Calls `securityUtils.getRequiredCurrentUser()` to verify role ownership.
   - Forwards call to `rentPaymentService.markPaymentAsPaid(...)`.
4. **Service (`RentPaymentService`):**
   - Retrieves the `RentPayment` entity from `RentPaymentRepository`.
   - Verifies the payment is not already `PAID`.
   - Checks if payment was overdue (`dueDate.isBefore(effectivePaymentDate)`).
   - If overdue and late fee is not yet applied, calculates `5%` of `baseRent` using `BigDecimal.setScale(2, RoundingMode.HALF_UP)`.
   - Sets `lateFee`, updates `totalAmount = baseRent + lateFee`, updates status to `PAID`, sets `paymentDate`, and saves.
5. **Repository (`RentPaymentRepository`):**
   - Executes Hibernate SQL `UPDATE rent_payments SET status = 'PAID', late_fee = 2900.00, total_amount = 60900.00, ... WHERE id = 22`.
6. **Database:** Persists the changes within the transaction.
7. **View Redirection:** The controller adds a flash success message and redirects to `/payments/22`, rendering the updated receipt view via Thymeleaf.

---

## 11. Troubleshooting

- **Port 8080 already in use:**
  If port 8080 is occupied by another process, start RentRoll with a different port:
  ```bash
  mvn spring-boot:run -Dspring-boot.run.arguments="--server.port=8085"
  ```
- **H2 Database resets after restart:**
  By default, H2 runs in-memory (`jdbc:h2:mem:rentrolldb`). Every time the application starts, `DataInitializer` seeds fresh development data. If you want persistent file storage, update `spring.datasource.url=jdbc:h2:file:./data/rentrolldb` in `application.properties`.
- **Thymeleaf Template Parsing Issues:**
  Thymeleaf cache is set to `false` for development (`spring.thymeleaf.cache=false`), so HTML modifications take effect immediately without rebuilding the application.
