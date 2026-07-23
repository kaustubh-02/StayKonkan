package com.staykonkan.payment.controller;

import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.payment.dto.CreatePaymentRequest;
import com.staykonkan.payment.dto.PaymentResponse;
import com.staykonkan.payment.dto.PaymentSummaryResponse;
import com.staykonkan.payment.dto.PaymentVerificationRequest;
import com.staykonkan.payment.service.PaymentService;
import com.staykonkan.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/**
 * Every endpoint here scopes to the caller (own payments for USER, own
 * properties' payments for OWNER, everything for ADMIN) — enforced
 * inside PaymentServiceImpl, same data-dependent-authorization pattern
 * used by every other module's controllers.
 */
@RestController
@RequestMapping(AppConstants.API_V1 + "/payments")
@Tag(name = "Payments", description = "Razorpay-backed booking payments (Module 10A — order creation and verification)")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    @Operation(summary = "Create a Razorpay order for a booking",
            description = "Only the booking's own guest may call this, and only while the booking is PENDING. " +
                    "The amount is always taken from the booking server-side, never from the request.")
    public ApiResponse<PaymentResponse> createOrder(@Valid @RequestBody CreatePaymentRequest request) {
        return ApiResponse.ok(paymentService.createOrder(request), "Payment order created successfully");
    }

    @PostMapping("/verify")
    @Operation(summary = "Verify a completed Razorpay payment",
            description = "Cryptographically verifies the gateway signature — this is the only trusted signal " +
                    "of success. On success, confirms the booking and updates the availability calendar.")
    public ApiResponse<PaymentResponse> verifyPayment(@Valid @RequestBody PaymentVerificationRequest request) {
        return ApiResponse.ok(paymentService.verifyPayment(request), "Payment verified and booking confirmed");
    }

    @GetMapping("/history")
    @Operation(summary = "Get the caller's payment history",
            description = "USER: own payments. OWNER: payments for own properties. ADMIN: all payments.")
    public ApiResponse<PageResponseDTO<PaymentResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ApiResponse.ok(paymentService.getPaymentHistory(page, size));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get a payment summary scoped to the caller",
            description = "Totals cover SUCCESS payments only. Scope follows the same USER/OWNER/ADMIN rule as /history.")
    public ApiResponse<PaymentSummaryResponse> getSummary() {
        return ApiResponse.ok(paymentService.getPaymentSummary());
    }

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Get the most recent payment for a booking",
            description = "Allowed for the booking's guest, the property owner, or an admin")
    public ApiResponse<PaymentResponse> getPaymentByBooking(@PathVariable Long bookingId) {
        return ApiResponse.ok(paymentService.getPaymentByBooking(bookingId));
    }
}
