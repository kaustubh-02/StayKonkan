package com.staykonkan.settlement.service.impl;

import com.staykonkan.exception.DuplicateResourceException;
import com.staykonkan.exception.ForbiddenException;
import com.staykonkan.exception.ResourceNotFoundException;
import com.staykonkan.exception.ValidationException;
import com.staykonkan.dto.PageResponseDTO;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.payment.repository.PaymentRepository;
import com.staykonkan.security.SecurityUserPrincipal;
import com.staykonkan.settlement.dto.SettlementResponse;
import com.staykonkan.settlement.dto.SettlementSummaryResponse;
import com.staykonkan.settlement.dto.UpdateSettlementStatusRequest;
import com.staykonkan.settlement.entity.Settlement;
import com.staykonkan.settlement.entity.SettlementStatus;
import com.staykonkan.settlement.mapper.SettlementMapper;
import com.staykonkan.settlement.repository.SettlementRepository;
import com.staykonkan.settlement.service.SettlementService;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

/**
 * @Transactional at the class level (unlike Refund/Webhook): unlike
 * those flows, nothing here calls out to an external gateway, so there
 * is no "external side effect already happened, must survive a later
 * failure" concern requiring a REQUIRES_NEW split — plain atomicity is
 * both correct and sufficient.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class SettlementServiceImpl implements SettlementService {

    private final SettlementRepository settlementRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final SettlementMapper settlementMapper;

    @Override
    public SettlementResponse createSettlement(Long paymentId) {

        User currentUser = getCurrentUser();
        assertAdmin(currentUser);

        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ResourceNotFoundException.of("Payment", paymentId));

        if (payment.getPaymentStatus() != PaymentStatus.SUCCESS) {
            throw new ValidationException(
                    "Only a SUCCESS payment can be settled (current status: " + payment.getPaymentStatus() + ")");
        }

        // Duplicate-settlement prevention: block while one is in flight
        // or already completed, but allow a fresh attempt after a FAILED
        // one — same application-level reasoning Module 10C used for
        // refunds rather than a blanket DB unique constraint.
        if (settlementRepository.existsByPaymentAndStatusIn(
                payment, List.of(SettlementStatus.PENDING, SettlementStatus.PROCESSING, SettlementStatus.COMPLETED))) {
            throw new DuplicateResourceException("A settlement already exists or is in progress for this payment");
        }

        // Commission calculation: snapshotted from Payment (Module 10A),
        // the single source of truth for that math — never recomputed
        // here, per "never duplicate payment logic".
        Settlement settlement = Settlement.builder()
                .payment(payment)
                .booking(payment.getBooking())
                .property(payment.getProperty())
                .owner(payment.getProperty().getOwner())
                .settlementReference(generateSettlementReference())
                .bookingAmount(payment.getBookingAmount())
                .platformCommissionPercentage(payment.getPlatformCommissionPercentage())
                .platformCommissionAmount(payment.getPlatformCommissionAmount())
                .settlementAmount(payment.getOwnerAmount())
                .currency(payment.getCurrency())
                .status(SettlementStatus.PENDING)
                .initiatedAt(Instant.now())
                .build();

        settlement = settlementRepository.save(settlement);

        return settlementMapper.toResponse(settlement);
    }

    @Override
    public SettlementResponse updateSettlementStatus(Long settlementId, UpdateSettlementStatusRequest request) {

        User currentUser = getCurrentUser();
        assertAdmin(currentUser);

        Settlement settlement = findByIdOrThrow(settlementId);

        validateStatusTransition(settlement.getStatus(), request.getStatus());

        settlement.setStatus(request.getStatus());
        if (request.getNotes() != null) {
            settlement.setNotes(request.getNotes());
        }
        if (request.getGatewayTransferId() != null) {
            settlement.setGatewayTransferId(request.getGatewayTransferId());
        }
        if (request.getStatus() == SettlementStatus.COMPLETED) {
            settlement.setCompletedAt(Instant.now());
        }

        return settlementMapper.toResponse(settlement);
    }

    @Override
    public SettlementResponse getSettlementById(Long settlementId) {

        User currentUser = getCurrentUser();
        Settlement settlement = findByIdOrThrow(settlementId);

        boolean isOwner = settlement.getOwner().getId().equals(currentUser.getId());
        if (!isOwner && currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("You do not have permission to view this settlement");
        }

        return settlementMapper.toResponse(settlement);
    }

    @Override
    public PageResponseDTO<SettlementResponse> getMySettlements(int page, int size) {

        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlementPage = settlementRepository.findByOwner(currentUser, pageable);

        return PageResponseDTO.from(settlementPage.map(settlementMapper::toResponse));
    }

    @Override
    public PageResponseDTO<SettlementResponse> getAllSettlements(int page, int size) {

        User currentUser = getCurrentUser();
        assertAdmin(currentUser);

        Pageable pageable = PageRequest.of(page, size);
        Page<Settlement> settlementPage = settlementRepository.findAllByOrderByCreatedAtDesc(pageable);

        return PageResponseDTO.from(settlementPage.map(settlementMapper::toResponse));
    }

    @Override
    public SettlementSummaryResponse getSettlementSummary() {

        User currentUser = getCurrentUser();
        boolean isAdmin = currentUser.getRole() == Role.ADMIN;

        List<Settlement> completed = isAdmin
                ? settlementRepository.findByStatus(SettlementStatus.COMPLETED)
                : settlementRepository.findByOwnerAndStatus(currentUser, SettlementStatus.COMPLETED);

        List<Settlement> pending = isAdmin
                ? settlementRepository.findByStatus(SettlementStatus.PENDING)
                : settlementRepository.findByOwnerAndStatus(currentUser, SettlementStatus.PENDING);
        List<Settlement> processing = isAdmin
                ? settlementRepository.findByStatus(SettlementStatus.PROCESSING)
                : settlementRepository.findByOwnerAndStatus(currentUser, SettlementStatus.PROCESSING);

        BigDecimal totalSettled = sum(completed, Settlement::getSettlementAmount);
        BigDecimal totalCommission = sum(completed, Settlement::getPlatformCommissionAmount);
        BigDecimal pendingAmount = sum(pending, Settlement::getSettlementAmount)
                .add(sum(processing, Settlement::getSettlementAmount));

        String currency = !completed.isEmpty() ? completed.get(0).getCurrency()
                : !pending.isEmpty() ? pending.get(0).getCurrency()
                : "INR";

        return SettlementSummaryResponse.builder()
                .completedSettlementCount(completed.size())
                .totalSettledAmount(totalSettled)
                .totalPlatformCommission(totalCommission)
                .pendingSettlementAmount(pendingAmount)
                .currency(currency)
                .build();
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Settlement findByIdOrThrow(Long settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> ResourceNotFoundException.of("Settlement", settlementId));
    }

    private void assertAdmin(User currentUser) {
        if (currentUser.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only an admin can perform this action");
        }
    }

    /** COMPLETED is terminal; everything else may move freely between PENDING/PROCESSING/ON_HOLD/FAILED. */
    private void validateStatusTransition(SettlementStatus current, SettlementStatus target) {
        if (current == SettlementStatus.COMPLETED) {
            throw new ValidationException("A COMPLETED settlement cannot be changed further");
        }
        if (current == target) {
            throw new ValidationException("Settlement is already in status " + current);
        }
    }

    private BigDecimal sum(List<Settlement> settlements, Function<Settlement, BigDecimal> extractor) {
        return settlements.stream()
                .map(extractor)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String generateSettlementReference() {
        return "ST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        SecurityUserPrincipal principal = (SecurityUserPrincipal) authentication.getPrincipal();
        return userRepository.findById(principal.getUserId())
                .orElseThrow(() -> ResourceNotFoundException.of("User", principal.getUserId()));
    }
}
