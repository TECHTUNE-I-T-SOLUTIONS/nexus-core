package com.techtune.nexus.apikey.application;

import com.techtune.nexus.apikey.domain.ApiKey;
import com.techtune.nexus.apikey.persistence.ApiKeyRepository;
import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Set;
import java.util.UUID;

@Service
public class ApiKeyService {

    private final ApiKeyRepository apiKeyRepository;
    private final TenantRepository tenantRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;

    public ApiKeyService(ApiKeyRepository apiKeyRepository, TenantRepository tenantRepository) {
        this.apiKeyRepository = apiKeyRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        this.secureRandom = new SecureRandom();
    }

    @Transactional
    public ApiKeyGenerationResult generateApiKey(UUID tenantId, Set<String> scopes, Integer expiresInDays) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Tenant not found"));

        UUID publicKeyId = UUID.randomUUID();
        String secret = generateSecret();
        String hashedSecret = passwordEncoder.encode(secret);
        
        Instant expiresAt = expiresInDays != null 
                ? Instant.now().plus(expiresInDays, ChronoUnit.DAYS) 
                : null;

        ApiKey apiKey = new ApiKey(tenant, publicKeyId, hashedSecret, scopes, expiresAt);
        apiKeyRepository.save(apiKey);

        return new ApiKeyGenerationResult(publicKeyId, secret, expiresAt);
    }

    @Transactional
    public ApiKeyValidationResult validateApiKey(UUID publicKeyId, String secret) {
        ApiKey apiKey = apiKeyRepository.findByPublicKeyIdAndIsActiveTrue(publicKeyId)
                .orElse(null);

        if (apiKey == null) {
            return new ApiKeyValidationResult(false, null, null, "Invalid API key");
        }

        if (!apiKey.isValid()) {
            return new ApiKeyValidationResult(false, null, null, "API key expired or inactive");
        }

        if (!passwordEncoder.matches(secret, apiKey.getHashedSecret())) {
            return new ApiKeyValidationResult(false, null, null, "Invalid API key secret");
        }

        // Update last used timestamp
        apiKey.setLastUsedAt(Instant.now());
        apiKeyRepository.save(apiKey);

        return new ApiKeyValidationResult(true, apiKey.getTenant().getId(), apiKey.getScopes(), null);
    }

    @Transactional
    public void revokeApiKey(UUID apiKeyId) {
        ApiKey apiKey = apiKeyRepository.findById(apiKeyId)
                .orElseThrow(() -> new IllegalArgumentException("API key not found"));
        apiKey.setActive(false);
        apiKeyRepository.save(apiKey);
    }

    private String generateSecret() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    public record ApiKeyGenerationResult(UUID publicKeyId, String secret, Instant expiresAt) {}
    public record ApiKeyValidationResult(boolean valid, UUID tenantId, Set<String> scopes, String error) {}
}
