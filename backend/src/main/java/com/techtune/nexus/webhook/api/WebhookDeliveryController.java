package com.techtune.nexus.webhook.api;

import com.techtune.nexus.webhook.application.WebhookDeliveryManagementService;
import com.techtune.nexus.webhook.domain.WebhookOutbox;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/webhook-deliveries")
@Tag(name = "Webhook Deliveries", description = "Webhook delivery management and dead-letter replay")
public class WebhookDeliveryController {

    private final WebhookDeliveryManagementService deliveryManagementService;

    public WebhookDeliveryController(WebhookDeliveryManagementService deliveryManagementService) {
        this.deliveryManagementService = deliveryManagementService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_webhooks:read')")
    @Operation(summary = "List webhook deliveries for a tenant")
    public ResponseEntity<List<WebhookDeliveryResponse>> listDeliveries(
            @RequestParam UUID tenantId,
            @RequestParam(required = false) String deliveryStatus) {
        List<WebhookOutbox> deliveries = deliveryManagementService.listDeliveries(tenantId, deliveryStatus);
        return ResponseEntity.ok(deliveries.stream().map(WebhookDeliveryResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_webhooks:read')")
    @Operation(summary = "Get a webhook delivery")
    public ResponseEntity<WebhookDeliveryResponse> getDelivery(@PathVariable UUID id) {
        WebhookOutbox delivery = deliveryManagementService.getDelivery(id);
        return ResponseEntity.ok(WebhookDeliveryResponse.from(delivery));
    }

    @PostMapping("/{id}/replay")
    @PreAuthorize("hasAuthority('SCOPE_webhooks:write')")
    @Operation(summary = "Replay a dead-lettered webhook delivery")
    public ResponseEntity<WebhookDeliveryResponse> replayDelivery(@PathVariable UUID id, Authentication auth) {
        UUID tenantId = extractTenantId(auth);
        WebhookOutbox delivery = deliveryManagementService.replayDelivery(id, tenantId);
        return ResponseEntity.ok(WebhookDeliveryResponse.from(delivery));
    }

    private UUID extractTenantId(Authentication auth) {
        return UUID.fromString(auth.getName());
    }
}
