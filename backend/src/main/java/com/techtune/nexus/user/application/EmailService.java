package com.techtune.nexus.user.application;

import com.techtune.nexus.config.EmailProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final EmailProperties emailProperties;

    public EmailService(EmailProperties emailProperties) {
        this.emailProperties = emailProperties;
    }

    public void sendVerificationEmail(String to, String verificationToken) {
        if (!emailProperties.isEnabled() || !emailProperties.isVerificationEnabled()) {
            logger.info("Email verification disabled, skipping email to: {}", to);
            return;
        }

        // Log the email content for now (requires spring-boot-starter-mail for actual delivery)
        logger.info("VERIFICATION EMAIL (would send to: {})", to);
        logger.info("Token: {}", verificationToken);
        logger.info("Expires in: {} minutes", emailProperties.getTokenExpirationMinutes());
    }

    public void sendPasswordResetEmail(String to, String resetToken) {
        if (!emailProperties.isEnabled() || !emailProperties.isPasswordResetEnabled()) {
            logger.info("Password reset email disabled, skipping email to: {}", to);
            return;
        }

        // Log the email content for now (requires spring-boot-starter-mail for actual delivery)
        logger.info("PASSWORD RESET EMAIL (would send to: {})", to);
        logger.info("Token: {}", resetToken);
        logger.info("Expires in: {} minutes", emailProperties.getTokenExpirationMinutes());
    }

    public boolean isEmailConfigured() {
        return emailProperties.isEnabled() &&
               emailProperties.getUsername() != null &&
               !emailProperties.getUsername().isBlank() &&
               emailProperties.getPassword() != null &&
               !emailProperties.getPassword().isBlank();
    }
}
