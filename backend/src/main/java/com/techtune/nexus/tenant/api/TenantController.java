package com.techtune.nexus.tenant.api;

import com.techtune.nexus.tenant.application.TenantService;
import com.techtune.nexus.tenant.domain.Tenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
@Tag(name = "Tenants", description = "Tenant management endpoints")
public class TenantController {

    private final TenantService tenantService;

    public TenantController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @PostMapping
    @Operation(summary = "Create a new tenant")
    public ResponseEntity<TenantResponse> createTenant(@Valid @RequestBody CreateTenantRequest request) {
        Tenant tenant = tenantService.createTenant(request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get tenant by ID")
    public ResponseEntity<TenantResponse> getTenant(@PathVariable UUID id) {
        Tenant tenant = tenantService.getTenant(id);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @GetMapping
    @Operation(summary = "List all tenants")
    public ResponseEntity<List<TenantResponse>> listTenants() {
        List<Tenant> tenants = tenantService.listTenants();
        return ResponseEntity.ok(tenants.stream().map(TenantResponse::from).toList());
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend a tenant")
    public ResponseEntity<TenantResponse> suspendTenant(@PathVariable UUID id) {
        Tenant tenant = tenantService.suspendTenant(id);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @PostMapping("/{id}/activate")
    @Operation(summary = "Activate a tenant")
    public ResponseEntity<TenantResponse> activateTenant(@PathVariable UUID id) {
        Tenant tenant = tenantService.activateTenant(id);
        return ResponseEntity.ok(TenantResponse.from(tenant));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a tenant")
    public ResponseEntity<Void> deleteTenant(@PathVariable UUID id) {
        tenantService.deleteTenant(id);
        return ResponseEntity.noContent().build();
    }
}
