package com.techtune.nexus.webhook.persistence;

import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {

    @Query("SELECT we FROM WebhookEndpoint we WHERE we.tenant.id = :tenantId")
    List<WebhookEndpoint> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT we FROM WebhookEndpoint we WHERE we.tenant.id = :tenantId AND we.enabled = true")
    List<WebhookEndpoint> findEnabledByTenantId(@Param("tenantId") UUID tenantId);
}
