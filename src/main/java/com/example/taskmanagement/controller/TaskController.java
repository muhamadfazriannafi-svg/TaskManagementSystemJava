package com.example.taskmanagement.controller;

import com.example.taskmanagement.advice.ApiError;
import com.example.taskmanagement.dto.CreateTaskRequest;
import com.example.taskmanagement.dto.TaskResponse;
import com.example.taskmanagement.dto.UpdateTaskRequest;
import com.example.taskmanagement.dto.UpdateTaskStatusRequest;
import com.example.taskmanagement.security.UserPrincipal;
import com.example.taskmanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Manajemen tugas; hasil GET terfilter berdasarkan role")
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Lihat daftar tugas (terfilter)",
            description = "ADMIN/MANAGER melihat semua tugas, STAFF hanya yang di-assign kepadanya.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Daftar tugas"),
            @ApiResponse(responseCode = "401", description = "Tanpa token / token tidak valid",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public List<TaskResponse> list(@AuthenticationPrincipal UserPrincipal caller) {
        return taskService.list(caller);
    }

    @PostMapping
    @Operation(summary = "Buat tugas baru", description = "ADMIN atau MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Tugas dibuat"),
            @ApiResponse(responseCode = "400", description = "Body tidak valid",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Bukan ADMIN/MANAGER",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Assignee tidak ditemukan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<TaskResponse> create(@Valid @RequestBody CreateTaskRequest request,
                                               @AuthenticationPrincipal UserPrincipal caller) {
        TaskResponse created = taskService.create(request, caller);
        return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Ubah status pengerjaan", description = "STAFF hanya untuk tugas miliknya.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status diperbarui"),
            @ApiResponse(responseCode = "400", description = "Nilai status tidak dikenal",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Bukan assignee / role tidak diizinkan",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Tugas tidak ditemukan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TaskResponse updateStatus(@PathVariable Long id,
                                     @Valid @RequestBody UpdateTaskStatusRequest request,
                                     @AuthenticationPrincipal UserPrincipal caller) {
        return taskService.updateStatus(id, request.status(), caller);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Ubah detail tugas", description = "Mengganti title dan description. ADMIN atau MANAGER.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tugas diperbarui"),
            @ApiResponse(responseCode = "400", description = "Body tidak valid",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Bukan ADMIN/MANAGER",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Tugas tidak ditemukan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TaskResponse update(@PathVariable Long id,
                               @Valid @RequestBody UpdateTaskRequest request) {
        return taskService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Hapus tugas", description = "Hanya ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Tugas dihapus"),
            @ApiResponse(responseCode = "403", description = "Bukan ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Tugas tidak ditemukan",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
