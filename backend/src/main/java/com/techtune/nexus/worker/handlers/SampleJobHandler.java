package com.techtune.nexus.worker.handlers;

import com.techtune.nexus.job.domain.Job;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class SampleJobHandler implements JobHandler {

    private static final Logger logger = LoggerFactory.getLogger(SampleJobHandler.class);

    @Override
    public String getJobType() {
        return "sample-job";
    }

    @Override
    public void handle(Job job) throws Exception {
        logger.info("Executing sample job: {}", job.getId());
        
        // Simulate some work
        Thread.sleep(100);
        
        logger.info("Sample job completed: {}", job.getId());
    }
}
