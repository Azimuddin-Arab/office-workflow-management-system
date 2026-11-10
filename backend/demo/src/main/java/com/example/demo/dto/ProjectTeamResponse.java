
package com.example.demo.dto;

import java.time.LocalDate;

public record ProjectTeamResponse(
        Long id,
        Long projectId,
        String projectName,
        Long employeeId,
        String employeeName,
        String employeeEmail,
        String teamRole,
        LocalDate joinedDate
) {
}
