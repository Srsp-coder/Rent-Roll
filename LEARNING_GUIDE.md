# RentRoll — Beginner's Learning Guide to Spring Boot 3

Welcome to your learning journey with Java and Spring Boot! This guide is written specifically for you as a college student to help you understand how every part of RentRoll works, why it was designed this way, and how to read the code with confidence.

---

## 1. Suggested Learning Order

To master Spring Boot without getting overwhelmed, study the concepts in this order:

```
1. Entities & Enums  ──>  2. Repositories  ──>  3. Services  ──>  4. Controllers & DTOs  ──>  5. Thymeleaf Templates  ──>  6. Spring Security
```

1. **Entities & Enums (`com.rentroll.entity`):** Understand how Java classes map to database tables.
2. **Repositories (`com.rentroll.repository`):** Learn how Spring Data JPA automatically writes SQL queries for you.
3. **Services (`com.rentroll.service`):** Discover where business rules, calculations, and validations live.
4. **Controllers & DTOs (`com.rentroll.controller`, `com.rentroll.dto`):** See how HTTP requests (GET, POST) are handled and how data from HTML forms is processed.
5. **Thymeleaf Templates (`src/main/resources/templates`):** Learn how HTML pages dynamically render data using attributes like `th:text`, `th:each`, and `th:if`.
6. **Spring Security (`com.rentroll.config.SecurityConfig`, `com.rentroll.security`):** Understand authentication (logging in) and authorization (role-based permissions).

---

## 2. Core Spring Boot Concepts Explained with RentRoll Code

### Concept 1: The Layered Architecture

In RentRoll, we strictly follow a 4-layer structure:
```
Browser Request ──> Controller ──> Service ──> Repository ──> Database
```

Why?
- **Controller:** Acts like a receptionist. It receives the HTTP request, asks the Service to do the heavy lifting, puts the result into a `Model`, and tells Spring which HTML template to display.
- **Service:** Contains the business logic. For example: *"Is this lease overlapping another? What is 5% of base rent? Has this payment already been marked paid?"*
- **Repository:** An interface that talks to the database. It saves, updates, deletes, and finds records.

---

### Concept 2: JPA Entities (No Lombok, Pure Java)

Look at `com.rentroll.entity.Property`:

```java
@Entity
@Table(name = "properties")
public class Property {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Property name is required")
    @Column(name = "property_name", nullable = false)
    private String propertyName;

    @NotNull(message = "Monthly asking rent is required")
    @DecimalMin(value = "0.0", inclusive = false)
    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "landlord_id", nullable = false)
    private User landlord;

    // Standard constructor, getters, and setters written explicitly
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    ...
}
```

**What to notice:**
- `@Entity`: Tells Hibernate that this class represents a table in the database.
- `@Id` and `@GeneratedValue`: Marks `id` as the auto-incrementing Primary Key.
- `@ManyToOne`: Many properties can belong to one landlord (`User`).
- `BigDecimal`: We **never** use `float` or `double` for currency because floating-point math can introduce rounding errors (e.g., `0.1 + 0.2 = 0.30000000000000004`). `BigDecimal` provides exact precision.

---

### Concept 3: Spring Data JPA Repositories

Look at `com.rentroll.repository.RentPaymentRepository`:

```java
public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    List<RentPayment> findByPropertyLandlordIdOrderByDueDateDesc(Long landlordId);

    boolean existsByLeaseIdAndRentalPeriod(Long leaseId, String rentalPeriod);

    @Query("SELECT p FROM RentPayment p WHERE p.property.landlord.id = :landlordId " +
           "AND (:status IS NULL OR p.status = :status)")
    List<RentPayment> filterLandlordPayments(@Param("landlordId") Long landlordId,
                                            @Param("status") PaymentStatus status);
}
```

**Magic of Spring Data JPA:**
You do **not** write SQL for `existsByLeaseIdAndRentalPeriod`. Spring parses the method name:
- `existsBy`: checks if a row exists
- `LeaseId`: where `lease.id = ?`
- `AndRentalPeriod`: and `rental_period = ?`
It generates the SQL query automatically at runtime! For custom filtering, we use `@Query` with JPQL (Java Persistence Query Language).

---

### Concept 4: Business Logic in Services

Look at the 5% late fee calculation in `com.rentroll.service.RentPaymentService`:

```java
public static final BigDecimal LATE_FEE_RATE = new BigDecimal("0.05"); // 5%

public BigDecimal calculateLateFee(BigDecimal baseRent) {
    if (baseRent == null) {
        return BigDecimal.ZERO;
    }
    // Multiply base rent by 0.05 and round half up to 2 decimal places
    return baseRent.multiply(LATE_FEE_RATE).setScale(2, RoundingMode.HALF_UP);
}
```

