package com.techtune.nexus.user.application;

import com.techtune.nexus.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
@ConditionalOnProperty(prefix = "nexus.auth.email", name = "enabled", havingValue = "true")
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;
    private final EmailProperties emailProperties;

    public EmailService(JavaMailSender mailSender, EmailProperties emailProperties) {
        this.mailSender = mailSender;
        this.emailProperties = emailProperties;
    }

    public void sendVerificationEmail(String to, String verificationToken) {
        if (!emailProperties.isEnabled() || !emailProperties.isVerificationEnabled()) {
            logger.info("Email verification disabled, skipping email to: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFrom());
            message.setTo(to);
            message.setSubject("Verify your NEXUS CORE account");
            message.setText(String.format(
                "Hello,\n\n" +
                "Please verify your email address by clicking the link below:\n\n" +
                "https://yourdomain.com/verify?token=%s\n\n" +
                "This link will expire in %d minutes.\n\n" +
                "If you did not create an account, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The NEXUS CORE Team",
                verificationToken,
                emailProperties.getTokenExpirationMinutes()
            ));

            mailSender.send(message);
            logger.info("Verification email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send verification email to: {}", to, e);
            throw new RuntimeException("Failed to send verification email", e);
        }
    }

    public void sendPasswordResetEmail(String to, String resetToken) {
        if (!emailProperties.isEnabled() || !emailProperties.isPasswordResetEnabled()) {
            logger.info("Password reset email disabled, skipping email to: {}", to);
            return;
        }

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailProperties.getFrom());
            message.setTo(to);
            message.setSubject("Reset your NEXUS CORE password");
            message.setText(String.format(
                "Hello,\n\n" +
                "You requested a password reset for your NEXUS CORE account.\n\n" +
                "Click the link below to reset your password:\n\n" +
                "https://yourdomain.com/reset-password?token=%s\n\n" +
                "This link will expire in %d minutes.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The NEXUS CORE Team",
                resetToken,
                emailProperties.getTokenExpirationMinutes()
            ));

            mailSender.send(message);
            logger.info("Password reset email sent to: {}", to);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", to, e);
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    public boolean isEmailConfigured() {
        return emailProperties.isEnabled() &&
               emailProperties.getUsername() != null &&
               !emailProperties.getUsername().isBlank() &&
               emailProperties.getPassword() != null &&
               !emailProperties.getPassword().isBlank();
    }
}
