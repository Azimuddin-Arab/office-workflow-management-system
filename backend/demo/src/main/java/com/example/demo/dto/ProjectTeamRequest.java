
package com.example.demo.dto;

import java.time.LocalDate;

public record ProjectTeamRequest(
    Long projectId,
    Long employeeId,
    String teamRole,
    LocalDate joinedDate
) {
}
