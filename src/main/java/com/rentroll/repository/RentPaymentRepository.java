package com.rentroll.repository;

import com.rentroll.entity.PaymentStatus;
import com.rentroll.entity.RentPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Spring Data JPA repository for RentPayment entity operations.
 */
@Repository
public interface RentPaymentRepository extends JpaRepository<RentPayment, Long> {

    List<RentPayment> findByPropertyLandlordIdOrderByDueDateDesc(Long landlordId);

    List<RentPayment> findByTenantIdOrderByDueDateDesc(Long tenantId);

    List<RentPayment> findByLeaseIdOrderByDueDateAsc(Long leaseId);

    List<RentPayment> findByPropertyId(Long propertyId);

    boolean existsByLeaseIdAndRentalPeriod(Long leaseId, String rentalPeriod);

    // Overdue payments: due date is past today and status is UPCOMING or PENDING
    @Query("SELECT p FROM RentPayment p WHERE p.dueDate < :today AND p.status IN ('UPCOMING', 'PENDING')")
    List<RentPayment> findPaymentsNeedingLateStatus(@Param("today") LocalDate today);

    // Filter payments for a Landlord
    @Query("SELECT p FROM RentPayment p WHERE p.property.landlord.id = :landlordId " +
           "AND (:status IS NULL OR p.status = :status) " +
           "AND (:query IS NULL OR LOWER(p.property.propertyName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.tenant.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.rentalPeriod) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.dueDate DESC")
    List<RentPayment> filterLandlordPayments(@Param("landlordId") Long landlordId,
                                            @Param("status") PaymentStatus status,
                                            @Param("query") String query);

    // Filter payments for Admin
    @Query("SELECT p FROM RentPayment p WHERE " +
           "(:status IS NULL OR p.status = :status) " +
           "AND (:query IS NULL OR LOWER(p.property.propertyName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.tenant.fullName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.rentalPeriod) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.dueDate DESC")
    List<RentPayment> filterAllPayments(@Param("status") PaymentStatus status,
                                        @Param("query") String query);

    // Recent payments for landlord dashboard
    List<RentPayment> findTop5ByPropertyLandlordIdOrderByDueDateDesc(Long landlordId);

    // Upcoming dues for landlord dashboard
    @Query("SELECT p FROM RentPayment p WHERE p.property.landlord.id = :landlordId " +
           "AND p.status IN ('UPCOMING', 'PENDING') " +
           "AND p.dueDate >= :today ORDER BY p.dueDate ASC")
    List<RentPayment> findUpcomingDuesForLandlord(@Param("landlordId") Long landlordId,
                                                @Param("today") LocalDate today);

    // Count overdue payments for landlord
    @Query("SELECT COUNT(p) FROM RentPayment p WHERE p.property.landlord.id = :landlordId " +
           "AND (p.status = 'LATE' OR (p.dueDate < :today AND p.status IN ('UPCOMING', 'PENDING')))")
    long countOverduePaymentsForLandlord(@Param("landlordId") Long landlordId, @Param("today") LocalDate today);

    // Count overdue payments for all
    @Query("SELECT COUNT(p) FROM RentPayment p WHERE p.status = 'LATE' OR (p.dueDate < :today AND p.status IN ('UPCOMING', 'PENDING'))")
    long countAllOverduePayments(@Param("today") LocalDate today);

    // Calculate sum of collected rent for landlord in date range
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RentPayment p " +
           "WHERE p.property.landlord.id = :landlordId AND p.status = 'PAID' " +
           "AND p.paymentDate BETWEEN :startDate AND :endDate")
    BigDecimal sumCollectedRentForLandlord(@Param("landlordId") Long landlordId,
                                          @Param("startDate") LocalDate startDate,
                                          @Param("endDate") LocalDate endDate);

    // Calculate sum of expected rent for landlord in date range (all except cancelled)
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RentPayment p " +
           "WHERE p.property.landlord.id = :landlordId AND p.status != 'CANCELLED' " +
           "AND p.dueDate BETWEEN :startDate AND :endDate")
    BigDecimal sumExpectedRentForLandlord(@Param("landlordId") Long landlordId,
                                         @Param("startDate") LocalDate startDate,
                                         @Param("endDate") LocalDate endDate);

    // Calculate sum of outstanding rent for landlord (pending + late)
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RentPayment p " +
           "WHERE p.property.landlord.id = :landlordId AND p.status IN ('PENDING', 'LATE')")
    BigDecimal sumOutstandingRentForLandlord(@Param("landlordId") Long landlordId);

    // Total collected rent across all properties
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RentPayment p WHERE p.status = 'PAID'")
    BigDecimal sumAllCollectedRent();

    // Total outstanding rent across all properties
    @Query("SELECT COALESCE(SUM(p.totalAmount), 0) FROM RentPayment p WHERE p.status IN ('PENDING', 'LATE')")
    BigDecimal sumAllOutstandingRent();
}

