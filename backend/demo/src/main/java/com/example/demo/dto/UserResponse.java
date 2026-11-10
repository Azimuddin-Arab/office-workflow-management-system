
package com.example.demo.dto;

import java.util.Set;

public record UserResponse(
    Long id,
    String fullName,
    String email,
    String role,
    boolean enabled,
    Set<String> permissions
) {
}
