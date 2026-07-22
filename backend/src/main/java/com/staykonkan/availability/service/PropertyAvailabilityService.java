package com.staykonkan.availability.service;

import com.staykonkan.availability.dto.AvailabilityCalendarResponse;
import com.staykonkan.availability.dto.AvailabilityResponse;
import com.staykonkan.availability.dto.UpdateAvailabilityRequest;

import java.time.LocalDate;
import java.util.List;

public interface PropertyAvailabilityService {

    /** Persisted "exception" rows only (sparse) — dates with no row are simply not included. */
    List<AvailabilityResponse> getAvailability(Long propertyId, LocalDate startDate, LocalDate endDate);

    List<AvailabilityResponse> blockDates(Long propertyId, UpdateAvailabilityRequest request);

    List<AvailabilityResponse> releaseDates(Long propertyId, UpdateAvailabilityRequest request);

    /**
     * Marks [checkInDate, checkOutDate) as BOOKED for the given booking,
     * under a row-level lock, and fails atomically if any date in range
     * is no longer available. Called by BookingServiceImpl when a
     * booking is confirmed — not exposed via the controller.
     */
    void bookDates(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate, Long bookingId);

    /**
     * Releases whatever availability rows are currently tied to the given
     * booking back to the implicit AVAILABLE state. Called by
     * BookingServiceImpl on cancellation; safe to call even if the
     * booking was never confirmed (no-op when no rows exist).
     */
    void releaseBookingDates(Long bookingId);

    /** True if every date in [checkInDate, checkOutDate) is currently available. */
    boolean checkAvailability(Long propertyId, LocalDate checkInDate, LocalDate checkOutDate);

    /** Gap-filled day-by-day calendar for [startDate, endDate], inclusive both ends. */
    AvailabilityCalendarResponse getCalendar(Long propertyId, LocalDate startDate, LocalDate endDate);
}
