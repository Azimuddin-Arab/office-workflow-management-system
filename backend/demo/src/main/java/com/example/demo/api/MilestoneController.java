
package com.example.demo.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.MilestoneRequest;
import com.example.demo.dto.MilestoneResponse;
import com.example.demo.model.Milestone;
import com.example.demo.model.Project;
import com.example.demo.repository.MilestoneRepository;
import com.example.demo.repository.ProjectRepository;

@RestController
@RequestMapping("/api/milestones")
public class MilestoneController {

    private final MilestoneRepository milestoneRepository;
    private final ProjectRepository projectRepository;

    public MilestoneController(
            MilestoneRepository milestoneRepository,
            ProjectRepository projectRepository) {

        this.milestoneRepository = milestoneRepository;
        this.projectRepository = projectRepository;
    }

    // Allowed milestone statuses
    private static final List<String> ALLOWED_STATUSES = List.of(
            "PENDING",
            "IN_PROGRESS",
            "COMPLETED",
            "DELAYED"
    );

    // ==========================================
    // GET ALL MILESTONES
    // ==========================================

    @GetMapping
    public ResponseEntity<List<MilestoneResponse>> getAllMilestones() {

        List<MilestoneResponse> milestones = milestoneRepository
                .findAll()
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // GET MILESTONE BY ID
    // ==========================================


    @GetMapping("/{id}")
    public ResponseEntity<?> getMilestoneById(
            @PathVariable Long id) {

        return milestoneRepository.findById(id)
                .<ResponseEntity<?>>map(milestone ->
                        ResponseEntity.ok(toResponse(milestone)))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Milestone not found"));
    }


    // ==========================================
    // GET MILESTONES BY PROJECT
    // ==========================================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<List<MilestoneResponse>> getByProject(
            @PathVariable Long projectId) {

        List<MilestoneResponse> milestones = milestoneRepository
                .findByProjectId(projectId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // GET MILESTONES BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getByStatus(
            @PathVariable String status) {

        String normalizedStatus = status.toUpperCase();

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            return ResponseEntity.badRequest()
                    .body("Invalid milestone status");
        }

        List<MilestoneResponse> milestones = milestoneRepository
                .findByStatusIgnoreCase(normalizedStatus)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // SEARCH MILESTONES BY TITLE
    // ==========================================

    @GetMapping("/search")
    public ResponseEntity<List<MilestoneResponse>> searchMilestones(
            @RequestParam(defaultValue = "") String title) {

        List<MilestoneResponse> milestones = milestoneRepository
                .findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(milestones);
    }

    // ==========================================
    // CREATE MILESTONE
    // ==========================================

    @PostMapping
    public ResponseEntity<?> createMilestone(
            @RequestBody MilestoneRequest request) {

        String validationError = validateRequest(request);

        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(validationError);
        }

        Project project = projectRepository
                .findById(request.projectId())
                .orElse(null);

        if (project == null) {
            return ResponseEntity.badRequest()
                    .body("Selected project does not exist");
        }

        Milestone milestone = new Milestone();

        copyRequest(request, milestone, project);

        Milestone savedMilestone =
                milestoneRepository.save(milestone);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(savedMilestone));
    }

    // ==========================================
    // UPDATE MILESTONE
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateMilestone(
            @PathVariable Long id,
            @RequestBody MilestoneRequest request) {

        String validationError = validateRequest(request);

        if (validationError != null) {
            return ResponseEntity.badRequest()
                    .body(validationError);
        }

        Milestone milestone = milestoneRepository
                .findById(id)
                .orElse(null);

        if (milestone == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Milestone not found");
        }

        Project project = projectRepository
                .findById(request.projectId())
                .orElse(null);

        if (project == null) {
            return ResponseEntity.badRequest()
                    .body("Selected project does not exist");
        }

        copyRequest(request, milestone, project);

        Milestone updatedMilestone =
                milestoneRepository.save(milestone);

        return ResponseEntity.ok(toResponse(updatedMilestone));
    }

    // ==========================================
    // DELETE MILESTONE
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMilestone(
            @PathVariable Long id) {

        if (!milestoneRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Milestone not found");
        }

        milestoneRepository.deleteById(id);

        return ResponseEntity.ok("Milestone deleted successfully");
    }

    // ==========================================
    // VALIDATE REQUEST
    // ==========================================

    private String validateRequest(MilestoneRequest request) {

        if (request == null) {
            return "Request body is required";
        }

        if (request.title() == null ||
                request.title().isBlank()) {
            return "Milestone title is required";
        }

        if (request.title().length() > 200) {
            return "Milestone title cannot exceed 200 characters";
        }

        if (request.description() != null &&
                request.description().length() > 2000) {
            return "Description cannot exceed 2000 characters";
        }

        if (request.projectId() == null) {
            return "Project is required";
        }

        String status = request.status();

        if (status != null && !status.isBlank()) {

            if (!ALLOWED_STATUSES.contains(
                    status.trim().toUpperCase())) {

                return "Invalid status. Allowed values: "
                        + ALLOWED_STATUSES;
            }
        }

        if (request.dueDate() != null &&
                request.completedDate() != null &&
                request.completedDate().isBefore(request.dueDate())) {

            // This is allowed: a milestone can finish before its due date.
        }

        return null;
    }

    // ==========================================
    // COPY REQUEST DATA TO ENTITY
    // ==========================================

    private void copyRequest(
            MilestoneRequest request,
            Milestone milestone,
            Project project) {

        milestone.setTitle(request.title().trim());
        milestone.setDescription(request.description());
        milestone.setProject(project);
        milestone.setDueDate(request.dueDate());

        String status = request.status();

        if (status == null || status.isBlank()) {
            status = "PENDING";
        }

        status = status.trim().toUpperCase();

        milestone.setStatus(status);

        LocalDate completedDate = request.completedDate();

        if ("COMPLETED".equals(status) && completedDate == null) {
            completedDate = LocalDate.now();
        }

        if (!"COMPLETED".equals(status)) {
            completedDate = null;
        }

        milestone.setCompletedDate(completedDate);
    }

    // ==========================================
    // CONVERT ENTITY TO RESPONSE DTO
    // ==========================================

    private MilestoneResponse toResponse(Milestone milestone) {

        return new MilestoneResponse(
                milestone.getId(),
                milestone.getTitle(),
                milestone.getDescription(),
                milestone.getProject().getId(),
                milestone.getProject().getProjectName(),
                milestone.getDueDate(),
                milestone.getCompletedDate(),
                milestone.getStatus(),
                milestone.getCreatedAt()
        );
    }
}
