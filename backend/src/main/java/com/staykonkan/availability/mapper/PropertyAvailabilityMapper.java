package com.staykonkan.availability.mapper;

import com.staykonkan.availability.dto.AvailabilityCalendarResponse;
import com.staykonkan.availability.dto.AvailabilityResponse;
import com.staykonkan.availability.entity.AvailabilityStatus;
import com.staykonkan.availability.entity.PropertyAvailability;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
public class PropertyAvailabilityMapper {

    public AvailabilityResponse toResponse(PropertyAvailability availability) {

        AvailabilityResponse.AvailabilityResponseBuilder builder = AvailabilityResponse.builder()
                .id(availability.getId())
                .availableDate(availability.getAvailableDate())
                .status(availability.getStatus())
                .createdAt(availability.getCreatedAt())
                .updatedAt(availability.getUpdatedAt());

        if (availability.getProperty() != null) {
            builder.propertyId(availability.getProperty().getId());
        }
        if (availability.getBooking() != null) {
            builder.bookingId(availability.getBooking().getId());
        }

        return builder.build();
    }

    /** Synthesizes an implicit AVAILABLE entry for a date that has no persisted row. */
    public AvailabilityResponse implicitAvailable(Long propertyId, LocalDate date) {
        return AvailabilityResponse.builder()
                .propertyId(propertyId)
                .availableDate(date)
                .status(AvailabilityStatus.AVAILABLE)
                .build();
    }

    public AvailabilityCalendarResponse.AvailabilityDay toCalendarDay(PropertyAvailability availability) {
        return AvailabilityCalendarResponse.AvailabilityDay.builder()
                .date(availability.getAvailableDate())
                .status(availability.getStatus())
                .bookingId(availability.getBooking() != null ? availability.getBooking().getId() : null)
                .build();
    }

    public AvailabilityCalendarResponse.AvailabilityDay implicitCalendarDay(LocalDate date) {
        return AvailabilityCalendarResponse.AvailabilityDay.builder()
                .date(date)
                .status(AvailabilityStatus.AVAILABLE)
                .bookingId(null)
                .build();
    }
}
