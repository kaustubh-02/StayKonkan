package com.staykonkan.review.repository;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.property.entity.Property;
import com.staykonkan.review.entity.Review;
import com.staykonkan.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByBooking(Booking booking);

    boolean existsByBooking(Booking booking);

    Page<Review> findByProperty(Property property, Pageable pageable);

    Page<Review> findByUser(User user, Pageable pageable);

    long countByProperty(Property property);

    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r WHERE r.property = :property")
    Double findAverageRatingByProperty(@Param("property") Property property);
}
