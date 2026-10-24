package com.example.taskmanagement.dto;

import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskStatus;

public record TaskResponse(Long id, String title, String description, TaskStatus status,
                           Long createdById, Long assignedToId) {

    public static TaskResponse from(Task task) {
        return new TaskResponse(task.getId(), task.getTitle(), task.getDescription(), task.getStatus(),
                task.getCreatedBy().getId(),
                task.getAssignedTo() == null ? null : task.getAssignedTo().getId());
    }
}
