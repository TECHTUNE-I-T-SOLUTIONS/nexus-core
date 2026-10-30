package com.techtune.nexus.tenant.persistence;

import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.domain.TenantStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantRepository extends JpaRepository<Tenant, UUID> {

    Optional<Tenant> findByName(String name);

    boolean existsByName(String name);

    boolean existsByStatus(TenantStatus status);
}
