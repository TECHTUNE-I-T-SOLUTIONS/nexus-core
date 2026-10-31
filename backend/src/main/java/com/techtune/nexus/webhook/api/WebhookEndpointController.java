package com.techtune.nexus.webhook.api;

import com.techtune.nexus.webhook.application.WebhookEndpointService;
import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook-endpoints")
@Tag(name = "Webhook Endpoints", description = "Webhook endpoint management")
public class WebhookEndpointController {

    private final WebhookEndpointService webhookEndpointService;

    public WebhookEndpointController(WebhookEndpointService webhookEndpointService) {
        this.webhookEndpointService = webhookEndpointService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_webhooks:write')")
    @Operation(summary = "Create a webhook endpoint")
    public ResponseEntity<WebhookEndpointResponse> createWebhookEndpoint(@Valid @RequestBody CreateWebhookEndpointRequest request) {
        WebhookEndpoint endpoint = webhookEndpointService.createWebhookEndpoint(
                request.tenantId(),
                request.url(),
                request.secret(),
                request.eventTypes()
        );
        return ResponseEntity.ok(WebhookEndpointResponse.from(endpoint));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_webhooks:read')")
    @Operation(summary = "Get a webhook endpoint")
    public ResponseEntity<WebhookEndpointResponse> getWebhookEndpoint(@PathVariable UUID id) {
        WebhookEndpoint endpoint = webhookEndpointService.getWebhookEndpoint(id);
        return ResponseEntity.ok(WebhookEndpointResponse.from(endpoint));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_webhooks:read')")
    @Operation(summary = "List webhook endpoints")
    public ResponseEntity<List<WebhookEndpointResponse>> listWebhookEndpoints(@RequestParam(required = false) UUID tenantId) {
        List<WebhookEndpoint> endpoints = webhookEndpointService.listWebhookEndpoints(tenantId);
        return ResponseEntity.ok(endpoints.stream().map(WebhookEndpointResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_webhooks:write')")
    @Operation(summary = "Update a webhook endpoint")
    public ResponseEntity<WebhookEndpointResponse> updateWebhookEndpoint(
            @PathVariable UUID id,
            @RequestBody UpdateWebhookEndpointRequest request) {
        WebhookEndpoint endpoint = webhookEndpointService.updateWebhookEndpoint(
                id,
                request.url(),
                request.secret(),
                request.eventTypes(),
                request.enabled()
        );
        return ResponseEntity.ok(WebhookEndpointResponse.from(endpoint));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_webhooks:write')")
    @Operation(summary = "Delete a webhook endpoint")
    public ResponseEntity<Void> deleteWebhookEndpoint(@PathVariable UUID id) {
        webhookEndpointService.deleteWebhookEndpoint(id);
        return ResponseEntity.noContent().build();
    }
}
