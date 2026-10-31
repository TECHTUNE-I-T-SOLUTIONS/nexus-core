package com.techtune.nexus.worker.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobHandlerRegistry {

    private static final Logger logger = LoggerFactory.getLogger(JobHandlerRegistry.class);
    
    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();
    private volatile boolean frozen = false;

    public void register(JobHandler handler) {
        if (frozen) {
            throw new IllegalStateException("Handler registry is frozen and cannot accept new registrations");
        }

        String jobType = handler.getJobType();
        
        if (jobType == null || jobType.isBlank()) {
            throw new IllegalArgumentException("Job type cannot be null or blank");
        }

        if (handlers.containsKey(jobType)) {
            throw new IllegalStateException("Duplicate handler registration for job type: " + jobType);
        }

        logger.info("Registering handler for job type: {}", jobType);
        handlers.put(jobType, handler);
    }

    public JobHandler getHandler(String jobType) {
        if (jobType == null || jobType.isBlank()) {
            throw new IllegalArgumentException("Job type cannot be null or blank");
        }

        JobHandler handler = handlers.get(jobType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for job type: " + jobType);
        }
        return handler;
    }

    public boolean hasHandler(String jobType) {
        return jobType != null && !jobType.isBlank() && handlers.containsKey(jobType);
    }

    public Set<String> getRegisteredJobTypes() {
        return Set.copyOf(handlers.keySet());
    }

    public void freeze() {
        this.frozen = true;
        logger.info("Handler registry frozen with {} registered handlers", handlers.size());
    }

    public boolean isFrozen() {
        return frozen;
    }
}

