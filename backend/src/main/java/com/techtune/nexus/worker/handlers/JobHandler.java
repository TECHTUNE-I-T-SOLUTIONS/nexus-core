package com.techtune.nexus.worker.handlers;

import com.techtune.nexus.job.domain.Job;
import com.fasterxml.jackson.databind.JsonNode;

public interface JobHandler {

    String getJobType();

    void handle(Job job) throws Exception;
}
