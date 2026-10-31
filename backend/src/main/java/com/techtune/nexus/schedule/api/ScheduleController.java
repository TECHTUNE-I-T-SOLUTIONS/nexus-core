package com.techtune.nexus.schedule.api;

import com.techtune.nexus.schedule.application.ScheduleService;
import com.techtune.nexus.schedule.domain.Schedule;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/schedules")
@Tag(name = "Schedules", description = "Recurring job schedule management endpoints")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Create a new schedule")
    public ResponseEntity<ScheduleResponse> createSchedule(@Valid @RequestBody CreateScheduleRequest request) {
        Schedule schedule = scheduleService.createSchedule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ScheduleResponse.from(schedule));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:read')")
    @Operation(summary = "Get schedule by ID")
    public ResponseEntity<ScheduleResponse> getSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.getSchedule(id)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + id));
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SCOPE_schedules:read')")
    @Operation(summary = "List schedules")
    public ResponseEntity<List<ScheduleResponse>> listSchedules(@RequestParam(required = false) UUID tenantId) {
        List<Schedule> schedules = scheduleService.listSchedules(tenantId);
        return ResponseEntity.ok(schedules.stream().map(ScheduleResponse::from).toList());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Update a schedule")
    public ResponseEntity<ScheduleResponse> updateSchedule(@PathVariable UUID id, @Valid @RequestBody CreateScheduleRequest request) {
        Schedule schedule = scheduleService.updateSchedule(id, request);
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @PostMapping("/{id}/pause")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Pause a schedule")
    public ResponseEntity<ScheduleResponse> pauseSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.pauseSchedule(id);
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @PostMapping("/{id}/resume")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Resume a schedule")
    public ResponseEntity<ScheduleResponse> resumeSchedule(@PathVariable UUID id) {
        Schedule schedule = scheduleService.resumeSchedule(id);
        return ResponseEntity.ok(ScheduleResponse.from(schedule));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SCOPE_schedules:write')")
    @Operation(summary = "Delete a schedule")
    public ResponseEntity<Void> deleteSchedule(@PathVariable UUID id) {
        scheduleService.deleteSchedule(id);
        return ResponseEntity.noContent().build();
    }
}
