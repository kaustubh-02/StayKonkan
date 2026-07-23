package com.staykonkan.refund.service.impl;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.entity.BookingStatus;
import com.staykonkan.booking.repository.BookingRepository;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.payment.gateway.GatewayRefundResult;
import com.staykonkan.payment.gateway.PaymentGatewayService;
import com.staykonkan.payment.repository.PaymentRepository;
import com.staykonkan.refund.dto.RefundRequest;
import com.staykonkan.refund.dto.RefundResponse;
import com.staykonkan.refund.entity.Refund;
import com.staykonkan.refund.entity.RefundAuditAction;
import com.staykonkan.refund.entity.RefundStatus;
import com.staykonkan.refund.mapper.RefundMapper;
import com.staykonkan.refund.repository.RefundRepository;
import com.staykonkan.refund.service.RefundService;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Deliberately NOT @Transactional at the class/method level — every
 * meaningful write goes through RefundAuditRecorder (REQUIRES_NEW), so
 * the REQUESTED row and its audit trail survive regardless of what the
 * gateway call does next. Same architecture as WebhookServiceImpl
 * (Module 10B).
 */
@Service
@RequiredArgsConstructor
public class RefundServiceImpl implements RefundService {

    private final RefundRepository refundRepository;
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final RefundMapper refundMapper;
    private final RefundAuditRecorder auditRecorder;
    private final PaymentGatewayService paymentGatewayService;

    @Override
    public RefundResponse requestRefund(Long bookingId, RefundRequest request) {

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        // Only the guest who made the booking (or an admin acting on
        // their behalf) may request a refund — the property owner does
        // not control the guest's money and cannot trigger this.
        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        if (!isGuest && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the guest who made this booking (or an admin) can request a refund");
        }

        if (booking.getStatus() != BookingStatus.CANCELLED) {
            throw new ValidationException("Refunds can only be requested for a cancelled booking");
        }

        List<Payment> payments = paymentRepository.findByBookingOrderByCreatedAtDesc(booking);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("No payment found for this booking");
        }
        Payment payment = payments.get(0);

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ValidationException(
                    "No successful payment found for this booking to refund (payment status: "
                            + payment.getPaymentStatus() + ")");
        }

        // Duplicate-refund prevention: block while one is in flight or
        // already succeeded, but allow a fresh request after a FAILED or
        // CANCELLED attempt — see Refund entity Javadoc for why this is
        // an application-level check rather than a DB unique constraint.
        if (refundRepository.existsByPaymentAndStatusIn(
                payment, List.of(RefundStatus.REQUESTED, RefundStatus.PROCESSING, RefundStatus.SUCCESS))) {
            throw new DuplicateResourceException(
                    "A refund has already been requested or completed for this booking's payment");
        }

        // Full refund only — the entire amount the guest was actually
        // charged. This can never exceed the payment amount by
        // construction, since it IS the payment amount.
        BigDecimal refundAmount = payment.getBookingAmount();

        String reason = (request != null && request.getReason() != null && !request.getReason().isBlank())
                ? request.getReason()
                : "Booking cancelled";

        Refund refund = Refund.builder()
                .payment(payment)
                .booking(booking)
                .user(currentUser)
                .refundReference(generateRefundReference())
                .refundAmount(refundAmount)
                .refundReason(reason)
                .status(RefundStatus.REQUESTED)
                .requestedAt(Instant.now())
                .build();

        // Durably committed before the gateway call, so a subsequent
        // gateway failure still leaves a permanent record of the request.
        refund = auditRecorder.persistNewRefund(refund);
        auditRecorder.audit(refund.getId(), RefundAuditAction.REFUND_REQUESTED,
                "Refund requested for booking " + booking.getBookingCode());

        try {
            GatewayRefundResult result = paymentGatewayService.initiateRefund(
                    payment.getGatewayPaymentId(), refundAmount, refund.getRefundReference());

            boolean immediatelyProcessed = "processed".equalsIgnoreCase(result.getGatewayStatus());

            if (immediatelyProcessed) {
                auditRecorder.updateOutcome(
                        refund.getId(), result.getGatewayRefundId(), RefundStatus.SUCCESS, Instant.now());
                auditRecorder.audit(refund.getId(), RefundAuditAction.REFUND_COMPLETED,
                        "Refund completed at the gateway");
            } else {
                auditRecorder.updateOutcome(
                        refund.getId(), result.getGatewayRefundId(), RefundStatus.PROCESSING, null);
                auditRecorder.audit(refund.getId(), RefundAuditAction.REFUND_APPROVED,
                        "Refund submitted to gateway, awaiting completion (gateway status: "
                                + result.getGatewayStatus() + ")");
            }

        } catch (RuntimeException e) {
            auditRecorder.updateOutcome(refund.getId(), null, RefundStatus.FAILED, Instant.now());
            auditRecorder.audit(refund.getId(), RefundAuditAction.REFUND_FAILED,
                    "Gateway refund initiation failed: " + e.getMessage());
            throw e;
        }

        final Long refundId = refund.getId();

Refund finalState = refundRepository.findById(refundId)
        .orElseThrow(() -> ResourceNotFoundException.of("Refund", refundId));

return refundMapper.toResponse(finalState);
    }

    @Override
    public RefundResponse getRefundByBooking(Long bookingId) {

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isPropertyOwner = booking.getProperty().getOwner().getId().equals(currentUser.getId());

        if (!isGuest && !isPropertyOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to view refunds for this booking");
        }

        List<Refund> refunds = refundRepository.findByBookingOrderByCreatedAtDesc(booking);
        if (refunds.isEmpty()) {
            throw new ResourceNotFoundException("No refund found for this booking");
        }

        return refundMapper.toResponse(refunds.get(0));
    }

    @Override
    public PageResponseDTO<RefundResponse> getAllRefunds(int page, int size) {

        User currentUser = getCurrentUser();
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can list all refunds");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Refund> refundPage = refundRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponseDTO.from(refundPage.map(refundMapper::toResponse));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private String generateRefundReference() {
        return "RF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
