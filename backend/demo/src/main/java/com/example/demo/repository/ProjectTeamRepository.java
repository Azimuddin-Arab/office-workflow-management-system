
package com.example.demo.repository;

import com.example.demo.model.ProjectTeam;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProjectTeamRepository
        extends JpaRepository<ProjectTeam, Long> {

    List<ProjectTeam> findByProjectId(Long projectId);

    List<ProjectTeam> findByEmployeeId(Long employeeId);

    boolean existsByProjectIdAndEmployeeId(
        Long projectId,
        Long employeeId
    );
}
