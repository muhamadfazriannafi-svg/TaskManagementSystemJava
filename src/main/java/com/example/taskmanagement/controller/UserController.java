package com.example.taskmanagement.controller;

import com.example.taskmanagement.dto.UserSummary;
import com.example.taskmanagement.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Daftar pengguna untuk pemilih assignee")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Daftar pengguna", description = "ADMIN atau MANAGER.")
    public List<UserSummary> list() {
        return userService.list();
    }
}
