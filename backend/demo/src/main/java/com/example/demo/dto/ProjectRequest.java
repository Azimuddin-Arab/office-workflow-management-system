
package com.example.demo.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProjectRequest(
        String projectName,
        String projectCode,
        String description,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        BigDecimal budget,
        Long managerId
) {
}
