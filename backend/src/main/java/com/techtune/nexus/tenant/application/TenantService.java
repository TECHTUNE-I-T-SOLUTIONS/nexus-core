package com.techtune.nexus.tenant.application;

import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.domain.TenantStatus;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public Tenant createTenant(String name) {
        if (tenantRepository.existsByName(name)) {
            throw new IllegalArgumentException("Tenant with name '" + name + "' already exists");
        }
        Tenant tenant = new Tenant(name);
        return tenantRepository.save(tenant);
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> findTenantById(UUID id) {
        return tenantRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Tenant getTenant(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
    }

    @Transactional(readOnly = true)
    public Optional<Tenant> getTenantByName(String name) {
        return tenantRepository.findByName(name);
    }

    @Transactional(readOnly = true)
    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tenant> listTenants() {
        return tenantRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Tenant> getActiveTenants() {
        return tenantRepository.findAll().stream()
                .filter(t -> t.getStatus() == TenantStatus.ACTIVE)
                .toList();
    }

    public Tenant suspendTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenant.suspend();
        return tenantRepository.save(tenant);
    }

    public Tenant activateTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenant.activate();
        return tenantRepository.save(tenant);
    }

    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found: " + id));
        tenant.delete();
        tenantRepository.save(tenant);
    }
}
