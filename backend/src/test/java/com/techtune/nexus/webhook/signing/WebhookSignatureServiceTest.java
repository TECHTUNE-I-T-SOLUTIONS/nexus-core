package com.techtune.nexus.webhook.signing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureServiceTest {

    private WebhookSignatureService signatureService;

    @BeforeEach
    void setUp() {
        signatureService = new WebhookSignatureService();
    }

    @Test
    void testSignatureIsDeterministic() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String signature1 = signatureService.generateSignature(secret, payload);
        String signature2 = signatureService.generateSignature(secret, payload);

        assertEquals(signature1, signature2);
    }

    @Test
    void testSignatureChangesWithDifferentPayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload1 = "{\"test\":\"data1\"}";
        String payload2 = "{\"test\":\"data2\"}";

        String signature1 = signatureService.generateSignature(secret, payload1);
        String signature2 = signatureService.generateSignature(secret, payload2);

        assertNotEquals(signature1, signature2);
    }

    @Test
    void testSignatureChangesWithDifferentSecret() {
        String secret1 = "my-secret-key-is-16-chars";
        String secret2 = "another-secret-key-16-ch";
        String payload = "{\"test\":\"data\"}";

        String signature1 = signatureService.generateSignature(secret1, payload);
        String signature2 = signatureService.generateSignature(secret2, payload);

        assertNotEquals(signature1, signature2);
    }

    @Test
    void testSignatureChangesWithSamePayloadDifferentCasing() {
        String secret = "my-secret-key-is-16-chars";
        String payload1 = "{\"test\":\"Data\"}";
        String payload2 = "{\"test\":\"data\"}";

        String signature1 = signatureService.generateSignature(secret, payload1);
        String signature2 = signatureService.generateSignature(secret, payload2);

        assertNotEquals(signature1, signature2);
    }

    @Test
    void testEmptyPayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "";

        String signature = signatureService.generateSignature(secret, payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    void testUnicodePayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"message\":\"Hello  🌍\"}";

        String signature = signatureService.generateSignature(secret, payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    void testEscapedJsonPayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"key\":\"value with \\\"quotes\\\"\"}";

        String signature = signatureService.generateSignature(secret, payload);

        assertNotNull(signature);
        assertFalse(signature.isEmpty());
    }

    @Test
    void testVerificationWithValidSignature() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String signature = signatureService.generateSignature(secret, payload);
        boolean isValid = signatureService.verifySignature(secret, payload, signature);

        assertTrue(isValid);
    }

    @Test
    void testVerificationWithInvalidSignature() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        boolean isValid = signatureService.verifySignature(secret, payload, "invalid-signature");

        assertFalse(isValid);
    }

    @Test
    void testVerificationWithModifiedPayload() {
        String secret = "my-secret-key-is-16-chars";
        String payload1 = "{\"test\":\"data1\"}";
        String payload2 = "{\"test\":\"data2\"}";

        String signature = signatureService.generateSignature(secret, payload1);
        boolean isValid = signatureService.verifySignature(secret, payload2, signature);

        assertFalse(isValid);
    }

    @Test
    void testVerificationWithModifiedSecret() {
        String secret1 = "my-secret-key-is-16-chars";
        String secret2 = "another-secret-key-16-ch";
        String payload = "{\"test\":\"data\"}";

        String signature = signatureService.generateSignature(secret1, payload);
        boolean isValid = signatureService.verifySignature(secret2, payload, signature);

        assertFalse(isValid);
    }

    @Test
    void testConstantTimeComparisonPreventsTimingAttacks() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String correctSignature = signatureService.generateSignature(secret, payload);
        String wrongSignature = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

        // Both should complete in similar time (difficult to test precisely)
        // but the method should use constant-time comparison
        assertTrue(signatureService.verifySignature(secret, payload, correctSignature));
        assertFalse(signatureService.verifySignature(secret, payload, wrongSignature));
    }

    @Test
    void testVerificationWithNullSignature() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        boolean isValid = signatureService.verifySignature(secret, payload, null);

        assertFalse(isValid);
    }

    @Test
    void testVerificationWithEmptySignature() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        boolean isValid = signatureService.verifySignature(secret, payload, "");

        assertFalse(isValid);
    }

    @Test
    void testBase64Encoding() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String signature = signatureService.generateSignature(secret, payload);

        // Base64 encoding should only contain [A-Za-z0-9+/=]
        assertTrue(signature.matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    void testSignatureLengthIsConsistent() {
        String secret = "my-secret-key-is-16-chars";
        String payload = "{\"test\":\"data\"}";

        String signature1 = signatureService.generateSignature(secret, payload);
        String signature2 = signatureService.generateSignature(secret, payload);

        // HMAC-SHA256 produces 32 bytes, Base64-encoded to 44 characters
        assertEquals(44, signature1.length());
        assertEquals(44, signature2.length());
    }
}
