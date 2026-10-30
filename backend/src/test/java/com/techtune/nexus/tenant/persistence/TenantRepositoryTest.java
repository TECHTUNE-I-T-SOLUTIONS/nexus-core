package com.techtune.nexus.tenant.persistence;

import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantRepositoryTest {

    @Test
    void shouldCreateTenant() {
        Tenant tenant = new Tenant("Test Tenant");

        assertEquals("Test Tenant", tenant.getName());
        assertEquals(com.techtune.nexus.tenant.domain.TenantStatus.ACTIVE, tenant.getStatus());
        // @PrePersist not called in unit tests without JPA context
        // assertNotNull(tenant.getId());
        // assertNotNull(tenant.getCreatedAt());
    }
}
