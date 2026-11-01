package com.techtune.nexus.job.api;

import com.techtune.nexus.job.application.JobService;
import com.techtune.nexus.job.domain.Job;
import com.techtune.nexus.user.application.AuthorizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Job management endpoints")
public class JobController {

    private final JobService jobService;
    private final AuthorizationService authorizationService;

    public JobController(JobService jobService, AuthorizationService authorizationService) {
        this.jobService = jobService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_jobs:write')")
    @Operation(summary = "Create a new job")
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request, Authentication authentication) {
        // Check tenant membership for human users
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, request.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Job job = jobService.createJob(
                request.getTenantId(),
                request.getJobType(),
                request.getPayload(),
                request.getPriority(),
                request.getScheduledAt(),
                request.getMaxAttempts(),
                request.getTimeoutSeconds(),
                request.getIdempotencyKey(),
                null // Manual jobs don't have a schedule
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(JobResponse.from(job));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_jobs:read')")
    @Operation(summary = "Get job by ID")
    public ResponseEntity<JobResponse> getJob(@PathVariable UUID id, Authentication authentication) {
        Job job = jobService.getJob(id);
        
        // Check tenant membership for human users
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, job.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(JobResponse.from(job));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_jobs:read')")
    @Operation(summary = "List jobs")
    public ResponseEntity<List<JobResponse>> listJobs(
            @RequestParam(required = false) UUID tenantId,
            @RequestParam(required = false) String status,
            Authentication authentication
    ) {
        // Check tenant membership for human users
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && tenantId != null && !authorizationService.hasAccessToTenant(userId, tenantId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<Job> jobs = jobService.listJobs(tenantId, status);
        return ResponseEntity.ok(jobs.stream().map(JobResponse::from).toList());
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('SCOPE_jobs:cancel')")
    @Operation(summary = "Cancel a job")
    public ResponseEntity<JobResponse> cancelJob(@PathVariable UUID id, Authentication authentication) {
        Job job = jobService.getJob(id);
        
        // Check tenant membership for human users
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, job.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Job cancelledJob = jobService.cancelJob(id);
        return ResponseEntity.ok(JobResponse.from(cancelledJob));
    }

    @PostMapping("/{id}/retry")
    @PreAuthorize("hasAuthority('SCOPE_jobs:retry')")
    @Operation(summary = "Retry a failed or dead-lettered job")
    public ResponseEntity<JobResponse> retryJob(@PathVariable UUID id, Authentication authentication) {
        Job job = jobService.getJob(id);
        
        // Check tenant membership for human users
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, job.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Job retriedJob = jobService.retryJob(id);
        return ResponseEntity.ok(JobResponse.from(retriedJob));
    }
}
