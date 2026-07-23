package com.staykonkan.refund.repository;

import com.staykonkan.refund.entity.Refund;
import com.staykonkan.refund.entity.RefundAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundAuditLogRepository extends JpaRepository<RefundAuditLog, Long> {

    List<RefundAuditLog> findByRefundOrderByCreatedAtAsc(Refund refund);
}
