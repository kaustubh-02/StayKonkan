package com.staykonkan.webhook.repository;

import com.staykonkan.webhook.entity.WebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {

    Optional<WebhookEvent> findByIdempotencyKey(String idempotencyKey);
}
