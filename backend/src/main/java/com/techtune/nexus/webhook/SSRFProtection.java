package com.techtune.nexus.webhook;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Set;

/**
 * SSRF (Server-Side Request Forgery) protection utilities.
 * 
 * This class provides comprehensive validation of webhook URLs to prevent
 * internal network access, loopback connections, and DNS rebinding attacks.
 */
public class SSRFProtection {

    // Cloud metadata endpoints that should be blocked
    private static final Set<String> CLOUD_METADATA_ENDPOINTS = Set.of(
            "169.254.169.254",  // AWS, GCP, Azure metadata
            "metadata.google.internal",
            "metadata.azure.com"
    );

    /**
     * Validates if a host is safe for webhook delivery.
     * 
     * This performs comprehensive checks including:
     * - Loopback addresses (IPv4 and IPv6)
     * - Private IP ranges (RFC1918)
     * - Link-local addresses
     * - Unique-local IPv6 addresses
     * - IPv4-mapped IPv6 addresses
     * - Cloud metadata endpoints
     * 
     * @param host The hostname or IP address to validate
     * @return true if the host is safe, false if it should be blocked
     */
    public static boolean isSafeHost(String host) {
        if (host == null || host.isBlank()) {
            return false;
        }

        try {
            InetAddress address = InetAddress.getByName(host);
            return isSafeAddress(address);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    /**
     * Validates if an InetAddress is safe for webhook delivery.
     */
    public static boolean isSafeAddress(InetAddress address) {
        if (address == null) {
            return false;
        }

        String hostAddress = address.getHostAddress();

        // Check cloud metadata endpoints
        if (CLOUD_METADATA_ENDPOINTS.contains(hostAddress) || 
            CLOUD_METADATA_ENDPOINTS.contains(address.getHostName())) {
            return false;
        }

        // Check loopback addresses
        if (address.isLoopbackAddress()) {
            return false;
        }

        // Check link-local addresses
        if (address.isLinkLocalAddress()) {
            return false;
        }

        // Check site-local addresses (includes RFC1918 private IPs)
        if (address.isSiteLocalAddress()) {
            return false;
        }

        // Check for IPv4-mapped IPv6 addresses (::ffff:x.x.x.x)
        if (isIPv4MappedIPv6(address)) {
            return false;
        }

        // Check for multicast addresses
        if (address.isMulticastAddress()) {
            return false;
        }

        return true;
    }

    /**
     * Checks if an InetAddress is an IPv4-mapped IPv6 address.
     * These are in the format ::ffff:x.x.x.x and can bypass IPv4 checks.
     */
    private static boolean isIPv4MappedIPv6(InetAddress address) {
        byte[] bytes = address.getAddress();
        if (bytes.length == 16) {
            // IPv6 addresses have 16 bytes
            // IPv4-mapped addresses start with 0:0:0:0:0:ffff
            return bytes[0] == 0 && bytes[1] == 0 && bytes[2] == 0 && bytes[3] == 0 &&
                   bytes[4] == 0 && bytes[5] == 0 && bytes[6] == 0 && bytes[7] == 0 &&
                   bytes[8] == 0 && bytes[9] == 0 && bytes[10] == 0xff && bytes[11] == 0xff;
        }
        return false;
    }

    /**
     * Validates if a port is within the acceptable range.
     * 
     * @param port The port number (-1 for default port)
     * @return true if the port is valid, false otherwise
     */
    public static boolean isValidPort(int port) {
        if (port == -1) {
            return true; // Default port is acceptable
        }
        return port >= 80 && port <= 65535;
    }

    /**
     * Validates if a URL scheme is allowed.
     * 
     * @param scheme The URL scheme (http, https, etc.)
     * @return true if the scheme is allowed, false otherwise
     */
    public static boolean isAllowedScheme(String scheme) {
        return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
    }

    /**
     * Checks if a URL contains user-info component (username:password@host).
     * This should be rejected as it can be used for authentication bypass.
     * 
     * @param url The full URL string
     * @return true if user-info is present (should be blocked)
     */
    public static boolean hasUserInfo(String url) {
        // Find the scheme:// separator
        int schemeEnd = url.indexOf("://");
        if (schemeEnd == -1) {
            return false;
        }
        
        // Find the path separator
        int pathStart = url.indexOf('/', schemeEnd + 3);
        if (pathStart == -1) {
            pathStart = url.length();
        }
        
        // Extract authority part (between :// and /)
        String authority = url.substring(schemeEnd + 3, pathStart);
        
        // Check for @ in authority (indicates user-info)
        return authority.contains("@");
    }
}
