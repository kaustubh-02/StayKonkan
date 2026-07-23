package com.staykonkan.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * Deliberately just the booking id. The amount is NEVER accepted from
 * the client — it's always read server-side from booking.totalAmount
 * (OWASP: never trust client-supplied payment amounts).
 */
@Getter
@Setter
public class CreatePaymentRequest {

    @NotNull(message = "Booking id is required")
    private Long bookingId;
}
