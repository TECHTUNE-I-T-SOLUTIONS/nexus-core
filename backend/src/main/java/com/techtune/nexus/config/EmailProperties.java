package com.techtune.nexus.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "nexus.auth.email")
public class EmailProperties {

    private boolean enabled = false;
    private String provider = "zoho";
    private String host = "smtp.zoho.com";
    private int port = 587;
    private String username;
    private String password;
    private String from = "noreply@yourdomain.com";
    private String fromName = "NEXUS CORE";
    private boolean verificationEnabled = false;
    private boolean passwordResetEnabled = false;
    private int tokenExpirationMinutes = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getFrom() {
        return from;
    }

    public void setFrom(String from) {
        this.from = from;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public boolean isVerificationEnabled() {
        return verificationEnabled;
    }

    public void setVerificationEnabled(boolean verificationEnabled) {
        this.verificationEnabled = verificationEnabled;
    }

    public boolean isPasswordResetEnabled() {
        return passwordResetEnabled;
    }

    public void setPasswordResetEnabled(boolean passwordResetEnabled) {
        this.passwordResetEnabled = passwordResetEnabled;
    }

    public int getTokenExpirationMinutes() {
        return tokenExpirationMinutes;
    }

    public void setTokenExpirationMinutes(int tokenExpirationMinutes) {
        this.tokenExpirationMinutes = tokenExpirationMinutes;
    }
}
