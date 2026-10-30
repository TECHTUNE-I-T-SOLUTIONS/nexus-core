package com.techtune.nexus.job.domain;

public enum JobStatus {
    PENDING,
    SCHEDULED,
    QUEUED,
    RUNNING,
    SUCCEEDED,
    FAILED,
    RETRY_WAIT,
    CANCELLED,
    DEAD_LETTERED
}
