package com.staykonkan.booking.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateBookingRequest {

    @NotNull
    private Long propertyId;

    @NotNull
    @Future
    private LocalDate checkInDate;

    @NotNull
    @Future
    private LocalDate checkOutDate;

    @NotNull
    @Min(1)
    private Integer guestCount;

    private String specialRequests;
}