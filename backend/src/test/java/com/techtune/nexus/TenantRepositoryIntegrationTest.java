package com.techtune.nexus;

import com.techtune.nexus.tenant.domain.Tenant;
import com.techtune.nexus.tenant.persistence.TenantRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class TenantRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("nexus_core")
            .withUsername("nexus")
            .withPassword("nexus_password");

    @DynamicPropertySource
    static void postgresProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private TenantRepository tenantRepository;

    @AfterEach
    void tearDown() {
        tenantRepository.deleteAll();
    }

    @Test
    void shouldCreateAndRetrieveTenant() {
        Tenant tenant = new Tenant("Test Tenant");
        Tenant saved = tenantRepository.save(tenant);

        assertNotNull(saved.getId());
        assertNotNull(saved.getCreatedAt());
        assertNotNull(saved.getUpdatedAt());

        Optional<Tenant> found = tenantRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("Test Tenant", found.get().getName());
    }

    @Test
    void shouldFindTenantByName() {
        Tenant tenant = new Tenant("Unique Tenant");
        tenantRepository.save(tenant);

        Optional<Tenant> found = tenantRepository.findByName("Unique Tenant");
        assertTrue(found.isPresent());
        assertEquals("Unique Tenant", found.get().getName());
    }

    @Test
    void shouldRejectDuplicateTenantName() {
        Tenant tenant1 = new Tenant("Duplicate Name");
        tenantRepository.save(tenant1);

        Tenant tenant2 = new Tenant("Duplicate Name");
        assertThrows(Exception.class, () -> tenantRepository.save(tenant2));
    }
}
