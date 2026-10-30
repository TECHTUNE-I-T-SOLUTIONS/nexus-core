package com.techtune.nexus.tenant.application;

import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.domain.TenantStatus;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @InjectMocks
    private TenantService tenantService;

    @Test
    void shouldCreateTenant() {
        when(tenantRepository.existsByName("Test Tenant")).thenReturn(false);
        when(tenantRepository.save(any(Tenant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Tenant tenant = tenantService.createTenant("Test Tenant");

        assertEquals("Test Tenant", tenant.getName());
        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        verify(tenantRepository).save(any(Tenant.class));
    }

    @Test
    void shouldNotCreateDuplicateTenant() {
        when(tenantRepository.existsByName("Test Tenant")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> tenantService.createTenant("Test Tenant"));
        verify(tenantRepository, never()).save(any());
    }

    @Test
    void shouldGetTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test Tenant");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        Tenant found = tenantService.getTenant(tenantId);

        assertEquals("Test Tenant", found.getName());
    }

    @Test
    void shouldSuspendTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test Tenant");
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.suspendTenant(tenantId);

        assertEquals(TenantStatus.SUSPENDED, tenant.getStatus());
        verify(tenantRepository).save(tenant);
    }

    @Test
    void shouldActivateTenant() {
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = new Tenant("Test Tenant");
        tenant.suspend();
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));
        when(tenantRepository.save(any(Tenant.class))).thenReturn(tenant);

        tenantService.activateTenant(tenantId);

        assertEquals(TenantStatus.ACTIVE, tenant.getStatus());
        verify(tenantRepository).save(tenant);
    }
}
