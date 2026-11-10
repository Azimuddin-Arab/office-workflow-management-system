
package com.example.demo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        String projectName,
        Long assignedEmployeeId,
        String assignedEmployeeName,
        String priority,
        String status,
        LocalDate startDate,
        LocalDate dueDate,
        LocalDateTime createdAt
) {
}
