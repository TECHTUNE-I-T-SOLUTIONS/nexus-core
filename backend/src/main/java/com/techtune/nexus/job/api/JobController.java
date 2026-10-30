package com.techtune.nexus.job.api;

import com.techtune.nexus.job.application.JobService;
import com.techtune.nexus.job.domain.Job;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Job management endpoints")
public class JobController {

    private final JobService jobService;

    public JobController(JobService jobService) {
        this.jobService = jobService;
    }

    @PostMapping
    @Operation(summary = "Create a new job")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        Job job = jobService.createJob(
                request.getTenantId(),
                request.getJobType(),
                request.getPayload(),
                request.getPriority(),
                request.getScheduledAt(),
                request.getMaxAttempts(),
                request.getTimeoutSeconds(),
                request.getIdempotencyKey()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id) {
        Job job = jobService.getJob(id);
        return ResponseEntity.ok(JobResponse.from(job));
    }

    @GetMapping
    @Operation(summary = "List jobs")
    public ResponseEntity<List<JobResponse>> listJobs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status
    ) {
        List<Job> jobs = jobService.listJobs(tenantId, status);
        return ResponseEntity.ok(jobs.stream().map(JobResponse::from).toList());
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel a job")
    public ResponseEntity<JobResponse> cancelJob(@PathVariable UUID id) {
        Job job = jobService.cancelJob(id);
        return ResponseEntity.ok(JobResponse.from(job));
    }

    @PostMapping("/{id}/retry")
    @Operation(summary = "Retry a failed or dead-lettered job")
    public ResponseEntity<JobResponse> retryJob(@PathVariable UUID id) {
        Job job = jobService.retryJob(id);
        return ResponseEntity.ok(JobResponse.from(job));
    }
}
