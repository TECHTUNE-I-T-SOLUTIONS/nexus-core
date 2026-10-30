package com.techtune.nexus.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        Integer status,
        String error,
        String message,
        Instant timestamp,
        Map<String, Object> details
) {
    public ErrorResponse(Integer status, String error, String message, Instant timestamp) {
        this(status, error, message, timestamp, null);
    }
}
