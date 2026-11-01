package com.rentroll.repository;

import com.rentroll.entity.MaintenancePriority;
import com.rentroll.entity.MaintenanceRequest;
import com.rentroll.entity.MaintenanceStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for MaintenanceRequest entity operations.
 */
@Repository
public interface MaintenanceRequestRepository extends JpaRepository<MaintenanceRequest, Long> {

    List<MaintenanceRequest> findByPropertyLandlordIdOrderByCreatedAtDesc(Long landlordId);

    List<MaintenanceRequest> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    List<MaintenanceRequest> findByPropertyIdOrderByCreatedAtDesc(Long propertyId);

    long countByPropertyLandlordIdAndStatus(Long landlordId, MaintenanceStatus status);

    long countByTenantIdAndStatus(Long tenantId, MaintenanceStatus status);

    long countByStatus(MaintenanceStatus status);

    // Filter maintenance tickets for a landlord
    @Query("SELECT m FROM MaintenanceRequest m WHERE m.property.landlord.id = :landlordId " +
           "AND (:status IS NULL OR m.status = :status) " +
           "AND (:priority IS NULL OR m.priority = :priority) " +
           "ORDER BY m.createdAt DESC")
    List<MaintenanceRequest> filterLandlordRequests(@Param("landlordId") Long landlordId,
                                                    @Param("status") MaintenanceStatus status,
                                                    @Param("priority") MaintenancePriority priority);

    // Filter maintenance tickets for Admin
    @Query("SELECT m FROM MaintenanceRequest m WHERE " +
           "(:status IS NULL OR m.status = :status) " +
           "AND (:priority IS NULL OR m.priority = :priority) " +
           "ORDER BY m.createdAt DESC")
    List<MaintenanceRequest> filterAllRequests(@Param("status") MaintenanceStatus status,
                                              @Param("priority") MaintenancePriority priority);
}

