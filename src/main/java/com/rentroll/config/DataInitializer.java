package com.rentroll.config;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import com.rentroll.entity.MaintenanceCategory;
import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.MaintenanceStatus;
import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import com.rentroll.entity.RentPayment;
import com.rentroll.entity.Role;
import com.rentroll.entity.User;
import com.rentroll.repository.LeaseRepository;
import com.rentroll.repository.MaintenanceRequestRepository;
import com.rentroll.repository.PropertyRepository;
import com.rentroll.repository.RentPaymentRepository;
import com.rentroll.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Initializes realistic sample development data for testing and demonstration.
 * Creates accounts for Admin, Landlords, and Tenants with Indian real estate context.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final LeaseRepository leaseRepository;
    private final RentPaymentRepository rentPaymentRepository;
    private final MaintenanceRequestRepository maintenanceRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           PropertyRepository propertyRepository,
                           LeaseRepository leaseRepository,
                           RentPaymentRepository rentPaymentRepository,
                           MaintenanceRequestRepository maintenanceRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.propertyRepository = propertyRepository;
        this.leaseRepository = leaseRepository;
        this.rentPaymentRepository = rentPaymentRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already contains data; skipping seed initialization.");
            return;
        }

        log.info("Seeding initial RentRoll development data...");

        // 1. Create Users
        User admin = new User(
                "Anita Sharma (Admin)",
                "admin@rentroll.in",
                passwordEncoder.encode("admin123"),
                "+91 9811122233",
                Role.ADMIN
        );
        userRepository.save(admin);

        User landlordRajesh = new User(
                "Rajesh Verma (Landlord)",
                "rajesh.landlord@rentroll.in",
                passwordEncoder.encode("landlord123"),
                "+91 9822012345",
                Role.LANDLORD
        );
        userRepository.save(landlordRajesh);

        User landlordPriya = new User(
                "Priya Kulkarni (Landlord)",
                "priya.landlord@rentroll.in",
                passwordEncoder.encode("landlord123"),
                "+91 9844056789",
                Role.LANDLORD
        );
        userRepository.save(landlordPriya);

        User tenantAmit = new User(
                "Amit Kumar (Tenant)",
                "amit.tenant@rentroll.in",
                passwordEncoder.encode("tenant123"),
                "+91 9900112233",
                Role.TENANT
        );
        userRepository.save(tenantAmit);

        User tenantSneha = new User(
                "Sneha Nair (Tenant)",
                "sneha.tenant@rentroll.in",
                passwordEncoder.encode("tenant123"),
                "+91 9944556677",
                Role.TENANT
        );
        userRepository.save(tenantSneha);

        User tenantVikram = new User(
                "Vikram Malhotra (Tenant)",
                "vikram.tenant@rentroll.in",
                passwordEncoder.encode("tenant123"),
                "+91 9988776655",
                Role.TENANT
        );
        userRepository.save(tenantVikram);

        // 2. Create Properties
        Property p1 = new Property(
                "Prestige Shantiniketan - Flat 702",
                "Tower 4, ITPL Main Road, Whitefield",
                "Bengaluru",
                "Karnataka",
                "560066",
                PropertyType.APARTMENT,
                3,
                new BigDecimal("42000.00"),
                "Spacious 3 BHK facing green courtyard with modular kitchen, 2 covered car parks, and club access.",
                landlordRajesh
        );
        propertyRepository.save(p1);

        Property p2 = new Property(
                "Green Glen Layout Villa #12",
                "Near Outer Ring Road, Bellandur",
                "Bengaluru",
                "Karnataka",
                "560103",
                PropertyType.INDEPENDENT_HOUSE,
                4,
                new BigDecimal("75000.00"),
                "Independent duplex 4 BHK villa with private terrace garden, power backup, and EV charging point.",
                landlordRajesh
        );
        propertyRepository.save(p2);

        Property p3 = new Property(
                "Hiranandani Gardens - Florencia 301",
                "Central Avenue, Powai",
                "Mumbai",
                "Maharashtra",
                "400076",
                PropertyType.APARTMENT,
                2,
                new BigDecimal("58000.00"),
                "Fully furnished 2 BHK overlooking Powai lake. Granite kitchen, piped gas, and Italian marble flooring.",
                landlordRajesh
        );
        propertyRepository.save(p3);

        Property p4 = new Property(
                "Amanora Park Town - Neo Towers 1104",
                "Hadapsar Bypass Road, Amanora Park",
                "Pune",
                "Maharashtra",
                "411028",
                PropertyType.APARTMENT,
                2,
                new BigDecimal("28000.00"),
                "Modern high-rise 2 BHK apartment with digital locks, video door phone, piped gas, and township amenities.",
                landlordPriya
        );
        propertyRepository.save(p4);

        Property p5 = new Property(
                "Cyber City Studio Suite 405",
                "Financial District, Gachibowli",
                "Hyderabad",
                "Telangana",
                "500032",
                PropertyType.STUDIO,
                1,
                new BigDecimal("22000.00"),
                "Fully serviced 1RK studio apartment ideal for IT professionals. Includes high-speed Wi-Fi and housekeeping.",
                landlordPriya
        );
        propertyRepository.save(p5);

        // 3. Create Leases
        // Lease 1: Rajesh -> Amit (Prestige Shantiniketan Flat 702)
        Lease l1 = new Lease(
                p1,
                tenantAmit,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 12, 31),
                new BigDecimal("42000.00"),
                5,
                new BigDecimal("150000.00"),
                LeaseStatus.ACTIVE
        );
        leaseRepository.save(l1);

        // Lease 2: Rajesh -> Sneha (Hiranandani Powai) - Expiring within 30 days!
        Lease l2 = new Lease(
                p3,
                tenantSneha,
                LocalDate.of(2025, 12, 1),
                LocalDate.of(2026, 11, 30),
                new BigDecimal("58000.00"),
                1,
                new BigDecimal("200000.00"),
                LeaseStatus.ACTIVE
        );
        leaseRepository.save(l2);

        // Lease 3: Priya -> Vikram (Amanora Park Town Pune)
        Lease l3 = new Lease(
                p4,
                tenantVikram,
                LocalDate.of(2026, 2, 1),
                LocalDate.of(2027, 1, 31),
                new BigDecimal("28000.00"),
                10,
                new BigDecimal("80000.00"),
                LeaseStatus.ACTIVE
        );
        leaseRepository.save(l3);

        // 4. Create Payments
        // Payments for Lease 1 (Amit Kumar - Jan 2026 to Dec 2026)
        String[] months2026 = {
                "January 2026", "February 2026", "March 2026", "April 2026",
                "May 2026", "June 2026", "July 2026", "August 2026",
                "September 2026", "October 2026", "November 2026", "December 2026"
        };

        for (int m = 1; m <= 12; m++) {
            LocalDate dueDate = LocalDate.of(2026, m, 5);
            PaymentStatus status;
            LocalDate paidDate = null;
            String method = null;
            String note = null;

            if (m <= 10) {
                // Past months paid on time
                status = PaymentStatus.PAID;
                paidDate = dueDate.minusDays(1);
                method = "UPI (Google Pay)";
                note = "Received via UPI Ref #" + (847291000L + m);
            } else if (m == 11) {
                // November 2026: Due soon / pending
                status = PaymentStatus.PENDING;
            } else {
                // December 2026: Upcoming
                status = PaymentStatus.UPCOMING;
            }

            RentPayment rp = new RentPayment(
                    l1, p1, tenantAmit, months2026[m - 1], dueDate,
                    l1.getMonthlyRent(), BigDecimal.ZERO, l1.getMonthlyRent(), status
            );
            rp.setPaymentDate(paidDate);
            rp.setPaymentMethod(method);
            rp.setNotes(note);
            rentPaymentRepository.save(rp);
        }

        // Payments for Lease 2 (Sneha Nair - Dec 2025 to Nov 2026)
        // Dec 2025 to Sep 2026: Paid
        for (int m = 1; m <= 9; m++) {
            RentPayment rp = new RentPayment(
                    l2, p3, tenantSneha, months2026[m - 1], LocalDate.of(2026, m, 1),
                    l2.getMonthlyRent(), BigDecimal.ZERO, l2.getMonthlyRent(), PaymentStatus.PAID
            );
            rp.setPaymentDate(LocalDate.of(2026, m, 1));
            rp.setPaymentMethod("IMPS Bank Transfer");
            rp.setNotes("HDFC Netbanking Ref #99281" + m);
            rentPaymentRepository.save(rp);
        }

        // October 2026: Overdue / LATE (Due Oct 1, 2026, unpaid)
        RentPayment latePayment = new RentPayment(
                l2, p3, tenantSneha, "October 2026", LocalDate.of(2026, 10, 1),
                l2.getMonthlyRent(), BigDecimal.ZERO, l2.getMonthlyRent(), PaymentStatus.LATE
        );
        latePayment.setNotes("Payment delayed by tenant; WhatsApp reminder sent");
        rentPaymentRepository.save(latePayment);

        // November 2026: PENDING
        RentPayment snehaNov = new RentPayment(
                l2, p3, tenantSneha, "November 2026", LocalDate.of(2026, 11, 1),
                l2.getMonthlyRent(), BigDecimal.ZERO, l2.getMonthlyRent(), PaymentStatus.PENDING
        );
        rentPaymentRepository.save(snehaNov);

        // Payments for Lease 3 (Vikram Malhotra - Amanora Pune)
        for (int m = 2; m <= 10; m++) {
            RentPayment rp = new RentPayment(
                    l3, p4, tenantVikram, months2026[m - 1], LocalDate.of(2026, m, 10),
                    l3.getMonthlyRent(), BigDecimal.ZERO, l3.getMonthlyRent(), PaymentStatus.PAID
            );
            rp.setPaymentDate(LocalDate.of(2026, m, 9));
            rp.setPaymentMethod("UPI (PhonePe)");
            rentPaymentRepository.save(rp);
        }
        RentPayment vikramNov = new RentPayment(
                l3, p4, tenantVikram, "November 2026", LocalDate.of(2026, 11, 10),
                l3.getMonthlyRent(), BigDecimal.ZERO, l3.getMonthlyRent(), PaymentStatus.UPCOMING
        );
        rentPaymentRepository.save(vikramNov);

        // 5. Create Maintenance Requests
        MaintenanceRequest req1 = new MaintenanceRequest(
                "Kitchen sink tap leaking persistently",
                "Water dripping slowly from the main sink mixer tap even after tightly turning off. Likely needs rubber washer replacement.",
                p1,
                tenantAmit,
                MaintenanceCategory.PLUMBING,
                MaintenancePriority.MEDIUM
        );
        maintenanceRepository.save(req1);

        MaintenanceRequest req2 = new MaintenanceRequest(
                "Geyser MCB tripping in master bathroom",
                "Whenever the water heater switch is turned on, the distribution box breaker trips immediately. Emergency attention needed.",
                p1,
                tenantAmit,
                MaintenanceCategory.ELECTRICAL,
                MaintenancePriority.HIGH
        );
        req2.setStatus(MaintenanceStatus.IN_PROGRESS);
        req2.setResolutionNotes("Electrician dispatched. Heating element being replaced.");
        maintenanceRepository.save(req2);

        MaintenanceRequest req3 = new MaintenanceRequest(
                "Balcony sliding door latch jammed",
                "Living room sliding glass door latch was sticking and difficult to lock. Lubricated and adjusted alignment.",
                p3,
                tenantSneha,
                MaintenanceCategory.STRUCTURAL,
                MaintenancePriority.LOW
        );
        req3.setStatus(MaintenanceStatus.RESOLVED);
        req3.setResolvedAt(LocalDateTime.now().minusDays(3));
        req3.setResolutionNotes("Lock alignment adjusted with silicone spray and tightening screws. Tested working smoothly.");
        maintenanceRepository.save(req3);

        MaintenanceRequest req4 = new MaintenanceRequest(
                "Air conditioner cooling gas refill needed",
                "Living room Split AC is blowing normal fan air instead of cool air. Likely refrigerant leak or servicing needed before summer.",
                p4,
                tenantVikram,
                MaintenanceCategory.APPLIANCE,
                MaintenancePriority.MEDIUM
        );
        maintenanceRepository.save(req4);

        log.info("RentRoll seed initialization complete! 6 users, 5 properties, 3 leases, payments & maintenance requests created.");
    }
}

