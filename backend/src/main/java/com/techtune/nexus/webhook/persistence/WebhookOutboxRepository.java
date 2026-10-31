package com.techtune.nexus.webhook.persistence;

import com.techtune.nexus.webhook.domain.WebhookOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookOutboxRepository extends JpaRepository<WebhookOutbox, UUID> {

    @Query("SELECT w FROM WebhookOutbox w WHERE w.deliveredAt IS NULL AND w.nextRetryAt <= :now ORDER BY w.nextRetryAt ASC")
    List<WebhookOutbox> findPendingWebhooks(@Param("now") Instant now);

    @Query("SELECT w FROM WebhookOutbox w WHERE w.deliveredAt IS NULL AND w.nextRetryAt <= :now AND (w.leaseExpiresAt IS NULL OR w.leaseExpiresAt < :now) ORDER BY w.nextRetryAt ASC LIMIT :batchSize")
    List<WebhookOutbox> findClaimableWebhooks(@Param("now") Instant now, @Param("batchSize") int batchSize);

    List<WebhookOutbox> findByJobId(UUID jobId);
}
