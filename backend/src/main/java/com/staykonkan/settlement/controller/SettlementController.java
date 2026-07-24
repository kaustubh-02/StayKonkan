package com.staykonkan.settlement.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.response.ApiResponse;
import com.staykonkan.settlement.dto.SettlementResponse;
import com.staykonkan.settlement.dto.SettlementSummaryResponse;
import com.staykonkan.settlement.dto.UpdateSettlementStatusRequest;
import com.staykonkan.settlement.service.SettlementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin-only actions (create, status update, list-all) are additionally
 * gated by @PreAuthorize("hasRole('ADMIN')") — same pattern as
 * AmenityController (Module 8). Owner-scoped read access (getById,
 * getMySettlements, getSettlementSummary) is data-dependent and
 * enforced inside SettlementServiceImpl instead, matching how every
 * other module's per-record checks work.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/settlements")
@Tag(name = "Settlements", description = "Owner payout tracking and commission settlement (Module 10D)")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/payment/{paymentId}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a settlement for a successful payment (Admin only)",
            description = "Snapshots the owner payout amount already computed on the Payment record " +
                    "(Module 10A) — commission is never recalculated here.")
    public ApiResponse<SettlementResponse> createSettlement(@PathVariable Long paymentId) {
        return ApiResponse.ok(settlementService.createSettlement(paymentId), "Settlement created successfully");
    }

    @PutMapping("/{settlementId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Update a settlement's status (Admin only)",
            description = "COMPLETED is terminal and cannot be changed further")
    public ApiResponse<SettlementResponse> updateStatus(
            @PathVariable Long settlementId,
            @Valid @RequestBody UpdateSettlementStatusRequest request) {
        return ApiResponse.ok(settlementService.updateSettlementStatus(settlementId, request),
                "Settlement status updated successfully");
    }

    @GetMapping("/{settlementId}")
    @Operation(summary = "Get a settlement by id",
            description = "Allowed for the settlement's owner or an admin")
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable Long settlementId) {
        return ApiResponse.ok(settlementService.getSettlementById(settlementId));
    }

    @GetMapping("/my")
    @Operation(summary = "List the current owner's settlements")
    public ApiResponse<PageResponseDTO<SettlementResponse>> getMySettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(settlementService.getMySettlements(page, size));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all settlements (Admin only)")
    public ApiResponse<PageResponseDTO<SettlementResponse>> getAllSettlements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(settlementService.getAllSettlements(page, size));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get a settlement summary scoped to the caller",
            description = "OWNER: own totals. ADMIN: platform-wide totals. Totals cover COMPLETED settlements only.")
    public ApiResponse<SettlementSummaryResponse> getSummary() {
        return ApiResponse.ok(settlementService.getSettlementSummary());
    }
}
