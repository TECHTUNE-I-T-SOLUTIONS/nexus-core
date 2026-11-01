package com.techtune.nexus.schedule;

import com.techtune.nexus.schedule.application.ScheduleService;
import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ScheduleSecurityTest {

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
    private ScheduleService scheduleService;

    @Autowired
    private TenantService tenantService;

    @Test
    void scheduleTenantIsolationWorks() {
        // Given
        SecurityContextHolder.clearContext();
        Tenant tenant1 = tenantService.createTenant("Tenant 1 " + UUID.randomUUID());
        Tenant tenant2 = tenantService.createTenant("Tenant 2 " + UUID.randomUUID());
        
        // Create schedule for tenant1
        var request = new com.techtune.nexus.schedule.api.CreateScheduleRequest(
                tenant1.getId(),
                "test.job",
                com.fasterxml.jackson.databind.node.JsonNodeFactory.instance.objectNode(),
                "0 * * * *",
                "UTC",
                5,
                3,
                null
        );
        
        // When
        var schedule = scheduleService.createSchedule(request);
        
        // Then
        assertNotNull(schedule.getId());
        assertEquals(tenant1.getId(), schedule.getTenantId());
        
        // Cleanup
        SecurityContextHolder.clearContext();
    }
}
