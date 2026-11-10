
package com.example.demo.dto;

import java.time.LocalDate;

public record MilestoneRequest(
        String title,
        String description,
        Long projectId,
        LocalDate dueDate,
        LocalDate completedDate,
        String status
) {
}
