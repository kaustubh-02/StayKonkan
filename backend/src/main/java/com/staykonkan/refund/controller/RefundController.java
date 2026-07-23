package com.staykonkan.refund.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.refund.dto.RefundRequest;
import com.staykonkan.refund.dto.RefundResponse;
import com.staykonkan.refund.service.RefundService;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * All authorization is data-dependent and enforced inside
 * RefundServiceImpl (who made the booking, who owns the property, who
 * is admin) — same pattern used by every other module's controllers
 * (Booking, Review, Wishlist, PropertyImage, Amenity, Availability,
 * Payment, Webhook).
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/payments")
@Tag(name = "Refunds", description = "Refund requests for cancelled, paid bookings (Module 10C)")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    @PostMapping("/refund/{bookingId}")
    @Operation(summary = "Request a refund for a cancelled booking",
            description = "Only the booking's guest (or an admin) may request this. The booking must already " +
                    "be CANCELLED and have a SUCCESS payment. Always refunds the full payment amount.")
    public ApiResponse<RefundResponse> requestRefund(
            @PathVariable Long bookingId,
            @Valid @RequestBody(required = false) RefundRequest request) {
        return ApiResponse.ok(refundService.requestRefund(bookingId, request), "Refund requested successfully");
    }

    @GetMapping("/refund/{bookingId}")
    @Operation(summary = "Get refund details for a booking",
            description = "Allowed for the booking's guest, the property owner, or an admin")
    public ApiResponse<RefundResponse> getRefundByBooking(@PathVariable Long bookingId) {
        return ApiResponse.ok(refundService.getRefundByBooking(bookingId));
    }

    @GetMapping("/refunds")
    @Operation(summary = "List all refunds (Admin only)")
    public ApiResponse<PageResponseDTO<RefundResponse>> getAllRefunds(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(refundService.getAllRefunds(page, size));
    }
}
