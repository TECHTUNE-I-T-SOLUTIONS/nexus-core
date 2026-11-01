package com.techtune.nexus.security;

import com.techtune.nexus.apikey.application.ApiKeyService;
import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApiKeySecurityTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    );

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ApiKeyService apiKeyService;

    @Autowired
    private TenantService tenantService;

    @Test
    void shouldStoreOnlyPublicKeyIdInAuthenticationContext() {
        // Given
        Tenant tenant = tenantService.createTenant("Test Tenant " + UUID.randomUUID());
        var result = apiKeyService.generateApiKey(tenant.getId(), Set.of("jobs:read", "jobs:write"), 30);
        
        // When
        ApiKeyAuthentication auth = new ApiKeyAuthentication(result.publicKeyId(), result.publicKeyId(), Set.of("jobs:read", "jobs:write"));
        auth.setAuthenticated(true);
        
        // Then
        assertEquals(result.publicKeyId(), auth.getTenantId());
        assertEquals(result.publicKeyId(), auth.getPublicKeyId());
        assertEquals(result.publicKeyId(), auth.getCredentials()); // Should be public key ID, not secret
        assertNotEquals(result.secret(), auth.getCredentials()); // Secret should NOT be stored
    }

    @Test
    void shouldValidateInvalidSecret() {
        // Given
        Tenant tenant = tenantService.createTenant("Test Tenant " + UUID.randomUUID());
        var result = apiKeyService.generateApiKey(tenant.getId(), Set.of("jobs:read"), 30);
        
        // When
        var validation = apiKeyService.validateApiKey(result.publicKeyId(), "invalid-secret");
        
        // Then
        assertFalse(validation.valid());
    }
}
