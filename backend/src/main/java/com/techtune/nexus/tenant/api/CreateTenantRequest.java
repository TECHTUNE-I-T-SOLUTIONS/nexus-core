package com.techtune.nexus.tenant.api;

import jakarta.validation.constraints.NotBlank;

public record CreateTenantRequest(
        @NotBlank(message = "Tenant name is required")
        String name
) {}
