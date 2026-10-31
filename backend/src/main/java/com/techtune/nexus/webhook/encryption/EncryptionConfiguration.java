package com.techtune.nexus.webhook.encryption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EncryptionConfiguration {

    @Bean
    public SecretEncryptionService secretEncryptionService(
            @Value("${nexus.webhook.encryption.enabled:false}") boolean encryptionEnabled,
            @Value("${nexus.webhook.encryption.key:}") String encryptionKey) {
        
        if (encryptionEnabled) {
            if (encryptionKey == null || encryptionKey.isBlank()) {
                throw new IllegalStateException("Encryption key must be provided when encryption is enabled");
            }
            return new SecretEncryptionService(encryptionKey);
        } else {
            // No-op service when encryption is disabled - uses a default key
            return new SecretEncryptionService("no-op-key-placeholder");
        }
    }
}
