package com.techtune.nexus.apikey.persistence;

import com.techtune.nexus.apikey.domain.ApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByPublicKeyIdAndIsActiveTrue(UUID publicKeyId);

    Optional<ApiKey> findByTenantId(UUID tenantId);

    Set<ApiKey> findByExpiresAtBefore(Instant expiresAt);
}
