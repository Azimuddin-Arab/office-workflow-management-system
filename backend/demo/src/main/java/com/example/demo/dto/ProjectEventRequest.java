
package com.example.demo.dto;

import java.time.LocalDateTime;

public record ProjectEventRequest(
        String title,
        String description,
        Long projectId,
        LocalDateTime startDateTime,
        LocalDateTime endDateTime,
        String eventType,
        String status
) {
}
