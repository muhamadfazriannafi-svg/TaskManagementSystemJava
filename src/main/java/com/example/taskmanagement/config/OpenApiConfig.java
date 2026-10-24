package com.example.taskmanagement.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER = "bearerAuth";

    @Bean
    OpenAPI apiInfo() {
        return new OpenAPI()
                .info(new Info()
                        .title("Task Management System API")
                        .version("v1")
                        .description("""
                                REST API manajemen tugas dengan RBAC.

                                1. `POST /api/v1/auth/login` (admin / admin12345 pada database segar) → salin `accessToken`.
                                2. Klik **Authorize** dan tempel token (tanpa prefix `Bearer`).
                                3. Role: `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`.
                                """))
                .components(new Components().addSecuritySchemes(BEARER, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER));
    }
}