And how overdue payments are marked as paid:
```java
// Check if payment was delayed
boolean isOverdue = payment.getStatus() == PaymentStatus.LATE ||
        (payment.getDueDate() != null && payment.getDueDate().isBefore(effectivePaymentDate));

if (isOverdue && (payment.getLateFee() == null || payment.getLateFee().compareTo(BigDecimal.ZERO) == 0)) {
    BigDecimal fee = calculateLateFee(payment.getBaseRent());
    payment.setLateFee(fee);
    payment.setTotalAmount(payment.getBaseRent().add(fee));
}

payment.setStatus(PaymentStatus.PAID);
payment.setPaymentDate(effectivePaymentDate);
```

**Key Takeaways:**
1. All rules are in one predictable method.
2. The fee is never added more than once.
3. Rounding is explicit with `RoundingMode.HALF_UP`.

---

### Concept 5: Safe Automated Rent Schedule Generation

Look at `com.rentroll.service.LeaseService`:

```java
public void generateRentSchedule(Lease lease) {
    LocalDate currentMonth = lease.getStartDate().withDayOfMonth(1);
    LocalDate endMonth = lease.getEndDate().withDayOfMonth(1);
    LocalDate today = LocalDate.now();

    while (!currentMonth.isAfter(endMonth)) {
        String rentalPeriod = currentMonth.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH)
                + " " + currentMonth.getYear();

        // 1. Duplicate guard: Never recreate existing records
        boolean alreadyExists = rentPaymentRepository.existsByLeaseIdAndRentalPeriod(lease.getId(), rentalPeriod);

        if (!alreadyExists) {
            int dueDay = lease.getDueDayOfMonth() != null ? lease.getDueDayOfMonth() : 5;
            LocalDate dueDate = currentMonth.withDayOfMonth(Math.min(dueDay, currentMonth.lengthOfMonth()));

            // Determine status based on due date
            PaymentStatus status = dueDate.isBefore(today) ? PaymentStatus.LATE : PaymentStatus.UPCOMING;

            RentPayment payment = new RentPayment(
                    lease, lease.getProperty(), lease.getTenant(),
                    rentalPeriod, dueDate, lease.getMonthlyRent(),
                    BigDecimal.ZERO, lease.getMonthlyRent(), status
            );
            rentPaymentRepository.save(payment);
        }
        currentMonth = currentMonth.plusMonths(1);
    }
}
```

This ensures that whether a lease lasts 6 months or 24 months, every single monthly cycle gets an individual record in the ledger immediately upon agreement signing.

---

### Concept 6: Server-Rendered Views with Thymeleaf

Look at `src/main/resources/templates/dashboard/landlord.html`:

```html
<!-- Displaying formatted currency -->
<div class="stat-value">
    ₹<span th:text="${#numbers.formatDecimal(stats.collectedRentCurrentMonth, 1, 'COMMA', 0, 'POINT')}">0</span>
</div>

<!-- Looping through recent payments -->
<tr th:each="p : ${stats.recentPayments}">
    <td th:text="${p.property.propertyName}">Property</td>
    <td th:text="${p.rentalPeriod}">Month</td>
    <td>
        <span th:if="${p.status.name() == 'PAID'}" class="badge-status badge-paid">Paid</span>
        <span th:if="${p.status.name() == 'LATE'}" class="badge-status badge-late">Overdue</span>
    </td>
</tr>
```

- `th:text`: Replaces placeholder HTML text with real data from Java.
- `th:each`: Loops over a Java `List`.
- `th:if`: Shows or hides HTML elements based on boolean conditions.
- `th:href`: Generates context-aware URLs (e.g. `th:href="@{/properties/{id}(id=${property.id})}"`).

---

### Concept 7: Spring Security & Role-Based Access Control

Look at `com.rentroll.config.SecurityConfig`:

```java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/", "/login", "/css/**", "/js/**").permitAll()
    .requestMatchers("/users/**").hasRole("ADMIN")
    .anyRequest().authenticated()
)
.formLogin(form -> form
    .loginPage("/login")
    .usernameParameter("email")
    .passwordParameter("password")
    .defaultSuccessUrl("/dashboard", true)
)
```

In the controllers, we also check ownership:
```java
// A landlord can only edit their own property
if (currentUser.getRole() == Role.LANDLORD && !property.getLandlord().getId().equals(currentUser.getId())) {
    throw new UnauthorizedAccessException("You do not have permission to access this property");
}
```
This double-layer approach (URL restrictions + service-level checks) ensures that users cannot tamper with URLs to view other people's data.

---

## 3. Recommended Exercises for Practice

Try making these small enhancements to test your understanding:

1. **Add a new property type:** Add `PENTHOUSE` to `com.rentroll.entity.PropertyType`. Notice how it immediately appears in all dropdowns and search filters!
2. **Add a payment search filter:** Look at `RentPaymentRepository.filterLandlordPayments` and add a filter by payment method (e.g., find all payments paid via UPI).
3. **Change the late fee percentage:** Open `RentPaymentService.java` and change `LATE_FEE_RATE` from `0.05` to `0.08` (8%). Run `mvn test` to see how the test alerts you to update the test expectation!

Happy coding!
