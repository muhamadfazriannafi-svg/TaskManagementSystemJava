package com.example.taskmanagement.controller;

import com.example.taskmanagement.advice.ApiError;
import com.example.taskmanagement.dto.LoginRequest;
import com.example.taskmanagement.dto.RegisterRequest;
import com.example.taskmanagement.dto.TokenResponse;
import com.example.taskmanagement.dto.UserResponse;
import com.example.taskmanagement.security.UserPrincipal;
import com.example.taskmanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Registrasi (khusus ADMIN), login, dan profil pemanggil")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Daftarkan akun baru", description = "Hanya ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Akun dibuat"),
            @ApiResponse(responseCode = "400", description = "Validasi gagal / username atau email sudah dipakai",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Tanpa token",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Bukan ADMIN",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.created(URI.create("/api/v1/users")).build();
    }

    @PostMapping("/login")
    @Operation(summary = "Autentikasi & dapatkan JWT", description = "Endpoint public.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Token diterbitkan"),
            @ApiResponse(responseCode = "400", description = "Body tidak valid",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Kredensial salah",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Profil pengguna yang sedang login")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profil"),
            @ApiResponse(responseCode = "401", description = "Tanpa token / token tidak valid",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return UserResponse.from(principal);
    }
}
