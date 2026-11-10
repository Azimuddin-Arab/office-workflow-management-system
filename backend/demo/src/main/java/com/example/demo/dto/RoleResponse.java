
package com.example.demo.dto;

import java.util.Set;

public record RoleResponse(
    Long id,
    String name,
    String description,
    boolean enabled,
    Set<Long> permissionIds,
    Set<String> permissionCodes
) {
}
