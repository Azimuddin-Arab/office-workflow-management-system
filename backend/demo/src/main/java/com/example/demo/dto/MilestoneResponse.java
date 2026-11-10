
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record MilestoneResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        String projectName,
        LocalDate dueDate,
        LocalDate completedDate,
        String status,
        LocalDateTime createdAt
) {
}
