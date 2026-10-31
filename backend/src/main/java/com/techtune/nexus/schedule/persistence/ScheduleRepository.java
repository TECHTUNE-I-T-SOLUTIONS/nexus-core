package com.techtune.nexus.schedule.persistence;

import com.techtune.nexus.schedule.domain.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, UUID> {

    @Query("SELECT s FROM Schedule s WHERE s.enabled = true AND s.nextExecutionAt <= :now ORDER BY s.nextExecutionAt")
    List<Schedule> findDueSchedules(@Param("now") Instant now);

    List<Schedule> findByTenantId(UUID tenantId);

    List<Schedule> findByTenantIdAndEnabled(UUID tenantId, boolean enabled);
}
