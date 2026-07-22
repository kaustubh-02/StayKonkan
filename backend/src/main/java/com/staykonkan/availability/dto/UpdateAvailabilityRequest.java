package com.staykonkan.availability.dto;

import com.staykonkan.availability.entity.AvailabilityStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Shared request body for both /block and /release. Both endpoints treat
 * startDate and endDate as an inclusive calendar-day range (unlike
 * booking check-in/check-out, which is checkout-exclusive) since this is
 * direct day-by-day calendar management, not a stay.
 * <p>
 * status is required for /block (and restricted there to BLOCKED or
 * MAINTENANCE — BOOKED/AVAILABLE are rejected since those transitions
 * only happen through the booking lifecycle) and ignored for /release,
 * which always resets the range back to the implicit AVAILABLE state.
 */
@Getter
@Setter
public class UpdateAvailabilityRequest {

    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    private AvailabilityStatus status;
}
