package com.staykonkan.availability.dto;

import com.staykonkan.availability.entity.AvailabilityStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * A gap-filled, day-by-day view for the requested range — unlike
 * AvailabilityService.getAvailability(), which only returns persisted
 * "exception" rows, this synthesizes an implicit AVAILABLE entry for
 * every date in range that has no row, so the frontend can render a
 * complete calendar without knowing about sparse storage.
 */
@Getter
@Setter
@Builder
public class AvailabilityCalendarResponse {

    private Long propertyId;

    private LocalDate startDate;

    private LocalDate endDate;

    private List<AvailabilityDay> days;

    @Getter
    @Setter
    @Builder
    public static class AvailabilityDay {

        private LocalDate date;

        private AvailabilityStatus status;

        private Long bookingId;
    }
}
