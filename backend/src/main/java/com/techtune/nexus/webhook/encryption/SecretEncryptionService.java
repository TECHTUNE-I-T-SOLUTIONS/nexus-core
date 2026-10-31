package com.techtune.nexus.webhook.encryption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Service for encrypting and decrypting webhook secrets using AES-GCM.
 * 
 * Secrets are encrypted at rest with authenticated encryption.
 * Keys are derived from configuration using SHA-256 hashing.
 */
@Service
public class SecretEncryptionService {

    private static final Logger logger = LoggerFactory.getLogger(SecretEncryptionService.class);
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    private static final int KEY_LENGTH = 256;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom;

    public SecretEncryptionService(String encryptionKey) {
        if (encryptionKey == null || encryptionKey.isBlank()) {
            throw new IllegalStateException("Encryption key must be configured");
        }
        
        this.secretKey = deriveKey(encryptionKey);
        this.secureRandom = new SecureRandom();
        
        logger.info("Secret encryption service initialized");
    }

    /**
     * Encrypts a plaintext secret.
     * 
     * Format: IV (12 bytes) + Ciphertext + GCM Tag (16 bytes)
     * All encoded in Base64.
     * 
     * @param plaintext The plaintext secret
     * @return Base64-encoded encrypted secret
     */
    public String encrypt(String plaintext) {
        try {
            byte[] iv = new byte[GCM_IV_LENGTH];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, spec);

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Combine IV + ciphertext + tag
            ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
            byteBuffer.put(iv);
            byteBuffer.put(ciphertext);

            return Base64.getEncoder().encodeToString(byteBuffer.array());
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt secret", e);
        }
    }

    /**
     * Decrypts an encrypted secret.
     * 
     * @param encrypted Base64-encoded encrypted secret
     * @return The plaintext secret
     */
    public String decrypt(String encrypted) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encrypted);

            // Extract IV (first 12 bytes)
            ByteBuffer byteBuffer = ByteBuffer.wrap(decoded);
            byte[] iv = new byte[GCM_IV_LENGTH];
            byteBuffer.get(iv);

            // Extract ciphertext (remaining bytes)
            byte[] ciphertext = new byte[byteBuffer.remaining()];
            byteBuffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec);

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to decrypt secret", e);
        }
    }

    /**
     * Derives a cryptographic key from the configuration key.
     * Uses SHA-256 to hash the key and extract 256 bits.
     */
    private SecretKey deriveKey(String key) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(key.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(hash, ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to derive encryption key", e);
        }
    }
}
