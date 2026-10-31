package com.techtune.nexus.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(EmailProperties.class)
public class MailConfiguration {

    // JavaMailSender bean will be created when email is enabled and dependency is available
    // For now, EmailService will log instead of sending emails
}
