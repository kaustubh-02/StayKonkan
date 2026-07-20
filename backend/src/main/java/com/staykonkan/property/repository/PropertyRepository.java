package com.staykonkan.property.repository;

import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;

public interface PropertyRepository extends JpaRepository<Property, Long> {

    Page<Property> findByLocationContainingIgnoreCase(String location, Pageable pageable);

    Page<Property> findByPricePerNightBetween(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    Page<Property> findByMaxGuestsGreaterThanEqual(
            Integer guests,
            Pageable pageable
    );

    Page<Property> findByOwner(
            User owner,
            Pageable pageable
    );
}