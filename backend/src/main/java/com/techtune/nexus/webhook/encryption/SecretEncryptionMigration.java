package com.techtune.nexus.webhook.encryption;

import com.techtune.nexus.webhook.domain.WebhookEndpoint;
import com.techtune.nexus.webhook.persistence.WebhookEndpointRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Operational command to encrypt existing plaintext webhook secrets.
 *
 * Usage: Run with --spring.profiles.active=encrypt-secrets
 *        and set nexus.webhook.encryption.enabled=true and nexus.webhook.encryption.key
 *
 * This command:
 * - Finds all webhook endpoints with plaintext secrets (secret_encrypted = false)
 * - Encrypts the secret using AES-GCM
 * - Updates the record with encrypted secret and sets secret_encrypted = true
 * - Verifies decryption before committing
 * - Can be safely rerun (skips already-encrypted records)
 */
@Component
@ConditionalOnProperty(name = "nexus.webhook.encryption.enabled", havingValue = "true")
public class SecretEncryptionMigration implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(SecretEncryptionMigration.class);

    private final WebhookEndpointRepository webhookEndpointRepository;
    private final SecretEncryptionService encryptionService;

    public SecretEncryptionMigration(WebhookEndpointRepository webhookEndpointRepository,
                                    SecretEncryptionService encryptionService) {
        this.webhookEndpointRepository = webhookEndpointRepository;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        logger.info("Starting webhook secret encryption migration...");

        List<WebhookEndpoint> endpoints = webhookEndpointRepository.findAll();
        int encrypted = 0;
        int skipped = 0;
        int failed = 0;

        for (WebhookEndpoint endpoint : endpoints) {
            if (endpoint.isSecretEncrypted()) {
                skipped++;
                logger.debug("Skipping already-encrypted endpoint: {}", endpoint.getId());
                continue;
            }

            try {
                String plaintextSecret = endpoint.getSecret();
                String encryptedSecret = encryptionService.encrypt(plaintextSecret);

                // Verify decryption before committing
                String decryptedSecret = encryptionService.decrypt(encryptedSecret);
                if (!decryptedSecret.equals(plaintextSecret)) {
                    logger.error("Decryption verification failed for endpoint: {}", endpoint.getId());
                    failed++;
                    continue;
                }

                endpoint.setSecret(encryptedSecret);
                endpoint.setSecretEncrypted(true);
                webhookEndpointRepository.save(endpoint);

                encrypted++;
                logger.info("Encrypted secret for endpoint: {}", endpoint.getId());

            } catch (Exception e) {
                logger.error("Failed to encrypt secret for endpoint: {}", endpoint.getId(), e);
                failed++;
            }
        }

        logger.info("Encryption migration complete. Encrypted: {}, Skipped: {}, Failed: {}", encrypted, skipped, failed);

        if (failed > 0) {
            throw new IllegalStateException("Encryption migration failed for " + failed + " endpoints");
        }
    }
}
