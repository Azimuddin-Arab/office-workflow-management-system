package com.example.demo.api;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.dto.ProjectRequest;
import com.example.demo.model.Employee;
import com.example.demo.model.Project;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.ProjectRepository;

@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public ProjectController(
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository) {

        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    // Get all projects
    @GetMapping
    public List<Project> getAllProjects() {
        return projectRepository.findAll();
    }

    // Get project by ID
    @GetMapping("/{id}")
    public Project getProject(@PathVariable Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));
    }

    // Create project
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Project createProject(@RequestBody ProjectRequest request) {

        validateProject(request);

        if (projectRepository.existsByProjectCodeIgnoreCase(
                request.projectCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Project code already exists");
        }

        Project project = new Project();
        copyRequest(project, request);

        return projectRepository.save(project);
    }

    // Update project
    @PutMapping("/{id}")
    public Project updateProject(
            @PathVariable Long id,
            @RequestBody ProjectRequest request) {

        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Project not found"));

        validateProject(request);

        boolean duplicateCode =
                projectRepository.existsByProjectCodeIgnoreCase(
                        request.projectCode());

        if (duplicateCode &&
                !project.getProjectCode().equalsIgnoreCase(
                        request.projectCode())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Project code already exists");
        }

        copyRequest(project, request);

        return projectRepository.save(project);
    }

    // Delete project
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {

        if (!projectRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Project not found");
        }

        projectRepository.deleteById(id);
    }

    // Search projects by name
    @GetMapping("/search")
    public List<Project> searchProjects(
            @RequestParam String name) {

        return projectRepository
                .findByProjectNameContainingIgnoreCase(name);
    }

    // Filter projects by status
    @GetMapping("/status/{status}")
    public List<Project> getProjectsByStatus(
            @PathVariable String status) {

        return projectRepository.findByStatusIgnoreCase(status);
    }

    // Validate project dates and required fields
    private void validateProject(ProjectRequest request) {

        if (request.projectName() == null ||
                request.projectName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Project name is required");
        }

        if (request.projectCode() == null ||
                request.projectCode().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Project code is required");
        }

        if (request.startDate() != null &&
                request.endDate() != null &&
                request.endDate().isBefore(request.startDate())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "End date cannot be before start date");
        }

        if (request.status() != null &&
                !List.of("PLANNING", "IN_PROGRESS", "ON_HOLD",
                        "COMPLETED", "CANCELLED")
                        .contains(request.status())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid project status");
        }
    }

    // Reusable method for create and update
    private void copyRequest(
            Project project,
            ProjectRequest request) {

        project.setProjectName(request.projectName().trim());
        project.setProjectCode(request.projectCode().trim());
        project.setDescription(request.description());
        project.setStartDate(request.startDate());
        project.setEndDate(request.endDate());
        project.setStatus(request.status() == null ||
                request.status().isBlank()
                ? "PLANNING" : request.status());
        project.setBudget(request.budget());

        if (request.managerId() == null) {
            project.setProjectManager(null);
        } else {
            Employee manager = employeeRepository
                    .findById(request.managerId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Project manager employee not found"));

            project.setProjectManager(manager);
        }
    }
}
