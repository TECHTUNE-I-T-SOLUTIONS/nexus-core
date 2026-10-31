package com.techtune.nexus.webhook;

import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.net.UnknownHostException;

import static org.junit.jupiter.api.Assertions.*;

class SSRFProtectionTest {

    @Test
    void testLoopbackIPv4IsBlocked() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("127.0.0.1");
        assertFalse(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testLoopbackIPv6IsBlocked() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("::1");
        assertFalse(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testLocalhostIsBlocked() {
        assertFalse(SSRFProtection.isSafeHost("localhost"));
    }

    @Test
    void testPrivateIPv4Range10IsBlocked() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("10.0.0.1");
        assertFalse(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testPrivateIPv4Range192_168IsBlocked() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("192.168.1.1");
        assertFalse(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testPrivateIPv4Range172_16_31IsBlocked() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("172.16.0.1");
        assertFalse(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testCloudMetadataEndpointIsBlocked() {
        assertFalse(SSRFProtection.isSafeHost("169.254.169.254"));
    }

    @Test
    void testPublicIPv4IsAllowed() throws UnknownHostException {
        InetAddress address = InetAddress.getByName("8.8.8.8");
        assertTrue(SSRFProtection.isSafeAddress(address));
    }

    @Test
    void testPublicHostnameIsAllowed() {
        assertTrue(SSRFProtection.isSafeHost("example.com"));
    }

    @Test
    void testNullHostIsBlocked() {
        assertFalse(SSRFProtection.isSafeHost(null));
    }

    @Test
    void testBlankHostIsBlocked() {
        assertFalse(SSRFProtection.isSafeHost(""));
    }

    @Test
    void testValidPortIsAllowed() {
        assertTrue(SSRFProtection.isValidPort(80));
        assertTrue(SSRFProtection.isValidPort(443));
        assertTrue(SSRFProtection.isValidPort(8080));
        assertTrue(SSRFProtection.isValidPort(65535));
    }

    @Test
    void testInvalidPortIsBlocked() {
        assertFalse(SSRFProtection.isValidPort(0));
        assertFalse(SSRFProtection.isValidPort(79));
        assertFalse(SSRFProtection.isValidPort(65536));
    }

    @Test
    void testDefaultPortIsAllowed() {
        assertTrue(SSRFProtection.isValidPort(-1));
    }

    @Test
    void testHttpSchemeIsAllowed() {
        assertTrue(SSRFProtection.isAllowedScheme("http"));
        assertTrue(SSRFProtection.isAllowedScheme("https"));
    }

    @Test
    void testOtherSchemesAreBlocked() {
        assertFalse(SSRFProtection.isAllowedScheme("ftp"));
        assertFalse(SSRFProtection.isAllowedScheme("file"));
        assertFalse(SSRFProtection.isAllowedScheme("gopher"));
    }

    @Test
    void testUserInfoDetection() {
        assertTrue(SSRFProtection.hasUserInfo("http://user:pass@example.com"));
        assertTrue(SSRFProtection.hasUserInfo("https://admin@example.com"));
        assertFalse(SSRFProtection.hasUserInfo("http://example.com"));
        assertFalse(SSRFProtection.hasUserInfo("https://example.com/path"));
    }
}
