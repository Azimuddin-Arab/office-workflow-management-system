package com.example.demo.api;


import com.example.demo.dto.TaskRequest;
import com.example.demo.dto.TaskResponse;
import com.example.demo.model.Employee;
import com.example.demo.model.Project;
import com.example.demo.model.Task;
import com.example.demo.repository.EmployeeRepository;
import com.example.demo.repository.ProjectRepository;
import com.example.demo.repository.ProjectTeamRepository;
import com.example.demo.repository.TaskRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskRepository taskRepository;
    private final ProjectRepository projectRepository;
    private final EmployeeRepository employeeRepository;
    private final ProjectTeamRepository projectTeamRepository;

    private static final Set<String> PRIORITIES =
            Set.of("LOW", "MEDIUM", "HIGH", "CRITICAL");

    private static final Set<String> STATUSES =
            Set.of("TODO", "IN_PROGRESS", "REVIEW", "COMPLETED");

    public TaskController(
            TaskRepository taskRepository,
            ProjectRepository projectRepository,
            EmployeeRepository employeeRepository,
            ProjectTeamRepository projectTeamRepository) {

        this.taskRepository = taskRepository;
        this.projectRepository = projectRepository;
        this.employeeRepository = employeeRepository;
        this.projectTeamRepository = projectTeamRepository;
    }

    // 1. Get all tasks
    @GetMapping
    public List<TaskResponse> getAllTasks() {

        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 2. Get task by ID
    @GetMapping("/{id}")
    public ResponseEntity<?> getTaskById(@PathVariable Long id) {

        return taskRepository.findById(id)
                .<ResponseEntity<?>>map(task ->
                        ResponseEntity.ok(toResponse(task)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    // 3. Get tasks by project
    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getTasksByProject(
            @PathVariable Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        List<TaskResponse> tasks =
                taskRepository.findByProjectId(projectId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(tasks);
    }

    // 4. Get tasks assigned to employee
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<?> getTasksByEmployee(
            @PathVariable Long employeeId) {

        if (!employeeRepository.existsById(employeeId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Employee not found");
        }

        List<TaskResponse> tasks =
                taskRepository.findByAssignedEmployeeId(employeeId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(tasks);
    }

    // 5. Filter tasks by status
    @GetMapping("/status/{status}")
    public ResponseEntity<?> getTasksByStatus(
            @PathVariable String status) {

        if (!STATUSES.contains(status.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body("Invalid task status");
        }

        List<TaskResponse> tasks =
                taskRepository.findByStatusIgnoreCase(status)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(tasks);
    }

    // 6. Filter tasks by priority
    @GetMapping("/priority/{priority}")
    public ResponseEntity<?> getTasksByPriority(
            @PathVariable String priority) {

        if (!PRIORITIES.contains(priority.toUpperCase())) {
            return ResponseEntity.badRequest()
                    .body("Invalid task priority");
        }

        List<TaskResponse> tasks =
                taskRepository.findByPriorityIgnoreCase(priority)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(tasks);
    }

    // 7. Search tasks by title
    @GetMapping("/search")
    public List<TaskResponse> searchTasks(
            @RequestParam String title) {

        return taskRepository
                .findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // 8. Create task
    @PostMapping
    public ResponseEntity<?> createTask(
            @RequestBody TaskRequest request) {

        ResponseEntity<String> validation = validateRequest(request);

        if (validation != null) {
            return ResponseEntity.badRequest()
                    .body(validation.getBody());
        }

        var projectOptional =
                projectRepository.findById(request.projectId());

        if (projectOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        Employee employee = null;

        if (request.assignedEmployeeId() != null) {

            var employeeOptional =
                    employeeRepository.findById(
                            request.assignedEmployeeId());

            if (employeeOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Employee not found");
            }

            boolean isTeamMember =
                    projectTeamRepository
                            .existsByProjectIdAndEmployeeId(
                                    request.projectId(),
                                    request.assignedEmployeeId());

            if (!isTeamMember) {
                return ResponseEntity.badRequest()
                        .body("Employee is not a member of this project team");
            }

            employee = employeeOptional.get();
        }

        Task task = new Task();

        copyRequest(task, request, projectOptional.get(), employee);

        Task saved = taskRepository.save(task);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(saved));
    }

    // 9. Update task
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long id,
            @RequestBody TaskRequest request) {

        var taskOptional = taskRepository.findById(id);

        if (taskOptional.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ResponseEntity<String> validation = validateRequest(request);

        if (validation != null) {
            return ResponseEntity.badRequest()
                    .body(validation.getBody());
        }

        var projectOptional =
                projectRepository.findById(request.projectId());

        if (projectOptional.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        Employee employee = null;

        if (request.assignedEmployeeId() != null) {

            var employeeOptional =
                    employeeRepository.findById(
                            request.assignedEmployeeId());

            if (employeeOptional.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Employee not found");
            }

            boolean isTeamMember =
                    projectTeamRepository
                            .existsByProjectIdAndEmployeeId(
                                    request.projectId(),
                                    request.assignedEmployeeId());

            if (!isTeamMember) {
                return ResponseEntity.badRequest()
                        .body("Employee is not a member of this project team");
            }

            employee = employeeOptional.get();
        }

        Task task = taskOptional.get();

        copyRequest(task, request, projectOptional.get(), employee);

        Task updated = taskRepository.save(task);

        return ResponseEntity.ok(toResponse(updated));
    }

    // 10. Delete task
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {

        if (!taskRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        taskRepository.deleteById(id);

        return ResponseEntity.noContent().build();
    }

    // Validate task fields
    private ResponseEntity<String> validateRequest(
            TaskRequest request) {

        if (request == null ||
                request.title() == null ||
                request.title().isBlank() ||
                request.projectId() == null) {

            return ResponseEntity.badRequest()
                    .body("Task title and project are required");
        }

        String priority = request.priority() == null
                ? "MEDIUM"
                : request.priority().toUpperCase();

        String status = request.status() == null
                ? "TODO"
                : request.status().toUpperCase();

        if (!PRIORITIES.contains(priority)) {
            return ResponseEntity.badRequest()
                    .body("Invalid priority. Use LOW, MEDIUM, HIGH or CRITICAL");
        }

        if (!STATUSES.contains(status)) {
            return ResponseEntity.badRequest()
                    .body("Invalid status. Use TODO, IN_PROGRESS, REVIEW or COMPLETED");
        }

        if (request.startDate() != null &&
                request.dueDate() != null &&
                request.dueDate().isBefore(request.startDate())) {

            return ResponseEntity.badRequest()
                    .body("Due date cannot be before start date");
        }

        return null;
    }

    // Copy request values into entity
    private void copyRequest(
            Task task,
            TaskRequest request,
            Project project,
            Employee employee) {

        task.setTitle(request.title().trim());
        task.setDescription(request.description());
        task.setProject(project);
        task.setAssignedEmployee(employee);

        task.setPriority(
                request.priority() == null ||
                        request.priority().isBlank()
                        ? "MEDIUM"
                        : request.priority().toUpperCase()
        );

        task.setStatus(
                request.status() == null ||
                        request.status().isBlank()
                        ? "TODO"
                        : request.status().toUpperCase()
        );

        task.setStartDate(request.startDate());
        task.setDueDate(request.dueDate());
    }

    // Convert entity to response DTO
    private TaskResponse toResponse(Task task) {

        Project project = task.getProject();
        Employee employee = task.getAssignedEmployee();

        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                project.getId(),
                project.getProjectName(),
                employee != null ? employee.getId() : null,
                employee != null ? employee.getFullName() : null,
                task.getPriority(),
                task.getStatus(),
                task.getStartDate(),
                task.getDueDate(),
                task.getCreatedAt()
        );
    }
}
