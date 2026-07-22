package com.staykonkan.availability.service.impl;

import com.staykonkan.availability.dto.AvailabilityCalendarResponse;
import com.staykonkan.availability.dto.AvailabilityResponse;
import com.staykonkan.availability.dto.UpdateAvailabilityRequest;
import com.staykonkan.availability.entity.AvailabilityStatus;
import com.staykonkan.availability.entity.PropertyAvailability;
import com.staykonkan.availability.mapper.PropertyAvailabilityMapper;
import com.staykonkan.availability.repository.PropertyAvailabilityRepository;
import com.staykonkan.availability.service.PropertyAvailabilityService;
import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.repository.BookingRepository;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.property.entity.Property;
import com.staykonkan.property.repository.PropertyRepository;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
@RequiredArgsConstructor
public class PropertyAvailabilityServiceImpl implements PropertyAvailabilityService {

    // Bounds a single query/calendar/block/release request so nobody can
    // request or lock an unbounded date range (OWASP: resource-consumption
    // hardening, same spirit as PropertyImage's MAX_IMAGES_PER_PROPERTY).
    private static final int MAX_RANGE_DAYS = 366;

    private final PropertyAvailabilityRepository availabilityRepository;
    private final PropertyRepository propertyRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PropertyAvailabilityMapper availabilityMapper;

    @Override
    public List<AvailabilityResponse> getAvailability(Long propertyId, LocalDate startDate, LocalDate endDate) {

        Property property = findPropertyOrThrow(propertyId);
        validateRange(startDate, endDate);

        return availabilityRepository.findAvailabilityByProperty(property, startDate, endDate)
                .stream()
                .map(availabilityMapper::toResponse)
                .toList();
    }

    @Override
    public List<AvailabilityResponse> blockDates(Long propertyId, UpdateAvailabilityRequest request) {

        User currentUser = getCurrentUser();
        Property property = findPropertyOrThrow(propertyId);
        assertOwnerOrAdmin(property, currentUser);

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        validateRange(startDate, endDate);

        AvailabilityStatus targetStatus = request.getStatus();
        if (targetStatus != AvailabilityStatus.BLOCKED && targetStatus != AvailabilityStatus.MAINTENANCE) {
            throw new ValidationException(
                    "status must be BLOCKED or MAINTENANCE to block dates; BOOKED/AVAILABLE are set automatically by the booking lifecycle");
        }

        // Lock the range first so a concurrent booking confirmation can't
        // race with this block (both mutate the same rows).
        List<PropertyAvailability> existing = availabilityRepository.lockDateRangeForUpdate(property, startDate, endDate);
        Map<LocalDate, PropertyAvailability> byDate = toDateMap(existing);

        for (PropertyAvailability row : existing) {
            if (row.getStatus() == AvailabilityStatus.BOOKED) {
                throw new ValidationException(
                        "Cannot block " + row.getAvailableDate() + " — it already has a confirmed booking");
            }
        }

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            PropertyAvailability row = byDate.get(date);
            if (row != null) {
                row.setStatus(targetStatus);
            } else {
                availabilityRepository.save(PropertyAvailability.builder()
                        .property(property)
                        .availableDate(date)
                        .status(targetStatus)
                        .build());
            }
        }

