# Peta Endpoint → File

Referensi cepat: untuk setiap endpoint, file mana saja yang dilalui request dari socket sampai response.

Notasi singkat:
- **C** = `controller/`
- **S** = `service/`
- **R** = `repository/`
- **D** = `dto/`
- **E** = `entity/`
- **SEC** = `security/`
- **ADV** = `advice/`

Semua jalur di bawah `src/main/java/com/example/taskmanagement/`.

---

## Alur Umum (berlaku untuk semua request)

| Urutan | File | Peran |
| --- | --- | --- |
| 1 | `TaskManagementApplication.java` | entry point, `SpringApplication.run` |
| 2 | `security/SecurityConfig.java` | `SecurityFilterChain`: cek token & matriks role per method+path |
| 3 | `security/JwtAuthFilter.java` | baca header `Authorization: Bearer …`, isi `SecurityContext` |
| 4 | `security/JwtService.java` | verifikasi signature + expiry, ambil `sub` |
| 5 | `security/AppUserDetailsService.java` | load user dari DB |
| 6 | `repository/UserRepository.java` | `findByUsername` (EAGER roles) |
| 7 | `entity/User.java`, `entity/Role.java` | sumber data |
| 8 | `security/UserPrincipal.java` | dibentuk, jadi `@AuthenticationPrincipal` di controller |
| 9 | `controller/*Controller.java` | terima request |
| 10 | `dto/*Request.java` | deserialisasi + validasi |
| 11 | `service/*Service.java` | aturan bisnis |
| 12 | `repository/*Repository.java` | query |
| 13 | `entity/*.java` | mapping tabel `user_m`, `role_m`, `user_role_m`, `task_t` |
| 14 | `dto/*Response.java` | bentuk output |
| 15 | `advice/ApiExceptionHandler.java` | bila exception di jalur MVC |
| 16 | `advice/SecurityErrorResponder.java` | bila gagal di filter chain (401/403) |
| 17 | `advice/ApiError.java` | bentuk JSON error seragam |

---

## POST `/api/v1/auth/login` (public)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `requestMatchers("/api/v1/auth/login").permitAll()` |
| Controller | `controller/AuthController.java#login` |
| Request DTO | `dto/LoginRequest.java` |
| Service | `service/AuthService.java#login` |
| Autentikasi | `security/SecurityConfig.java#authenticationManager` → `security/AppUserDetailsService.java` → `repository/UserRepository.java#findByUsername` |
| Password | `security/SecurityConfig.java#passwordEncoder` (`BCryptPasswordEncoder`) |
| Entity | `entity/User.java`, `entity/Role.java` |
| Token | `security/JwtService.java#generateToken` |
| Response DTO | `dto/TokenResponse.java` |
| Error 401 | `advice/ApiExceptionHandler.java#unauthorized` → `advice/ApiError.java` |
| Error 400 | `advice/ApiExceptionHandler.java#validation` |

---

## POST `/api/v1/auth/register` (ADMIN)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasRole("ADMIN")` → gagal → `advice/SecurityErrorResponder.java` |
| Controller | `controller/AuthController.java#register` |
| Request DTO | `dto/RegisterRequest.java` |
| Service | `service/AuthService.java#register` |
| Cek duplikat | `repository/UserRepository.java#existsByUsername`, `#existsByEmail` |
| Cek role | `repository/RoleRepository.java#findByName` |
| Password | `security/SecurityConfig.java#passwordEncoder` |
| Simpan | `repository/UserRepository.java#save` → `entity/User.java`, `entity/Role.java`, pivot `user_role_m` |
| Error 400 | `advice/ApiExceptionHandler.java` (`IllegalArgumentException`) |

---

## GET `/api/v1/auth/me` (authenticated)

| Tahap | File |
| --- | --- |
| Token | `security/JwtAuthFilter.java` → `security/JwtService.java#extractUsername` → `security/AppUserDetailsService.java` |
| Controller | `controller/AuthController.java#me` |
| Principal | `security/UserPrincipal.java` |
| Response DTO | `dto/UserResponse.java#from` |
| Error 401 | `advice/SecurityErrorResponder.java` |

---

## GET `/api/v1/tasks` (ADMIN, MANAGER, STAFF)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasAnyRole(ADMIN, MANAGER, STAFF)` |
| Controller | `controller/TaskController.java#list` |
| Service | `service/TaskService.java#list` |
| Cek role staff | `service/TaskService.java#isStaff` |
| Query staff | `repository/TaskRepository.java#findByAssignedToIdOrderById` (custom query, `@EntityGraph`) |
| Query admin/manager | `repository/TaskRepository.java#findAll` (`@EntityGraph`) |
| Entity | `entity/Task.java`, `entity/TaskStatus.java`, `entity/User.java` |
| Response DTO | `dto/TaskResponse.java#from` |

