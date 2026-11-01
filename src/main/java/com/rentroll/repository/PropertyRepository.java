package com.rentroll.repository;

import com.rentroll.entity.Property;
import com.rentroll.entity.PropertyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Spring Data JPA repository for Property entity operations.
 */
@Repository
public interface PropertyRepository extends JpaRepository<Property, Long> {

    List<Property> findByLandlordId(Long landlordId);

    long countByLandlordId(Long landlordId);

    // Search and filter for Landlords
    @Query("SELECT p FROM Property p WHERE p.landlord.id = :landlordId " +
           "AND (:query IS NULL OR LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.city) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:propertyType IS NULL OR p.propertyType = :propertyType)")
    List<Property> searchLandlordProperties(@Param("landlordId") Long landlordId,
                                           @Param("query") String query,
                                           @Param("propertyType") PropertyType propertyType);

    // Search and filter for Admin
    @Query("SELECT p FROM Property p WHERE " +
           "(:query IS NULL OR LOWER(p.propertyName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.address) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "     OR LOWER(p.city) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:propertyType IS NULL OR p.propertyType = :propertyType)")
    List<Property> searchAllProperties(@Param("query") String query,
                                       @Param("propertyType") PropertyType propertyType);
}

