package com.techtune.nexus.webhook.application;

import com.techtune.nexus.webhook.domain.WebhookOutbox;
import com.techtune.nexus.webhook.persistence.WebhookOutboxRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebhookDeliveryManagementServiceTest {

    @Mock
    private WebhookOutboxRepository webhookOutboxRepository;

    private WebhookDeliveryManagementService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new WebhookDeliveryManagementService(webhookOutboxRepository);
    }

    @Test
    void testReplayDeadLetteredDelivery() {
        UUID deliveryId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        WebhookOutbox delivery = mock(WebhookOutbox.class);
        when(delivery.getDeliveryStatus()).thenReturn("DEAD_LETTER");
        when(delivery.getTenant()).thenReturn(mock(com.techtune.nexus.tenant.domain.Tenant.class));
        when(delivery.getTenant().getId()).thenReturn(tenantId);

        when(webhookOutboxRepository.findById(deliveryId)).thenReturn(java.util.Optional.of(delivery));
        when(webhookOutboxRepository.save(any(WebhookOutbox.class))).thenReturn(delivery);

        WebhookOutbox result = service.replayDelivery(deliveryId, tenantId);

        verify(delivery).markForRetry();
        verify(delivery).setErrorMessage(null);
        verify(delivery).setHttpStatus(null);
        verify(webhookOutboxRepository).save(delivery);
    }

    @Test
    void testReplayFailsForNonDeadLetteredDelivery() {
        UUID deliveryId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();

        WebhookOutbox delivery = mock(WebhookOutbox.class);
        when(delivery.getDeliveryStatus()).thenReturn("PENDING");
        when(delivery.getTenant()).thenReturn(mock(com.techtune.nexus.tenant.domain.Tenant.class));
        when(delivery.getTenant().getId()).thenReturn(tenantId);

        when(webhookOutboxRepository.findById(deliveryId)).thenReturn(java.util.Optional.of(delivery));

        assertThrows(IllegalStateException.class, () -> service.replayDelivery(deliveryId, tenantId));
    }

    @Test
    void testReplayFailsForCrossTenantDelivery() {
        UUID deliveryId = UUID.randomUUID();
        UUID tenantId = UUID.randomUUID();
        UUID otherTenantId = UUID.randomUUID();

        WebhookOutbox delivery = mock(WebhookOutbox.class);
        when(delivery.getDeliveryStatus()).thenReturn("DEAD_LETTER");
        when(delivery.getTenant()).thenReturn(mock(com.techtune.nexus.tenant.domain.Tenant.class));
        when(delivery.getTenant().getId()).thenReturn(otherTenantId);

        when(webhookOutboxRepository.findById(deliveryId)).thenReturn(java.util.Optional.of(delivery));

        assertThrows(SecurityException.class, () -> service.replayDelivery(deliveryId, tenantId));
    }
}