        return getAvailability(propertyId, startDate, endDate);
    }

    @Override
    public List<AvailabilityResponse> releaseDates(Long propertyId, UpdateAvailabilityRequest request) {

        User currentUser = getCurrentUser();
        Property property = findPropertyOrThrow(propertyId);
        assertOwnerOrAdmin(property, currentUser);

        LocalDate startDate = request.getStartDate();
        LocalDate endDate = request.getEndDate();
        validateRange(startDate, endDate);

        List<PropertyAvailability> existing = availabilityRepository.lockDateRangeForUpdate(property, startDate, endDate);

        for (PropertyAvailability row : existing) {
            if (row.getStatus() == AvailabilityStatus.BOOKED) {
                throw new ValidationException(
                        "Cannot release " + row.getAvailableDate()
                                + " — it has a confirmed booking; cancel the booking instead");
            }
        }

        // Release = delete, returning the range to its implicit AVAILABLE
        // state, consistent with this module's sparse-storage design.
        existing.forEach(availabilityRepository::delete);

        return getAvailability(propertyId, startDate, endDate);
    }

    @Override
    public void bookDates(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate, Long bookingId) {

        Property property = findPropertyOrThrow(propertyId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        LocalDate lastNight = checkOutDate.minusDays(1);
        validateRange(checkInDate, lastNight);

        try {
            // Row-level lock closes the race window between two concurrent
            // confirmations for overlapping dates on the same property.
            List<PropertyAvailability> existing =
                    availabilityRepository.lockDateRangeForUpdate(property, checkInDate, lastNight);
            Map<LocalDate, PropertyAvailability> byDate = toDateMap(existing);

            for (PropertyAvailability row : existing) {
                boolean belongsToThisBooking = row.getBooking() != null && row.getBooking().getId().equals(bookingId);
                if (row.getStatus() != AvailabilityStatus.AVAILABLE && !belongsToThisBooking) {
                    throw new ValidationException(
                            "Cannot confirm booking — " + row.getAvailableDate() + " is no longer available");
                }
            }

            for (LocalDate date = checkInDate; !date.isAfter(lastNight); date = date.plusDays(1)) {
                PropertyAvailability row = byDate.get(date);
                if (row != null) {
                    row.setStatus(AvailabilityStatus.BOOKED);
                    row.setBooking(booking);
                } else {
                    availabilityRepository.save(PropertyAvailability.builder()
                            .property(property)
                            .availableDate(date)
                            .status(AvailabilityStatus.BOOKED)
                            .booking(booking)
                            .build());
                }
            }
        } catch (DataIntegrityViolationException e) {
            // Safety net behind the pessimistic lock: the unique
            // (property_id, available_date) constraint is the last line
            // of defense if two transactions ever raced past the lock.
            throw new ValidationException("Selected dates were just booked — please choose different dates");
        }
    }

    @Override
    public void releaseBookingDates(Long bookingId) {

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        // No-op if the booking never reached CONFIRMED (no rows were ever
        // created for it) — cancelling a PENDING booking is valid and
        // must not fail here.
        List<PropertyAvailability> rows = availabilityRepository.findByBooking(booking);
        rows.forEach(availabilityRepository::delete);
    }

    @Override
    public boolean checkAvailability(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate) {

        Property property = findPropertyOrThrow(propertyId);

        LocalDate lastNight = checkOutDate.minusDays(1);
        validateRange(checkInDate, lastNight);

        boolean hasUnavailable = availabilityRepository.existsByPropertyAndAvailableDateBetweenAndStatusIn(
                property, checkInDate, lastNight,
                List.of(AvailabilityStatus.BOOKED, AvailabilityStatus.BLOCKED, AvailabilityStatus.MAINTENANCE));

        return !hasUnavailable;
    }

    @Override
    public AvailabilityCalendarResponse getCalendar(Long propertyId, LocalDate startDate, LocalDate endDate) {

        Property property = findPropertyOrThrow(propertyId);
        validateRange(startDate, endDate);

        Map<LocalDate, PropertyAvailability> byDate =
                toDateMap(availabilityRepository.findCalendar(property, startDate, endDate));

        List<AvailabilityCalendarResponse.AvailabilityDay> days = new ArrayList<>();
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            PropertyAvailability row = byDate.get(date);
            days.add(row != null
                    ? availabilityMapper.toCalendarDay(row)
                    : availabilityMapper.implicitCalendarDay(date));
        }

        return AvailabilityCalendarResponse.builder()
                .propertyId(propertyId)
                .startDate(startDate)
                .endDate(endDate)
                .days(days)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Property findPropertyOrThrow(Long propertyId) {
        return propertyRepository.findById(propertyId)
                .orElseThrow(() -> ResourceNotFoundException.of("Property", propertyId));
    }

    private void assertOwnerOrAdmin(Property property, User currentUser) {
        boolean isOwner = property.getOwner().getId().equals(currentUser.getId());
        if (!isOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the property owner or an admin can manage availability for this property");
        }
    }

    private void validateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            throw new ValidationException("Start date and end date are required");
        }
        if (endDate.isBefore(startDate)) {
            throw new ValidationException("End date must not be before start date");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > MAX_RANGE_DAYS) {
            throw new ValidationException("Date range must not exceed " + MAX_RANGE_DAYS + " days");
        }
    }

    private Map<LocalDate, PropertyAvailability> toDateMap(List<PropertyAvailability> rows) {
        Map<LocalDate, PropertyAvailability> map = new HashMap<>();
        for (PropertyAvailability row : rows) {
            map.put(row.getAvailableDate(), row);
        }
        return map;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
