package com.example.taskmanagement.dto;

import com.example.taskmanagement.entity.TaskStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateTaskStatusRequest(@NotNull TaskStatus status) {
}
