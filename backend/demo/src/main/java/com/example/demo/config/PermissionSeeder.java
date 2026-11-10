package com.example.demo.config;

import com.example.demo.model.Permission;
import com.example.demo.repository.PermissionRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class PermissionSeeder {

    @Bean
    CommandLineRunner seedPermissions(
            PermissionRepository permissionRepository) {

        return args -> {

            List<Permission> permissions = List.of(

                // Employees
                new Permission("EMPLOYEE_VIEW", "View Employees", "Employees"),
                new Permission("EMPLOYEE_CREATE", "Create Employees", "Employees"),
                new Permission("EMPLOYEE_UPDATE", "Update Employees", "Employees"),
                new Permission("EMPLOYEE_DELETE", "Delete Employees", "Employees"),

                // Departments
                new Permission("DEPARTMENT_VIEW", "View Departments", "Departments"),
                new Permission("DEPARTMENT_CREATE", "Create Departments", "Departments"),
                new Permission("DEPARTMENT_UPDATE", "Update Departments", "Departments"),
                new Permission("DEPARTMENT_DELETE", "Delete Departments", "Departments"),

                // Projects
                new Permission("PROJECT_VIEW", "View Projects", "Projects"),
                new Permission("PROJECT_CREATE", "Create Projects", "Projects"),
                new Permission("PROJECT_UPDATE", "Update Projects", "Projects"),
                new Permission("PROJECT_DELETE", "Delete Projects", "Projects"),

                // Tasks
                new Permission("TASK_VIEW", "View Tasks", "Tasks"),
                new Permission("TASK_CREATE", "Create Tasks", "Tasks"),
                new Permission("TASK_UPDATE", "Update Tasks", "Tasks"),
                new Permission("TASK_DELETE", "Delete Tasks", "Tasks"),

                // Milestones
                new Permission("MILESTONE_VIEW", "View Milestones", "Milestones"),
                new Permission("MILESTONE_CREATE", "Create Milestones", "Milestones"),
                new Permission("MILESTONE_UPDATE", "Update Milestones", "Milestones"),
                new Permission("MILESTONE_DELETE", "Delete Milestones", "Milestones"),

                // Calendar
                new Permission("CALENDAR_VIEW", "View Calendar", "Calendar"),
                new Permission("CALENDAR_CREATE", "Create Events", "Calendar"),
                new Permission("CALENDAR_UPDATE", "Update Events", "Calendar"),
                new Permission("CALENDAR_DELETE", "Delete Events", "Calendar"),

                // Reports
                new Permission("REPORT_VIEW", "View Reports", "Reports"),
                new Permission("REPORT_EXPORT", "Export Reports", "Reports"),

                // User management
                new Permission("USER_VIEW", "View Users", "Users"),
                new Permission("USER_CREATE", "Create Users", "Users"),
                new Permission("USER_UPDATE", "Update Users", "Users"),
                new Permission("USER_DELETE", "Delete Users", "Users")
            );

            for (Permission permission : permissions) {

                if (!permissionRepository.existsByCode(
                        permission.getCode())) {

                    permissionRepository.save(permission);
                }
            }

            System.out.println("Permission seeding completed.");
        };
    }
}