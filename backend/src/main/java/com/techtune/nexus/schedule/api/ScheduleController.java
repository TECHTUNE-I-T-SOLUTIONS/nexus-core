package com.techtune.nexus.schedule.api;

import com.techtune.nexus.schedule.application.ScheduleService;
import com.techtune.nexus.schedule.domain.Schedule;
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
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Recurring job schedule management endpoints")
public class ScheduleController {

    private final ScheduleService scheduleService;
    private final AuthorizationService authorizationService;

    public ScheduleController(ScheduleService scheduleService, AuthorizationService authorizationService) {
        this.scheduleService = scheduleService;
        this.authorizationService = authorizationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request, Authentication authentication) {
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, request.tenantId())) {
            return ResponseEntity.status(403).build();
        }
        Schedule schedule = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleResponse.from(schedule));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:read')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable UUID id, Authentication authentication) {
        Schedule schedule = scheduleService.getSchedule(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, schedule.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_schedules:read')")
    @Operation(summary = "List schedules")
    public ResponseEntity<List<ScheduleResponse>> listSchedules(@RequestParam(required = false) UUID tenantId, Authentication authentication) {
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && tenantId != null && !authorizationService.hasAccessToTenant(userId, tenantId)) {
            return ResponseEntity.status(403).build();
        }
        
        List<Schedule> schedules = scheduleService.listSchedules(tenantId);
        return ResponseEntity.ok(schedules.stream().map(ScheduleResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Update a schedule")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable UUID id, @Valid @RequestBody CreateScheduleRequest request, Authentication authentication) {
        Schedule schedule = scheduleService.getSchedule(id).orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, schedule.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Schedule updatedSchedule = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ScheduleResponse.from(updatedSchedule));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Pause a schedule")
    public ResponseEntity<ScheduleResponse> pauseSchedule(@PathVariable UUID id, Authentication authentication) {
        Schedule schedule = scheduleService.getSchedule(id).orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, schedule.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Schedule pausedSchedule = scheduleService.pauseSchedule(id);
        return ResponseEntity.ok(ScheduleResponse.from(pausedSchedule));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Resume a schedule")
    public ResponseEntity<ScheduleResponse> resumeSchedule(@PathVariable UUID id, Authentication authentication) {
        Schedule schedule = scheduleService.getSchedule(id).orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, schedule.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        Schedule resumedSchedule = scheduleService.resumeSchedule(id);
        return ResponseEntity.ok(ScheduleResponse.from(resumedSchedule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id, Authentication authentication) {
        Schedule schedule = scheduleService.getSchedule(id).orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        
        UUID userId = authorizationService.getUserIdFromAuthentication(authentication);
        if (userId != null && !authorizationService.hasAccessToTenant(userId, schedule.getTenantId())) {
            return ResponseEntity.status(403).build();
        }
        
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
