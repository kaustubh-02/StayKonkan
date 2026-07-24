package com.staykonkan.settlement.dto;

import com.staykonkan.settlement.entity.SettlementStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateSettlementStatusRequest {

    @NotNull(message = "Status is required")
    private SettlementStatus status;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;

    @Size(max = 100, message = "Gateway transfer id must not exceed 100 characters")
    private String gatewayTransferId;
}
