package com.staykonkan.booking.repository;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.entity.BookingStatus;
import com.staykonkan.property.entity.Property;
import com.staykonkan.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    Page<Booking> findByGuest(User guest, Pageable pageable);

    Page<Booking> findByPropertyOwner(User owner, Pageable pageable);

    boolean existsByPropertyAndStatusInAndCheckInDateLessThanAndCheckOutDateGreaterThan(
            Property property,
            Iterable<BookingStatus> statuses,
            LocalDate checkOutDate,
            LocalDate checkInDate
    );
}