package com.techtune.nexus.tenant.api;

import com.techtune.nexus.tenant.domain.TenantStatus;

import java.time.Instant;
import java.util.UUID;

public record TenantResponse(
        UUID id,
        String name,
        TenantStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static TenantResponse from(com.techtune.nexus.tenant.domain.Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
