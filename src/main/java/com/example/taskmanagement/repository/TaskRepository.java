package com.example.taskmanagement.repository;

import com.example.taskmanagement.entity.Task;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Custom query for FR-2.2: staff only see what is assigned to them. */
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Task> findByAssignedToIdOrderById(Long userId);

    @Override
    @EntityGraph(attributePaths = {"createdBy", "assignedTo"})
    List<Task> findAll();
}
