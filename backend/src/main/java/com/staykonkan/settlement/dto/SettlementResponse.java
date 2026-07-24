package com.staykonkan.settlement.dto;

import com.staykonkan.settlement.entity.SettlementStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Builder
public class SettlementResponse {

    private Long id;

    private String settlementReference;

    private Long paymentId;

    private Long bookingId;

    private String bookingCode;

    private Long propertyId;

    private String propertyTitle;

    private Long ownerId;

    private String ownerName;

    private BigDecimal bookingAmount;

    private BigDecimal platformCommissionPercentage;

    private BigDecimal platformCommissionAmount;

    private BigDecimal settlementAmount;

    private String currency;

    private SettlementStatus status;

    private String gatewayTransferId;

    private String notes;

    private Instant initiatedAt;

    private Instant completedAt;

    private Instant createdAt;

    private Instant updatedAt;
}
