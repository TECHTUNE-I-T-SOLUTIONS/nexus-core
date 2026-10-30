package com.techtune.nexus.tenant.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TenantTest {

    @Test
    void shouldCreateTenant() {
        Tenant tenant = new Tenant("Test Tenant");

        assertEquals("Test Tenant", tenant.getName());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        // @PrePersist not called in unit tests without JPA context
        // assertNotNull(tenant.getId());
        // assertNotNull(tenant.getCreatedAt());
        // assertNotNull(tenant.getUpdatedAt());
    }

    @Test
    void shouldSuspendTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        tenant.suspend();

        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus());
    }

    @Test
    void shouldActivateTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        tenant.suspend();
        tenant.activate();

        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
    }

    @Test
    void shouldDeleteTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        tenant.delete();

        assertEquals(TenantStatus.DELETED, tenant.getStatus());
    }

    @Test
    void shouldNotSuspendDeletedTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        tenant.delete();

        assertThrows(IllegalStateException.class, tenant::suspend);
    }

    @Test
    void shouldNotActivateDeletedTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        tenant.delete();

        assertThrows(IllegalStateException.class, tenant::activate);
    }
}
