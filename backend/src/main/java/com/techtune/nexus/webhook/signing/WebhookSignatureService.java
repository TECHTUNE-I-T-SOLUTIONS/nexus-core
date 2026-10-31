package com.techtune.nexus.webhook.signing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

/**
 * Service for generating and verifying HMAC-SHA256 webhook signatures.
 *
 * <p>Signature format: Base64(HMAC-SHA256(secret, payload))
 *
 * <p>The payload is the raw JSON webhook body as UTF-8 bytes.
 * The secret is the webhook endpoint secret as UTF-8 bytes.
 *
 * <p>Consumers should verify signatures by:
 * 1. Extract the X-Webhook-Signature header
 * 2. Compute HMAC-SHA256(secret, rawRequestBodyBytes)
 * 3. Base64-encode the result
 * 4. Compare with the header value using constant-time comparison
 */
@Service
public class WebhookSignatureService {

    private static final Logger logger = LoggerFactory.getLogger(WebhookSignatureService.class);
    private static final String HMAC_SHA256_ALGORITHM = "HmacSHA256";

    /**
     * Generates an HMAC-SHA256 signature for the given payload and secret.
     *
     * @param secret The webhook endpoint secret (UTF-8 string)
     * @param payload The raw JSON payload (UTF-8 string)
     * @return Base64-encoded HMAC-SHA256 signature
     * @throws RuntimeException if signature generation fails
     */
    public String generateSignature(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256_ALGORITHM);
            SecretKeySpec secretKey = new SecretKeySpec(
                    secret.getBytes(StandardCharsets.UTF_8),
                    HMAC_SHA256_ALGORITHM
            );
            mac.init(secretKey);
            byte[] signatureBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signatureBytes);
        } catch (NoSuchAlgorithmException e) {
            logger.error("HMAC-SHA256 algorithm not available", e);
            throw new RuntimeException("HMAC-SHA256 algorithm not available", e);
        } catch (InvalidKeyException e) {
            logger.error("Invalid secret key for HMAC-SHA256", e);
            throw new RuntimeException("Invalid secret key for HMAC-SHA256", e);
        }
    }

    /**
     * Verifies a signature against the expected value.
     *
     * <p>Uses constant-time comparison to prevent timing attacks.
     *
     * @param secret The webhook endpoint secret
     * @param payload The raw JSON payload
     * @param providedSignature The signature from X-Webhook-Signature header
     * @return true if the signature is valid, false otherwise
     */
    public boolean verifySignature(String secret, String payload, String providedSignature) {
        String expectedSignature = generateSignature(secret, payload);
        return constantTimeEquals(expectedSignature, providedSignature);
    }

    /**
     * Constant-time string comparison to prevent timing attacks.
     *
     * @param a First string
     * @param b Second string
     * @return true if strings are equal, false otherwise
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return a == b;
        }

        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);

        if (aBytes.length != bBytes.length) {
            return false;
        }

        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }

        return result == 0;
    }
}
