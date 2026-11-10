
package com.example.demo.dto;

import java.util.Set;

public record RoleRequest(
    String name,
    String description,
    Boolean enabled,
    Set<Long> permissionIds
) {
}
