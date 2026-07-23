package com.staykonkan.refund.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * bookingId comes from the path, not the body. The only meaningful
 * client input for a full refund is an optional reason — no amount
 * field: this module only supports full refunds of the payment's
 * bookingAmount (see RefundServiceImpl), so accepting a client-supplied
 * amount would just be an unused/misleading field.
 */
@Getter
@Setter
public class RefundRequest {

    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
