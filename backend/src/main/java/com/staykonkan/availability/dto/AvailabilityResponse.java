package com.staykonkan.availability.dto;

import com.staykonkan.availability.entity.AvailabilityStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Builder
public class AvailabilityResponse {

    /** Null for dates with no persisted row — i.e. an implicit AVAILABLE date. */
    private Long id;

    private Long propertyId;

    private LocalDate availableDate;

    private AvailabilityStatus status;

    /** Null unless status = BOOKED. */
    private Long bookingId;

    private Instant createdAt;

    private Instant updatedAt;
}
