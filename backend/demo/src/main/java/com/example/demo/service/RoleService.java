
package com.example.demo.service;

import com.example.demo.dto.RoleRequest;
import com.example.demo.dto.RoleResponse;
import com.example.demo.model.Permission;
import com.example.demo.model.Role;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRepository userRepository;

    public RoleService(
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            UserRepository userRepository) {

        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public java.util.List<RoleResponse> getAllRoles() {

        return roleRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RoleResponse getRoleById(Long id) {
        return toResponse(findRole(id));
    }

    @Transactional
    public RoleResponse createRole(RoleRequest request) {

        validateName(request.name());

        String name = request.name().trim();

        if (roleRepository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Role name already exists"
            );
        }

        Role role = new Role(
                name,
                request.description()
        );

        role.setEnabled(
                request.enabled() == null || request.enabled()
        );

        role.setPermissions(resolvePermissions(request.permissionIds()));

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public RoleResponse updateRole(Long id, RoleRequest request) {

        Role role = findRole(id);

        if (request.name() != null && !request.name().isBlank()) {

            String name = request.name().trim();

            roleRepository.findByNameIgnoreCase(name).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Role name already exists"
                    );
                }
            });

            role.setName(name);
        }

        if (request.description() != null) {
            role.setDescription(request.description());
        }

        if (request.enabled() != null) {

            // Prevent disabling a role that is still assigned to users.
            if (!request.enabled() &&
                    userRepository.existsByRole_Id(id)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "This role is assigned to users. Reassign those users before disabling it."
                );
            }

            role.setEnabled(request.enabled());
        }

        if (request.permissionIds() != null) {
            role.setPermissions(
                    resolvePermissions(request.permissionIds())
            );
        }

        return toResponse(roleRepository.save(role));
    }

    @Transactional
    public void deleteRole(Long id) {

        Role role = findRole(id);

        if (userRepository.existsByRole_Id(id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cannot delete a role assigned to users"
            );
        }

        if ("ADMIN".equalsIgnoreCase(role.getName()) ||
                "USER".equalsIgnoreCase(role.getName())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Default system roles cannot be deleted"
            );
        }

        roleRepository.delete(role);
    }

    private Role findRole(Long id) {

        return roleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Role not found"
                ));
    }

    private Set<Permission> resolvePermissions(Set<Long> permissionIds) {

        if (permissionIds == null || permissionIds.isEmpty()) {
            return new java.util.HashSet<>();
        }

        Set<Permission> permissions =
                permissionRepository.findAllById(permissionIds)
                        .stream()
                        .collect(Collectors.toSet());

        if (permissions.size() != permissionIds.size()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "One or more permissions were not found"
            );
        }

        return permissions;
    }

    private void validateName(String name) {

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Role name is required"
            );
        }
    }

    private RoleResponse toResponse(Role role) {

        Set<Long> permissionIds = role.getPermissions()
                .stream()
                .map(Permission::getId)
                .collect(Collectors.toSet());

        Set<String> permissionCodes = role.getPermissions()
                .stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        return new RoleResponse(
                role.getId(),
                role.getName(),
                role.getDescription(),
                role.isEnabled(),
                permissionIds,
                permissionCodes
        );
    }
}
