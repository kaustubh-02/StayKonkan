package com.staykonkan.webhook.repository;

import com.staykonkan.webhook.entity.WebhookAuditLog;
import com.staykonkan.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WebhookAuditLogRepository extends JpaRepository<WebhookAuditLog, Long> {

    List<WebhookAuditLog> findByWebhookEventOrderByCreatedAtAsc(WebhookEvent webhookEvent);
}
