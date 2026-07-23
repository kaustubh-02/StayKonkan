package com.staykonkan.payment.repository;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.payment.entity.PaymentStatus;
import com.staykonkan.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByBookingOrderByCreatedAtDesc(Booking booking);

    Optional<Payment> findByGatewayOrderId(String gatewayOrderId);

    boolean existsByBookingAndPaymentStatusIn(Booking booking, List<PaymentStatus> statuses);

    Page<Payment> findByUser(User user, Pageable pageable);

    // Nested-property traversal (property.owner) — same pattern as
    // BookingRepository.findByPropertyOwner.
    Page<Payment> findByPropertyOwner(User owner, Pageable pageable);

    List<Payment> findByUserAndPaymentStatus(User user, PaymentStatus status);

    List<Payment> findByPropertyOwnerAndPaymentStatus(User owner, PaymentStatus status);

    List<Payment> findByPaymentStatus(PaymentStatus status);
}
