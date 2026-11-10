
package com.example.demo.dto;

import java.time.LocalDateTime;

public record ProjectEventResponse(
        Long id,
        String title,
        String description,
        Long projectId,
        String projectName,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String eventType,
        String status,
        LocalDateTime createdAt
) {
}
