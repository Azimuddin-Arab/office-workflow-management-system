
package com.example.demo.api;

import com.example.demo.dto.ProjectTeamRequest;
import com.example.demo.dto.ProjectTeamResponse;
import com.example.demo.model.Employee;
import com.example.demo.model.Project;
import com.example.demo.model.ProjectTeam;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.ProjectTeamRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/project-teams")
public class ProjectTeamController {

    private final ProjectTeamRepository projectTeamRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;

    public ProjectTeamController(
            ProjectTeamRepository projectTeamRepository,
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository) {

        this.projectTeamRepository = projectTeamRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
    }

    // 1. Get all project team members
    @GetMapping
    public List<ProjectTeamResponse> getAllMembers() {

        return projectTeamRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 2. Get all members of a specific project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getProjectMembers(
            @PathVariable Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        List<ProjectTeamResponse> members =
                projectTeamRepository.findByProjectId(projectId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(members);
    }

    // 3. Get all projects assigned to an employee
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getEmployeeProjects(
            @PathVariable Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Employee not found");
        }

        List<ProjectTeamResponse> teams =
                projectTeamRepository.findByEmployeeId(employeeId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(teams);
    }

    // 4. Get one team membership by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getMemberById(@PathVariable Long id) {

        return projectTeamRepository.findById(id)
                .map(member -> ResponseEntity.ok(toResponse(member)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 5. Add an employee to a project
    @PostMapping
    public ResponseEntity<?> addMember(
            @RequestBody ProjectTeamRequest request) {

        if (request.projectId() == null ||
                request.employeeId() == null ||
                request.teamRole() == null ||
                request.teamRole().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Project, employee and team role are required");
        }

        var projectOptional =
                projectRepository.findById(request.projectId());

        if (projectOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        var employeeOptional =
                employeeRepository.findById(request.employeeId());

        if (employeeOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Employee not found");
        }

        boolean alreadyAssigned =
                projectTeamRepository.existsByProjectIdAndEmployeeId(
                        request.projectId(),
                        request.employeeId()
                );

        if (alreadyAssigned) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This employee is already assigned to this project");
        }

        ProjectTeam member = new ProjectTeam();

        member.setProject(projectOptional.get());
        member.setEmployee(employeeOptional.get());
        member.setTeamRole(request.teamRole().trim());

        member.setJoinedDate(
                request.joinedDate() != null
                        ? request.joinedDate()
                        : LocalDate.now()
        );

        ProjectTeam saved = projectTeamRepository.save(member);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    // 6. Update a project team membership
    @PutMapping("/{id}")
    public ResponseEntity<?> updateMember(
            @PathVariable Long id,
            @RequestBody ProjectTeamRequest request) {

        var memberOptional = projectTeamRepository.findById(id);

        if (memberOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        if (request.projectId() == null ||
                request.employeeId() == null ||
                request.teamRole() == null ||
                request.teamRole().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Project, employee and team role are required");
        }

        ProjectTeam member = memberOptional.get();

        var projectOptional =
                projectRepository.findById(request.projectId());

        if (projectOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        var employeeOptional =
                employeeRepository.findById(request.employeeId());

        if (employeeOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Employee not found");
        }

        boolean assignmentChanged =
                !member.getProject().getId().equals(request.projectId()) ||
                !member.getEmployee().getId().equals(request.employeeId());

        if (assignmentChanged &&
                projectTeamRepository.existsByProjectIdAndEmployeeId(
                        request.projectId(),
                        request.employeeId())) {

            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("This employee is already assigned to this project");
        }

        member.setProject(projectOptional.get());
        member.setEmployee(employeeOptional.get());
        member.setTeamRole(request.teamRole().trim());

        if (request.joinedDate() != null) {
            member.setJoinedDate(request.joinedDate());
        }

        ProjectTeam updated = projectTeamRepository.save(member);

        return ResponseEntity.ok(toResponse(updated));
    }

    // 7. Remove an employee from a project
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long id) {

        if (!projectTeamRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        projectTeamRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    // Convert entity to response DTO
    private ProjectTeamResponse toResponse(ProjectTeam member) {

        Project project = member.getProject();
        Employee employee = member.getEmployee();

        return new ProjectTeamResponse(
                member.getId(),
                project.getId(),
                project.getProjectName(),
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                member.getTeamRole(),
                member.getJoinedDate()
        );
    }
}