---

## POST `/api/v1/tasks` (ADMIN, MANAGER)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasAnyRole(ADMIN, MANAGER)` |
| Controller | `controller/TaskController.java#create` |
| Request DTO | `dto/CreateTaskRequest.java` |
| Service | `service/TaskService.java#create` |
| Cari assignee | `repository/UserRepository.java#findById` / `#getReferenceById` (creator) |
| Simpan | `repository/TaskRepository.java#save` → `entity/Task.java` |
| Response DTO | `dto/TaskResponse.java` |
| Error 404 | `service/TaskService.java#notFound` → `advice/ApiExceptionHandler.java#statusException` |
| Error 403 | `advice/SecurityErrorResponder.java` |

---

## PATCH `/api/v1/tasks/{id}/status` (STAFF, MANAGER, ADMIN)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasAnyRole(STAFF, MANAGER, ADMIN)` |
| Controller | `controller/TaskController.java#updateStatus` |
| Request DTO | `dto/UpdateTaskStatusRequest.java` (+ `entity/TaskStatus.java`) |
| Service | `service/TaskService.java#updateStatus` |
| Cek kepemilikan | `service/TaskService.java#isAssignee` (staff vs `assigned_to`) |
| Load & update | `repository/TaskRepository.java#findById` → `entity/Task.java#setStatus` |
| Response DTO | `dto/TaskResponse.java` |
| Error 400 (enum asing) | `advice/ApiExceptionHandler.java#unreadable` |
| Error 403 (bukan assignee) | `service/TaskService.java` → `advice/ApiExceptionHandler.java#statusException` |

---

## PUT `/api/v1/tasks/{id}` (ADMIN, MANAGER)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasAnyRole(ADMIN, MANAGER)` |
| Controller | `controller/TaskController.java#update` |
| Request DTO | `dto/UpdateTaskRequest.java` |
| Service | `service/TaskService.java#update` |
| Load & update | `repository/TaskRepository.java#findById` → `entity/Task.java#setTitle`, `#setDescription` |
| Response DTO | `dto/TaskResponse.java` |
| Error 404 | `service/TaskService.java#notFound` |

---

## DELETE `/api/v1/tasks/{id}` (ADMIN)

| Tahap | File |
| --- | --- |
| Otorisasi | `security/SecurityConfig.java` — `hasRole("ADMIN")` |
| Controller | `controller/TaskController.java#delete` |
| Service | `service/TaskService.java#delete` |
| Cek & hapus | `repository/TaskRepository.java#existsById`, `#deleteById` → `entity/Task.java` |
| Error 404 | `service/TaskService.java#notFound` |
| Error 403 | `advice/SecurityErrorResponder.java` |

---

## Dokumentasi & Konfigurasi (bukan endpoint bisnis)

| File | Peran |
| --- | --- |
| `config/OpenApiConfig.java` | metadata OpenAPI + skema `bearerAuth` |
| `/swagger-ui/index.html` | Springdoc UI |
| `/v3/api-docs` | dokumen OpenAPI JSON |
| `config/BootstrapConfig.java` | seed 3 role + user `admin` saat start |
| `src/main/resources/application.yml` | datasource, virtual threads, secret JWT |
| `src/test/resources/application.yml` | H2 + secret test |

---

## Ringkasan Cepat

| Endpoint | Controller | Service | Repository | DTO |
| --- | --- | --- | --- | --- |
| POST `/auth/login` | `AuthController` | `AuthService` | `UserRepository` | `LoginRequest`, `TokenResponse` |
| POST `/auth/register` | `AuthController` | `AuthService` | `UserRepository`, `RoleRepository` | `RegisterRequest` |
| GET `/auth/me` | `AuthController` | — | — | `UserResponse` |
| GET `/tasks` | `TaskController` | `TaskService` | `TaskRepository` | `TaskResponse` |
| POST `/tasks` | `TaskController` | `TaskService` | `TaskRepository`, `UserRepository` | `CreateTaskRequest`, `TaskResponse` |
| PATCH `/tasks/{id}/status` | `TaskController` | `TaskService` | `TaskRepository` | `UpdateTaskStatusRequest`, `TaskResponse` |
| PUT `/tasks/{id}` | `TaskController` | `TaskService` | `TaskRepository` | `UpdateTaskRequest`, `TaskResponse` |
| DELETE `/tasks/{id}` | `TaskController` | `TaskService` | `TaskRepository` | — |
