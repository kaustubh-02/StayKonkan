package com.staykonkan.settlement.service;

import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.settlement.dto.SettlementResponse;
import com.staykonkan.settlement.dto.SettlementSummaryResponse;
import com.staykonkan.settlement.dto.UpdateSettlementStatusRequest;

public interface SettlementService {

    SettlementResponse createSettlement(Long paymentId);

    SettlementResponse updateSettlementStatus(Long settlementId, UpdateSettlementStatusRequest request);

    SettlementResponse getSettlementById(Long settlementId);

    PageResponseDTO<SettlementResponse> getMySettlements(int page, int size);

    PageResponseDTO<SettlementResponse> getAllSettlements(int page, int size);

    SettlementSummaryResponse getSettlementSummary();
}
