
package com.example.demo.repository;

import com.example.demo.model.ProjectEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ProjectEventRepository
        extends JpaRepository<ProjectEvent, Long> {

    // Find all events belonging to a project
    List<ProjectEvent> findByProjectId(Long projectId);

    // Find events by status
    List<ProjectEvent> findByStatusIgnoreCase(String status);

    // Find events by type
    List<ProjectEvent> findByEventTypeIgnoreCase(String eventType);

    // Find events within a date range
    List<ProjectEvent> findByStartDateTimeBetween(
            LocalDateTime start,
            LocalDateTime end
    );

    // Find events belonging to a project within a date range
    List<ProjectEvent> findByProjectIdAndStartDateTimeBetween(
            Long projectId,
            LocalDateTime start,
            LocalDateTime end
    );

    // Search events by title
    List<ProjectEvent> findByTitleContainingIgnoreCase(String title);
}
