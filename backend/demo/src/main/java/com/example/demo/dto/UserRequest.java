
package com.example.demo.dto;

public record UserRequest(
    String fullName,
    String email,
    String password,
    String role,
    Boolean enabled
) {
}
