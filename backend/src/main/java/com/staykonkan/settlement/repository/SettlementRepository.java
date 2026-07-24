package com.staykonkan.settlement.repository;

import com.staykonkan.payment.entity.Payment;
import com.staykonkan.settlement.entity.Settlement;
import com.staykonkan.settlement.entity.SettlementStatus;
import com.staykonkan.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByPayment(Payment payment);

    boolean existsByPaymentAndStatusIn(Payment payment, List<SettlementStatus> statuses);

    Page<Settlement> findByOwner(User owner, Pageable pageable);

    Page<Settlement> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Settlement> findByOwnerAndStatus(User owner, SettlementStatus status);

    List<Settlement> findByStatus(SettlementStatus status);
}
