package com.staykonkan.refund.repository;

import com.staykonkan.booking.entity.Booking;
import com.staykonkan.payment.entity.Payment;
import com.staykonkan.refund.entity.Refund;
import com.staykonkan.refund.entity.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {

    List<Refund> findByPaymentOrderByCreatedAtDesc(Payment payment);

    List<Refund> findByBookingOrderByCreatedAtDesc(Booking booking);

    boolean existsByPaymentAndStatusIn(Payment payment, List<RefundStatus> statuses);

    Optional<Refund> findByRefundReference(String refundReference);

    Page<Refund> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
