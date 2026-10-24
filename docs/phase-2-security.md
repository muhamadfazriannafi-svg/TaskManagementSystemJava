# Fase 2 — Keamanan (JWT + Security Filter Chain)

Tujuan: login menerbitkan JWT, setiap request tervalidasi tokennya, dan matriks akses PRD §6 ditegakkan di level HTTP.

## 2.1 Alur Autentikasi

```
POST /api/v1/auth/login {username,password}
        │
        ├─ AuthenticationManager → DaoAuthenticationProvider → AppUserDetailsService
        │      (cocokkan BCrypt hash)
        ├─ JwtService.generateToken(UserPrincipal)  → HS256, subject = username
        └─ 200 {accessToken, tokenType, expiresInMs}

Request berikutnya: Authorization: Bearer <jwt>
        │
        └─ JwtAuthFilter (OncePerRequestFilter)
               verify signature + expiry → load UserDetails → set SecurityContext
```

## 2.2 Komponen

| Class | Tanggung jawab |
| --- | --- |
| `JwtService` | terbit & verifikasi token via `jjwt` 0.12.6 (`Jwts.parser().verifyWith(key)`) |
| `JwtAuthFilter` | membaca header `Authorization`, mengisi `SecurityContextHolder` |
| `AppUserDetailsService` | `UserRepository.findByUsername` → `UserPrincipal` |
| `UserPrincipal` | `record` implementasi `UserDetails`; membawa `id` + authorities |
| `SecurityConfig` | `SecurityFilterChain`, `PasswordEncoder`, `AuthenticationManager` |
| `BootstrapConfig` | seed 3 role + user `admin` saat aplikasi start |

### Klaim token

| Klaim | Isi |
| --- | --- |
| `sub` | username |
| `uid` | id user |
| `roles` | daftar authority (`ROLE_ADMIN`, …) |
| `iat` / `exp` | waktu terbit / kedaluwarsa (`app.jwt.expiration-ms`, default 24 jam) |

### Password

`BCryptPasswordEncoder`. Registrasi menolak password > 72 byte karena BCrypt memotong input di batas itu (batas teknis algoritma, bukan pilihan desain).

### State

`SessionCreationPolicy.STATELESS`, CSRF dimatikan — API murni bearer token, tidak ada cookie/sesi.

## 2.3 Matriks Otorisasi (SecurityConfig)

| Method | Path | Rule |
| --- | --- | --- |
| POST | `/api/v1/auth/login` | permitAll |
| POST | `/api/v1/auth/register` | `hasRole("ADMIN")` |
| GET | `/api/v1/auth/me` | authenticated |
| GET | `/api/v1/tasks` | ADMIN, MANAGER, STAFF |
| POST | `/api/v1/tasks` | ADMIN, MANAGER |
| PATCH | `/api/v1/tasks/*/status` | STAFF, MANAGER, ADMIN |
| PUT | `/api/v1/tasks/*` | ADMIN, MANAGER |
| DELETE | `/api/v1/tasks/*` | ADMIN |
| — | `/swagger-ui/**`, `/v3/api-docs/**` | permitAll |

Catatan: PRD menulis "`ROLE_ADMIN`, `MANAGER`". `hasRole("ADMIN")` otomatis menambahkan prefix `ROLE_`, sehingga nilai di DB tetap `ROLE_ADMIN`. ADMIN ikut diberi akses PATCH (PRD hanya menyebut STAFF/MANAGER) karena role ADMIN didefinisikan "akses absolut".

### Respons error

| Kondisi | Status | Bodi |
| --- | --- | --- |
| tanpa token / token rusak / kedaluwarsa | 401 | `{"status":401,"error":"Unauthorized","message":"…"}` |
| token valid tapi role kurang | 403 | `{"status":403,"error":"Forbidden","message":"Access denied"}` |
| kredensial login salah | 401 | via `ApiExceptionHandler` |
| duplikat username/email, role tidak dikenal | 400 | via `ApiExceptionHandler` |
| body tidak valid | 400 | `"field: pesan"` |

Filter-chain error dihasilkan `AuthenticationEntryPoint`/`AccessDeniedHandler` (bukan `@RestControllerAdvice`, yang hanya menangani exception di dalam MVC) tetapi memakai bodi standar yang sama.

## 2.4 Endpoint Fase Ini

### POST `/api/v1/auth/login` — public

```json
{ "username": "admin", "password": "admin12345" }
```
→ `200`
```json
{ "accessToken": "eyJ…", "tokenType": "Bearer", "expiresInMs": 86400000 }
```

### POST `/api/v1/auth/register` — ADMIN

```json
{ "username": "carol", "email": "carol@example.com", "password": "secret123", "role": "ROLE_STAFF" }
```
→ `201 Created`. Role harus salah satu dari `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF`.

### GET `/api/v1/auth/me` — authenticated

```json
{ "id": 1, "username": "admin", "roles": ["ROLE_ADMIN"] }
```

## 2.5 Bootstrap Data

Saat start (idempotent): role `ROLE_ADMIN`, `ROLE_MANAGER`, `ROLE_STAFF` dibuat bila belum ada, lalu user `admin` / `admin12345` dengan `ROLE_ADMIN`.

> **Peringatan keamanan:** kredensial admin ini hard-coded untuk development. Sebelum deploy, hapus blok pembuatan admin atau ambil kredensial dari environment variable dan wajibkan ganti password. `JWT_SECRET` default juga hanya placeholder dan harus diganti.

## 2.6 Verifikasi

`AuthFlowTest` (`@SpringBootTest` + MockMvc, H2):

1. login mengembalikan token; `/me` mengembalikan username + `ROLE_ADMIN`;
2. `/api/v1/tasks` tanpa token → 401; token sampah → 401;
3. register tanpa token → 401; dengan token admin → 201;
4. register user STAFF, login, `/me` → 200, lalu register dengan token STAFF → 403.

```
mvn test
Tests run: 5, Failures: 0, Errors: 0   (4 AuthFlowTest + 1 EntityMappingTest)
```

## 2.7 Struktur Paket (tambahan)

```
security/
├── SecurityConfig.java
├── JwtService.java
├── JwtAuthFilter.java
├── AppUserDetailsService.java
└── UserPrincipal.java
repository/
├── UserRepository.java
└── RoleRepository.java
dto/
├── LoginRequest.java
├── RegisterRequest.java
├── TokenResponse.java
└── UserResponse.java
service/AuthService.java
controller/AuthController.java
config/BootstrapConfig.java
advice/ApiExceptionHandler.java   (kerangka; diperluas di Fase 4)
```
