package com.staykonkan.booking.dto;

import com.staykonkan.booking.entity.BookingStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateBookingStatusRequest {

    @NotNull
    private BookingStatus status;
}