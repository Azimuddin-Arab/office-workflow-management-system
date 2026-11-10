
package com.example.demo.api;

import com.example.demo.dto.PermissionResponse;
import com.example.demo.dto.UserRequest;
import com.example.demo.dto.UserResponse;
import com.example.demo.model.Permission;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.repository.PermissionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class UserManagementController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository,
            PasswordEncoder passwordEncoder) {

        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/users")
    @Transactional(readOnly = true)
    public java.util.List<UserResponse> getAllUsers() {

        return userRepository.findAll()
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    @GetMapping("/users/{id}")
    @Transactional(readOnly = true)
    public UserResponse getUserById(@PathVariable Long id) {
        return toUserResponse(findUser(id));
    }

    @PostMapping("/users")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UserResponse createUser(@RequestBody UserRequest request) {

        String email = normalizeEmail(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Email is already registered"
            );
        }

        if (request.fullName() == null || request.fullName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Full name is required"
            );
        }

        if (request.password() == null || request.password().length() < 8) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must contain at least 8 characters"
            );
        }

        Role role = resolveRole(request.role());

        User user = new User(
                request.fullName().trim(),
                email,
                passwordEncoder.encode(request.password()),
                role
        );

        user.setEnabled(request.enabled() == null || request.enabled());

        return toUserResponse(userRepository.save(user));
    }

    @PutMapping("/users/{id}")
    @Transactional
    public UserResponse updateUser(
            @PathVariable Long id,
            @RequestBody UserRequest request) {

        User user = findUser(id);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.email() != null && !request.email().isBlank()) {

            String email = normalizeEmail(request.email());

            userRepository.findByEmail(email).ifPresent(existing -> {
                if (!existing.getId().equals(id)) {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Email is already used by another user"
                    );
                }
            });

            user.setEmail(email);
        }

        if (request.password() != null && !request.password().isBlank()) {

            if (request.password().length() < 8) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Password must contain at least 8 characters"
                );
            }

            user.setPassword(passwordEncoder.encode(request.password()));
        }

        if (request.role() != null && !request.role().isBlank()) {
            user.setRole(resolveRole(request.role()));
        }

        if (request.enabled() != null) {
            user.setEnabled(request.enabled());
        }

        return toUserResponse(userRepository.save(user));
    }

    @DeleteMapping("/users/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void deleteUser(
            @PathVariable Long id,
            Authentication authentication) {

        User user = findUser(id);

        if (user.getEmail().equalsIgnoreCase(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot delete your own account"
            );
        }

        userRepository.delete(user);
    }

    @GetMapping("/permissions")
    @Transactional(readOnly = true)
    public java.util.List<PermissionResponse> getAllPermissions() {

        return permissionRepository.findAll()
                .stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getCode(),
                        permission.getName(),
                        permission.getModule()
                ))
                .toList();
    }

    @GetMapping("/permissions/module/{module}")
    @Transactional(readOnly = true)
    public java.util.List<PermissionResponse> getPermissionsByModule(
            @PathVariable String module) {

        return permissionRepository.findByModule(module)
                .stream()
                .map(permission -> new PermissionResponse(
                        permission.getId(),
                        permission.getCode(),
                        permission.getName(),
                        permission.getModule()
                ))
                .toList();
    }

    private User findUser(Long id) {

        return userRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "User not found"
                ));
    }

    private String normalizeEmail(String email) {

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Email is required"
            );
        }

        return email.trim().toLowerCase(Locale.ROOT);
    }

    private Role resolveRole(String roleName) {

        if (roleName == null || roleName.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "A role must be selected"
            );
        }

        Role role = roleRepository.findByNameIgnoreCase(roleName.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Role not found: " + roleName
                ));

        if (!role.isEnabled()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "This role is disabled"
            );
        }

        return role;
    }

    private UserResponse toUserResponse(User user) {

        Set<String> permissionCodes = user.getPermissions()
                .stream()
                .map(Permission::getCode)
                .collect(Collectors.toSet());

        String roleName = user.getRole() == null
                ? null
                : user.getRole().getName();

        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                roleName,
                user.isEnabledAccount(),
                permissionCodes
        );
    }
}
