package com.example.demo.api;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.example.demo.model.Department;
import com.example.demo.repository.DepartmentRepository;

@RestController
@RequestMapping("/api/departments")
public class DepartmentController {

    private final DepartmentRepository departmentRepository;

    public DepartmentController(DepartmentRepository departmentRepository) {
        this.departmentRepository = departmentRepository;
    }

    // Get all departments
    @GetMapping
    public List<Department> getAllDepartments() {
        return departmentRepository.findAll();
    }

    // Get department by ID
    @GetMapping("/{id}")
    public Department getDepartment(@PathVariable Long id) {
        return departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"));
    }

    // Create department
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Department createDepartment(@RequestBody Department department) {

        if (departmentRepository.existsByNameIgnoreCase(department.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Department name already exists");
        }

        department.setId(null);
        return departmentRepository.save(department);
    }

    // Update department
    @PutMapping("/{id}")
    public Department updateDepartment(
            @PathVariable Long id,
            @RequestBody Department updatedDepartment) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"));

        boolean duplicate = departmentRepository
                .existsByNameIgnoreCase(updatedDepartment.getName());

        if (duplicate && !department.getName().equalsIgnoreCase(
                updatedDepartment.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Department name already exists");
        }

        department.setName(updatedDepartment.getName());
        department.setDescription(updatedDepartment.getDescription());
        department.setActive(updatedDepartment.isActive());

        return departmentRepository.save(department);
    }

    // Delete department
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDepartment(@PathVariable Long id) {

        Department department = departmentRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Department not found"));

        if (!departmentRepository.existsById(id)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Department not found");
        }

        departmentRepository.delete(department);
    }
}
