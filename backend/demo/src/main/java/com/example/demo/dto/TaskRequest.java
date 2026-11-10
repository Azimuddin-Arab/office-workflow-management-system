
package com.example.demo.dto;

import java.time.LocalDate;

public record TaskRequest(
        String title,
        String description,
        Long projectId,
        Long assignedEmployeeId,
        String priority,
        String status,
        LocalDate startDate,
        LocalDate dueDate
) {
}
