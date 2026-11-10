
package com.example.demo.repository;

import com.example.demo.model.Task;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TaskRepository extends JpaRepository<Task, Long> {

    // Get all tasks belonging to a project
    List<Task> findByProjectId(Long projectId);

    // Get all tasks assigned to an employee
    List<Task> findByAssignedEmployeeId(Long employeeId);

    // Filter tasks by status
    List<Task> findByStatusIgnoreCase(String status);

    // Filter tasks by priority
    List<Task> findByPriorityIgnoreCase(String priority);

    // Search tasks by title
    List<Task> findByTitleContainingIgnoreCase(String title);

    // Check whether an employee is assigned to a task
    boolean existsByIdAndAssignedEmployeeId(
            Long id,
            Long employeeId
    );
}
