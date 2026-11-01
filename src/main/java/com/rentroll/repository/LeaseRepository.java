package com.rentroll.repository;

import com.rentroll.entity.Lease;
import com.rentroll.entity.LeaseStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for Lease entity operations.
 */
@Repository
public interface LeaseRepository extends JpaRepository<Lease, Long> {

    List<Lease> findByPropertyLandlordId(Long landlordId);

    List<Lease> findByTenantId(Long tenantId);

    List<Lease> findByPropertyId(Long propertyId);

    long countByStatus(LeaseStatus status);

    long countByPropertyLandlordIdAndStatus(Long landlordId, LeaseStatus status);

    Optional<Lease> findByPropertyIdAndStatus(Long propertyId, LeaseStatus status);

    // Find active lease for a specific tenant
    Optional<Lease> findFirstByTenantIdAndStatus(Long tenantId, LeaseStatus status);

    // Detect overlapping active leases on the same property
    @Query("SELECT l FROM Lease l WHERE l.property.id = :propertyId " +
           "AND l.status = 'ACTIVE' " +
           "AND (:excludeLeaseId IS NULL OR l.id != :excludeLeaseId) " +
           "AND (l.startDate <= :endDate AND l.endDate >= :startDate)")
    List<Lease> findOverlappingActiveLeases(@Param("propertyId") Long propertyId,
                                           @Param("startDate") LocalDate startDate,
                                           @Param("endDate") LocalDate endDate,
                                           @Param("excludeLeaseId") Long excludeLeaseId);

    // Leases expiring within next N days (for landlord)
    @Query("SELECT l FROM Lease l WHERE l.property.landlord.id = :landlordId " +
           "AND l.status = 'ACTIVE' " +
           "AND l.endDate BETWEEN :today AND :expiryLimit " +
           "ORDER BY l.endDate ASC")
    List<Lease> findExpiringLeasesForLandlord(@Param("landlordId") Long landlordId,
                                             @Param("today") LocalDate today,
                                             @Param("expiryLimit") LocalDate expiryLimit);

    // Leases expiring across entire system (for Admin or overall reports)
    @Query("SELECT l FROM Lease l WHERE l.status = 'ACTIVE' " +
           "AND l.endDate BETWEEN :today AND :expiryLimit " +
           "ORDER BY l.endDate ASC")
    List<Lease> findAllExpiringLeases(@Param("today") LocalDate today,
                                     @Param("expiryLimit") LocalDate expiryLimit);
}

