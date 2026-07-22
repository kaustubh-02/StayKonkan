package com.staykonkan.propertyimage.repository;

import com.staykonkan.property.entity.Property;
import com.staykonkan.propertyimage.entity.PropertyImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyImageRepository extends JpaRepository<PropertyImage, Long> {

    List<PropertyImage> findByPropertyOrderByDisplayOrderAsc(Property property);

    // Convenience alias matching the requested method name; delegates to the
    // ordered variant above since callers always want display order.
    default List<PropertyImage> findByProperty(Property property) {
        return findByPropertyOrderByDisplayOrderAsc(property);
    }

    Optional<PropertyImage> findByPropertyAndIsCoverTrue(Property property);

    default Optional<PropertyImage> findCoverImage(Property property) {
        return findByPropertyAndIsCoverTrue(property);
    }

    long countByProperty(Property property);

    void deleteByPublicId(String publicId);

    void deleteAllByProperty(Property property);
}
