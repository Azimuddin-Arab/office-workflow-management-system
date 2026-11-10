
package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Milestone;

public interface MilestoneRepository extends JpaRepository<Milestone, Long> {

    // Find all milestones belonging to a project
    List<Milestone> findByProjectId(Long projectId);

    // Find milestones by status
    List<Milestone> findByStatusIgnoreCase(String status);

    // Search milestones by title
    List<Milestone> findByTitleContainingIgnoreCase(String title);

    // Find milestones by project and status
    List<Milestone> findByProjectIdAndStatusIgnoreCase(
            Long projectId,
            String status
    );
}
