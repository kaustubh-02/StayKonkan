package com.staykonkan.amenity.repository;

import com.staykonkan.amenity.entity.Amenity;
import com.staykonkan.amenity.entity.AmenityCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AmenityRepository extends JpaRepository<Amenity, Long> {

    Optional<Amenity> findByName(String name);

    boolean existsByName(String name);

    List<Amenity> findByCategory(AmenityCategory category);

    List<Amenity> findByActiveTrue();

    default List<Amenity> findActiveAmenities() {
        return findByActiveTrue();
    }
}
