package com.staykonkan.availability.repository;

import com.staykonkan.availability.entity.AvailabilityStatus;
import com.staykonkan.availability.entity.PropertyAvailability;
import com.staykonkan.booking.entity.Booking;
import com.staykonkan.property.entity.Property;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface PropertyAvailabilityRepository extends JpaRepository<PropertyAvailability, Long> {

    List<PropertyAvailability> findByPropertyAndAvailableDateBetweenOrderByAvailableDateAsc(
            Property property, LocalDate startDate, LocalDate endDate);

    default List<PropertyAvailability> findAvailabilityByProperty(
            Property property, LocalDate startDate, LocalDate endDate) {
        return findByPropertyAndAvailableDateBetweenOrderByAvailableDateAsc(property, startDate, endDate);
    }

    default List<PropertyAvailability> findCalendar(
            Property property, LocalDate startDate, LocalDate endDate) {
        return findByPropertyAndAvailableDateBetweenOrderByAvailableDateAsc(property, startDate, endDate);
    }

    List<PropertyAvailability> findByPropertyAndStatusAndAvailableDateBetween(
            Property property, AvailabilityStatus status, LocalDate startDate, LocalDate endDate);

    default List<PropertyAvailability> findBookedDates(
            Property property, LocalDate startDate, LocalDate endDate) {
        return findByPropertyAndStatusAndAvailableDateBetween(property, AvailabilityStatus.BOOKED, startDate, endDate);
    }

    default List<PropertyAvailability> findAvailableDates(
            Property property, LocalDate startDate, LocalDate endDate) {
        return findByPropertyAndStatusAndAvailableDateBetween(property, AvailabilityStatus.AVAILABLE, startDate, endDate);
    }

    boolean existsByPropertyAndStatusAndAvailableDateBetween(
            Property property, AvailabilityStatus status, LocalDate startDate, LocalDate endDate);

    default boolean existsBookedBetweenDates(Property property, LocalDate startDate, LocalDate endDate) {
        return existsByPropertyAndStatusAndAvailableDateBetween(property, AvailabilityStatus.BOOKED, startDate, endDate);
    }

    boolean existsByPropertyAndAvailableDateBetweenAndStatusIn(
            Property property, LocalDate startDate, LocalDate endDate, List<AvailabilityStatus> statuses);

    List<PropertyAvailability> findByBooking(Booking booking);

    void deleteAllByBooking(Booking booking);

    /**
     * Row-locks every existing availability row in the range so a
     * concurrent confirmation for an overlapping stay on the same
     * property blocks until this transaction commits or rolls back,
     * closing the race window that a simple exists-check alone cannot.
     * Must be called from within a @Transactional method. The unique
     * constraint on (property_id, available_date) is the second,
     * DB-enforced layer of defense if this is ever bypassed.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT pa FROM PropertyAvailability pa "
            + "WHERE pa.property = :property AND pa.availableDate BETWEEN :startDate AND :endDate")
    List<PropertyAvailability> lockDateRangeForUpdate(
            @Param("property") Property property,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);
}
