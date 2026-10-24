package com.example.taskmanagement.service;

import com.example.taskmanagement.dto.CreateTaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.UpdateTaskRequest;
import com.example.taskmanagement.entity.Task;
import com.example.taskmanagement.entity.TaskStatus;
import com.example.taskmanagement.entity.User;
import com.example.taskmanagement.repository.TaskRepository;
import com.example.taskmanagement.repository.UserRepository;
import com.example.taskmanagement.security.UserPrincipal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskService {

    private static final String ROLE_STAFF = "ROLE_STAFF";

    private final TaskRepository taskRepository;
    private final UserRepository userRepository;

    public TaskService(TaskRepository taskRepository, UserRepository userRepository) {
        this.taskRepository = taskRepository;
        this.userRepository = userRepository;
    }

    /** FR-2.2: ADMIN/MANAGER see everything, STAFF only their own assignments. */
    @Transactional(readOnly = true)
    public List<TaskResponse> list(UserPrincipal caller) {
        List<Task> tasks = isStaff(caller)
                ? taskRepository.findByAssignedToIdOrderById(caller.id())
                : taskRepository.findAll();
        return tasks.stream().map(TaskResponse::from).toList();
    }

    @Transactional
    public TaskResponse create(CreateTaskRequest request, UserPrincipal caller) {
        User creator = userRepository.getReferenceById(caller.id());
        User assignee = userRepository.findById(request.assignedToId())
                .orElseThrow(() -> notFound("Assignee not found: " + request.assignedToId()));
        Task task = new Task(request.title(), request.description(), creator, assignee);
        return TaskResponse.from(taskRepository.save(task));
    }

    /** FR-2.3: STAFF may only touch tasks assigned to them, and only the status. */
    @Transactional
    public TaskResponse updateStatus(Long id, TaskStatus status, UserPrincipal caller) {
        Task task = find(id);
        if (isStaff(caller) && !isAssignee(task, caller)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Staff may only update tasks assigned to them");
        }
        task.setStatus(status);
        return TaskResponse.from(task);
    }

    @Transactional
    public TaskResponse update(Long id, UpdateTaskRequest request) {
        Task task = find(id);
        task.setTitle(request.title());
        task.setDescription(request.description());
        return TaskResponse.from(task);
    }

    @Transactional
    public void delete(Long id) {
        if (!taskRepository.existsById(id)) {
            throw notFound("Task not found: " + id);
        }
        taskRepository.deleteById(id);
    }

    private Task find(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> notFound("Task not found: " + id));
    }

    private static boolean isStaff(UserPrincipal caller) {
        return caller.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(ROLE_STAFF));
    }

    private static boolean isAssignee(Task task, UserPrincipal caller) {
        return task.getAssignedTo() != null && task.getAssignedTo().getId().equals(caller.id());
    }

    private static ResponseStatusException notFound(String message) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, message);
    }
}
