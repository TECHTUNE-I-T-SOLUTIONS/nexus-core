package com.techtune.nexus.webhook.encryption;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SecretEncryptionServiceTest {

    private SecretEncryptionService encryptionService;

    @BeforeEach
    void setUp() {
        encryptionService = new SecretEncryptionService("test-encryption-key-min-32-chars-long");
    }

    @Test
    void testEncryptDecrypt() {
        String plaintext = "my-webhook-secret-key-is-16-chars";
        String encrypted = encryptionService.encrypt(plaintext);
        
        assertNotNull(encrypted);
        assertNotEquals(plaintext, encrypted);
        
        String decrypted = encryptionService.decrypt(encrypted);
        assertEquals(plaintext, decrypted);
    }

    @Test
    void testEncryptProducesDifferentCiphertext() {
        String plaintext = "test-secret-12345678";
        String encrypted1 = encryptionService.encrypt(plaintext);
        String encrypted2 = encryptionService.encrypt(plaintext);
        
        // Each encryption should produce different ciphertext due to random IV
        assertNotEquals(encrypted1, encrypted2);
        
        // But both should decrypt to the same plaintext
        assertEquals(plaintext, encryptionService.decrypt(encrypted1));
        assertEquals(plaintext, encryptionService.decrypt(encrypted2));
    }

    @Test
    void testDecryptWithWrongCiphertext() {
        assertThrows(RuntimeException.class, () -> {
            encryptionService.decrypt("invalid-ciphertext");
        });
    }

    @Test
    void testEmptyKeyThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            new SecretEncryptionService("");
        });
    }

    @Test
    void testNullKeyThrowsException() {
        assertThrows(IllegalStateException.class, () -> {
            new SecretEncryptionService(null);
        });
    }
}
