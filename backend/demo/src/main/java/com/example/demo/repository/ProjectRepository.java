
package com.example.demo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.demo.model.Project;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {

    boolean existsByProjectCodeIgnoreCase(String projectCode);

    List<Project> findByProjectNameContainingIgnoreCase(String projectName);

    List<Project> findByStatusIgnoreCase(String status);
}
