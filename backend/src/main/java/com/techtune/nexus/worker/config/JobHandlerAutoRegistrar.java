package com.techtune.nexus.worker.config;

import com.techtune.nexus.worker.handlers.JobHandler;
import com.techtune.nexus.worker.handlers.JobHandlerRegistry;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class JobHandlerAutoRegistrar {

    private final ApplicationContext applicationContext;
    private final JobHandlerRegistry registry;

    public JobHandlerAutoRegistrar(ApplicationContext applicationContext, JobHandlerRegistry registry) {
        this.applicationContext = applicationContext;
        this.registry = registry;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void registerHandlers() {
        applicationContext.getBeansOfType(JobHandler.class).values().forEach(handler -> {
            try {
                registry.register(handler);
            } catch (IllegalStateException e) {
                // Duplicate or invalid - log and continue
                System.err.println("Failed to register handler: " + e.getMessage());
            }
        });

        // Freeze the registry after auto-registration
        registry.freeze();
    }
}
