
package com.example.demo.api;

import com.example.demo.dto.ProjectEventRequest;
import com.example.demo.dto.ProjectEventResponse;
import com.example.demo.model.Project;
import com.example.demo.model.ProjectEvent;
import com.example.demo.repository.ProjectEventRepository;
import com.example.demo.repository.ProjectRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/project-events")
public class ProjectEventController {

    private final ProjectEventRepository eventRepository;
    private final ProjectRepository projectRepository;

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "MEETING",
            "REVIEW",
            "PRESENTATION",
            "DEADLINE",
            "OTHER"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "SCHEDULED",
            "COMPLETED",
            "CANCELLED"
    );

    public ProjectEventController(
            ProjectEventRepository eventRepository,
            ProjectRepository projectRepository) {

        this.eventRepository = eventRepository;
        this.projectRepository = projectRepository;
    }

    // ==========================================
    // GET ALL EVENTS
    // ==========================================

    @GetMapping
    public List<ProjectEventResponse> getAllEvents() {

        return eventRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ==========================================
    // GET EVENT BY ID
    // ==========================================

    @GetMapping("/{id}")
    public ResponseEntity<?> getEventById(@PathVariable Long id) {

        return eventRepository.findById(id)
                .<ResponseEntity<?>>map(event ->
                        ResponseEntity.ok(toResponse(event)))
                .orElseGet(() ->
                        ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body("Project event not found"));
    }

    // ==========================================
    // GET EVENTS BY PROJECT
    // ==========================================

    @GetMapping("/project/{projectId}")
    public ResponseEntity<?> getEventsByProject(
            @PathVariable Long projectId) {

        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        List<ProjectEventResponse> events =
                eventRepository.findByProjectId(projectId)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(events);
    }

    // ==========================================
    // GET EVENTS BY STATUS
    // ==========================================

    @GetMapping("/status/{status}")
    public ResponseEntity<?> getEventsByStatus(
            @PathVariable String status) {

        String normalizedStatus = status.toUpperCase();

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            return ResponseEntity.badRequest()
                    .body("Invalid event status");
        }

        return ResponseEntity.ok(
                eventRepository.findByStatusIgnoreCase(normalizedStatus)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    // ==========================================
    // GET EVENTS BY TYPE
    // ==========================================

    @GetMapping("/type/{eventType}")
    public ResponseEntity<?> getEventsByType(
            @PathVariable String eventType) {

        String normalizedType = eventType.toUpperCase();

        if (!ALLOWED_TYPES.contains(normalizedType)) {
            return ResponseEntity.badRequest()
                    .body("Invalid event type");
        }

        return ResponseEntity.ok(
                eventRepository.findByEventTypeIgnoreCase(normalizedType)
                        .stream()
                        .map(this::toResponse)
                        .toList()
        );
    }

    // ==========================================
    // GET EVENTS BY DATE RANGE
    // ==========================================

    @GetMapping("/calendar")
    public ResponseEntity<?> getCalendarEvents(
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {

        if (!isValidDateRange(start, end)) {
            return ResponseEntity.badRequest()
                    .body("Start date must be before end date");
        }

        List<ProjectEventResponse> events =
                eventRepository.findByStartDateTimeBetween(start, end)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(events);
    }

    // ==========================================
    // GET EVENTS BY PROJECT AND DATE RANGE
    // ==========================================

    @GetMapping("/calendar/project/{projectId}")
    public ResponseEntity<?> getProjectCalendarEvents(
            @PathVariable Long projectId,
            @RequestParam LocalDateTime start,
            @RequestParam LocalDateTime end) {

        if (!projectRepository.existsById(projectId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        if (!isValidDateRange(start, end)) {
            return ResponseEntity.badRequest()
                    .body("Start date must be before end date");
        }

        List<ProjectEventResponse> events =
                eventRepository
                        .findByProjectIdAndStartDateTimeBetween(
                                projectId, start, end)
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return ResponseEntity.ok(events);
    }

    // ==========================================
    // SEARCH EVENTS BY TITLE
    // ==========================================

    @GetMapping("/search")
    public List<ProjectEventResponse> searchEvents(
            @RequestParam String title) {

        return eventRepository
                .findByTitleContainingIgnoreCase(title)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ==========================================
    // CREATE EVENT
    // ==========================================

    @PostMapping
    public ResponseEntity<?> createEvent(
            @RequestBody ProjectEventRequest request) {

        ResponseEntity<String> validation = validateRequest(request);

        if (validation != null) {
            return ResponseEntity.badRequest()
                    .body(validation.getBody());
        }

        Project project = projectRepository
                .findById(request.projectId())
                .orElse(null);

        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        ProjectEvent event = new ProjectEvent();

        copyRequest(event, request, project);

        ProjectEvent savedEvent = eventRepository.save(event);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toResponse(savedEvent));
    }

    // ==========================================
    // UPDATE EVENT
    // ==========================================

    @PutMapping("/{id}")
    public ResponseEntity<?> updateEvent(
            @PathVariable Long id,
            @RequestBody ProjectEventRequest request) {

        ProjectEvent event = eventRepository.findById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project event not found");
        }

        ResponseEntity<String> validation = validateRequest(request);

        if (validation != null) {
            return ResponseEntity.badRequest()
                    .body(validation.getBody());
        }

        Project project = projectRepository
                .findById(request.projectId())
                .orElse(null);

        if (project == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project not found");
        }

        copyRequest(event, request, project);

        ProjectEvent updatedEvent = eventRepository.save(event);

        return ResponseEntity.ok(toResponse(updatedEvent));
    }

    // ==========================================
    // DELETE EVENT
    // ==========================================

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteEvent(@PathVariable Long id) {

        ProjectEvent event = eventRepository.findById(id)
                .orElse(null);

        if (event == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Project event not found");
        }

        eventRepository.delete(event);

        return ResponseEntity.ok("Project event deleted successfully");
    }

    // ==========================================
    // VALIDATE REQUEST
    // ==========================================

    private ResponseEntity<String> validateRequest(
            ProjectEventRequest request) {

        if (request == null) {
            return ResponseEntity.badRequest()
                    .body("Request body is required");
        }

        if (request.title() == null ||
                request.title().isBlank()) {

            return ResponseEntity.badRequest()
                    .body("Event title is required");
        }

        if (request.title().length() > 200) {
            return ResponseEntity.badRequest()
                    .body("Event title cannot exceed 200 characters");
        }

        if (request.description() != null &&
                request.description().length() > 2000) {

            return ResponseEntity.badRequest()
                    .body("Description cannot exceed 2000 characters");
        }

        if (request.projectId() == null) {
            return ResponseEntity.badRequest()
                    .body("Project is required");
        }

        if (request.startDateTime() == null ||
                request.endDateTime() == null) {

            return ResponseEntity.badRequest()
                    .body("Event start and end dates are required");
        }

        if (!request.endDateTime().isAfter(request.startDateTime())) {
            return ResponseEntity.badRequest()
                    .body("End date and time must be after start date and time");
        }

        String eventType = request.eventType() == null
                ? "OTHER"
                : request.eventType().toUpperCase();

        if (!ALLOWED_TYPES.contains(eventType)) {
            return ResponseEntity.badRequest()
                    .body("Invalid event type");
        }

        String status = request.status() == null
                ? "SCHEDULED"
                : request.status().toUpperCase();

        if (!ALLOWED_STATUSES.contains(status)) {
            return ResponseEntity.badRequest()
                    .body("Invalid event status");
        }

        return null;
    }

    // ==========================================
    // COPY REQUEST TO ENTITY
    // ==========================================

    private void copyRequest(
            ProjectEvent event,
            ProjectEventRequest request,
            Project project) {

        event.setTitle(request.title().trim());
        event.setDescription(request.description());
        event.setProject(project);

        event.setStartDateTime(request.startDateTime());
        event.setEndDateTime(request.endDateTime());

        event.setEventType(
                request.eventType() == null ||
                        request.eventType().isBlank()
                        ? "OTHER"
                        : request.eventType().toUpperCase()
        );

        event.setStatus(
                request.status() == null ||
                        request.status().isBlank()
                        ? "SCHEDULED"
                        : request.status().toUpperCase()
        );
    }

    // ==========================================
    // ENTITY TO RESPONSE DTO
    // ==========================================

    private ProjectEventResponse toResponse(ProjectEvent event) {

        return new ProjectEventResponse(
                event.getId(),
                event.getTitle(),
                event.getDescription(),
                event.getProject().getId(),
                event.getProject().getProjectName(),
                event.getStartDateTime(),
                event.getEndDateTime(),
                event.getEventType(),
                event.getStatus(),
                event.getCreatedAt()
        );
    }

    // ==========================================
    // DATE RANGE VALIDATION
    // ==========================================

    private boolean isValidDateRange(
            LocalDateTime start,
            LocalDateTime end) {

        return start != null &&
                end != null &&
                end.isAfter(start);
    }
}
