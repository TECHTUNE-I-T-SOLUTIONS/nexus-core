package com.techtune.nexus.worker.handlers;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JobHandlerRegistry {

    private final Map<String, JobHandler> handlers = new ConcurrentHashMap<>();

    public void register(JobHandler handler) {
        handlers.put(handler.getJobType(), handler);
    }

    public JobHandler getHandler(String jobType) {
        JobHandler handler = handlers.get(jobType);
        if (handler == null) {
            throw new IllegalArgumentException("No handler registered for job type: " + jobType);
        }
        return handler;
    }

    public boolean hasHandler(String jobType) {
        return handlers.containsKey(jobType);
    }
}
