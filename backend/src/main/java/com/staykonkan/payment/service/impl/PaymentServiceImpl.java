package com.staykonkan.payment.service.impl;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.booking.entity.BookingStatus;
import com.staykonkan.booking.repository.BookingRepository;
import com.staykonkan.booking.service.BookingService;
import com.staykonkan.config.PaymentConfig;
import com.staykonkan.constant.AppConstants;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.payment.dto.CreatePaymentRequest;
import com.staykonkan.payment.dto.PaymentResponse;
import com.staykonkan.payment.dto.PaymentSummaryResponse;
import com.staykonkan.payment.dto.PaymentVerificationRequest;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentGateway;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.payment.gateway.GatewayOrderResult;
import com.staykonkan.payment.gateway.PaymentGatewayService;
import com.staykonkan.payment.mapper.PaymentMapper;
import com.staykonkan.payment.repository.PaymentRepository;
import com.staykonkan.payment.service.PaymentService;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.user.entity.Role;
import com.staykonkan.user.entity.User;
import com.staykonkan.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

@Service
@Transactional
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentGatewayService paymentGatewayService;
    private final BookingService bookingService;
    private final PaymentStatusUpdater paymentStatusUpdater;
    private final PaymentConfig.PaymentProperties paymentProperties;

    @Override
    public PaymentResponse createOrder(CreatePaymentRequest request) {

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", request.getBookingId()));

        if (!booking.getGuest().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only pay for your own bookings");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new ValidationException(
                    "Only a PENDING booking can be paid for (current status: " + booking.getStatus() + ")");
        }

        // Duplicate-order prevention: don't let a client spam this
        // endpoint and rack up multiple live Razorpay orders for the
        // same booking.
        if (paymentRepository.existsByBookingAndPaymentStatusIn(booking, List.of(PaymentStatus.ORDER_CREATED))) {
            throw new DuplicateResourceException(
                    "A payment order is already in progress for this booking — verify or retry that one");
        }

        BigDecimal bookingAmount = booking.getTotalAmount();
        BigDecimal commissionPercentage = paymentProperties.getPlatformCommissionPercentage();
        BigDecimal commissionAmount = bookingAmount
                .multiply(commissionPercentage)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal ownerAmount = bookingAmount.subtract(commissionAmount);

        String currency = paymentProperties.getDefaultCurrency();

        // Gateway call happens BEFORE any Payment row is persisted — if
        // it fails, there's nothing to roll back and nothing misleading
        // left in the database (see PaymentStatus.PENDING Javadoc).
        GatewayOrderResult gatewayOrder = paymentGatewayService.createOrder(
                bookingAmount, currency, booking.getBookingCode());

        Payment payment = Payment.builder()
                .booking(booking)
                .user(currentUser)
                .property(booking.getProperty())
                .gatewayOrderId(gatewayOrder.getGatewayOrderId())
                .bookingAmount(bookingAmount)
                .platformCommissionPercentage(commissionPercentage)
                .platformCommissionAmount(commissionAmount)
                .ownerAmount(ownerAmount)
                .currency(currency)
                .paymentGateway(PaymentGateway.RAZORPAY)
                .paymentStatus(PaymentStatus.ORDER_CREATED)
                .build();

        payment = paymentRepository.save(payment);

        return paymentMapper.toResponse(payment);
    }

    @Override
    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {

        User currentUser = getCurrentUser();

        Payment payment = paymentRepository.findByGatewayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("No payment order found for this order id"));

        if (!payment.getUser().getId().equals(currentUser.getId())) {
            throw new ForbiddenException("You can only verify your own payments");
        }

        // Idempotency / replay protection: a client retry (or a
        // malicious replay) of an already-verified payment must not be
        // processed twice.
        if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
            throw new DuplicateResourceException("This payment has already been verified");
        }
        if (payment.getPaymentStatus() != PaymentStatus.ORDER_CREATED) {
            throw new ValidationException(
                    "Payment is not in a verifiable state (current status: " + payment.getPaymentStatus() + ")");
        }

        boolean signatureValid = paymentGatewayService.verifySignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!signatureValid) {
            // No later step depends on this write's durability the way
            // SUCCESS does, so a plain in-transaction update is fine here.
            payment.setPaymentStatus(PaymentStatus.FAILED);
            throw new ValidationException("Payment verification failed — invalid signature");
        }

        String paymentMethod = paymentGatewayService.fetchPaymentMethod(request.getRazorpayPaymentId())
                .orElse(null);

        // Durably commits SUCCESS in its own transaction, independent of
        // whatever happens next — see PaymentStatusUpdater Javadoc for why.
        Payment updated = paymentStatusUpdater.markSuccess(
                payment.getId(), request.getRazorpayPaymentId(), request.getRazorpaySignature(), paymentMethod);

        // Booking confirmation + availability locking happens after the
        // payment is already durably SUCCESS. If this throws (e.g. a
        // genuinely rare availability conflict), the exception propagates
        // as-is — the client sees the confirmation failure, but the
        // payment correctly stays recorded as SUCCESS for support/finance
        // to reconcile (future Refunds module), rather than silently
        // reverting a real charge to look like it never happened.
        bookingService.confirmAfterPayment(updated.getBooking().getId());

        return paymentMapper.toResponse(updated);
    }

    @Override
    public PageResponseDTO<PaymentResponse> getPaymentHistory(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, AppConstants.DEFAULT_SORT_FIELD));

        Page<Payment> paymentPage = switch (currentUser.getRole()) {
            case ADMIN -> paymentRepository.findAll(pageable);
            case OWNER -> paymentRepository.findByPropertyOwner(currentUser, pageable);
            case USER -> paymentRepository.findByUser(currentUser, pageable);
        };

        return PageResponseDTO.from(paymentPage.map(paymentMapper::toResponse));
    }

    @Override
    public PaymentSummaryResponse getPaymentSummary() {

        User currentUser = getCurrentUser();

        List<Payment> successfulPayments = switch (currentUser.getRole()) {
            case ADMIN -> paymentRepository.findByPaymentStatus(PaymentStatus.SUCCESS);
            case OWNER -> paymentRepository.findByPropertyOwnerAndPaymentStatus(currentUser, PaymentStatus.SUCCESS);
            case USER -> paymentRepository.findByUserAndPaymentStatus(currentUser, PaymentStatus.SUCCESS);
        };

        BigDecimal totalBookingAmount = sum(successfulPayments, Payment::getBookingAmount);
        BigDecimal totalCommission = sum(successfulPayments, Payment::getPlatformCommissionAmount);
        BigDecimal totalOwnerEarnings = sum(successfulPayments, Payment::getOwnerAmount);

        return PaymentSummaryResponse.builder()
                .successfulPaymentCount(successfulPayments.size())
                .totalBookingAmount(totalBookingAmount)
                .totalPlatformCommission(totalCommission)
                .totalOwnerEarnings(totalOwnerEarnings)
                .currency(paymentProperties.getDefaultCurrency())
                .build();
    }

    @Override
    public PaymentResponse getPaymentByBooking(Long bookingId) {

        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> ResourceNotFoundException.of("Booking", bookingId));

        boolean isGuest = booking.getGuest().getId().equals(currentUser.getId());
        boolean isPropertyOwner = booking.getProperty().getOwner().getId().equals(currentUser.getId());

        if (!isGuest && !isPropertyOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to view payments for this booking");
        }

        List<Payment> payments = paymentRepository.findByBookingOrderByCreatedAtDesc(booking);

        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("No payment found for this booking");
        }

        // Most recent attempt — in the current flow there is normally at
        // most one (duplicate-order-creation is prevented above), but a
        // FAILED verification followed by a fresh order-creation retry
        // can legitimately produce more than one row over time.
        return paymentMapper.toResponse(payments.get(0));
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private BigDecimal sum(List<Payment> payments, Function<Payment, BigDecimal> extractor) {
        return payments.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
