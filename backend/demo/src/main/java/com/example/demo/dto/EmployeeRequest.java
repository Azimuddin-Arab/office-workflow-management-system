
package com.example.demo.dto;

public record EmployeeRequest(
        String fullName,
        String email,
        String phone,
        String jobTitle,
        String status,
        Long departmentId
) {
}
