# Fase 5 — Dokumentasi API (Springdoc OpenAPI 3)

Tujuan: seluruh endpoint terdokumentasi otomatis dan bisa dicoba langsung dari Swagger UI, termasuk alur autentikasi bearer.

## 5.1 URL

| URL | Isi |
| --- | --- |
| `/swagger-ui/index.html` | Swagger UI (public) |
| `/v3/api-docs` | dokumen OpenAPI 3 JSON (public) |

Keduanya dikecualikan dari autentikasi di `SecurityConfig`.

## 5.2 `OpenApiConfig`

- `Info`: judul `Task Management System API`, versi `v1`, deskripsi berisi langkah login → Authorize → role tersedia.
- `SecurityScheme` bernama `bearerAuth`, tipe `HTTP`, skema `bearer`, format `JWT`.
- Skema diterapkan global (`addSecurityItem`), sehingga semua operasi otomatis memakai ikon gembok. Endpoint public menimpanya dengan `@SecurityRequirements` (kosong).

Praktisnya: tombol **Authorize** cukup diisi token hasil `POST /api/v1/auth/login` tanpa kata `Bearer`.

## 5.3 Anotasi per Controller

| Anotasi | Kegunaan |
| --- | --- |
| `@Tag` | mengelompokkan operasi (`Authentication`, `Tasks`) |
| `@Operation` | ringkasan + aturan role dalam teks |
| `@ApiResponses` / `@ApiResponse` | daftar status yang mungkin, termasuk 401/403/404 dengan skema `ApiError` |
| `@SecurityRequirement(name = "bearerAuth")` | menandai endpoint tertentu butuh token |
| `@SecurityRequirements` | menghapus kebutuhan token (dipakai pada `/auth/login`) |

DTO (Java Record) otomatis menjadi skema request/response — `CreateTaskRequest`, `TaskResponse`, dan seterusnya.

## 5.4 Verifikasi

`OpenApiTest`:

1. `GET /v3/api-docs` → 200, judul benar, skema `bearerAuth.scheme = bearer`, dan **ketujuh** path PRD hadir (`auth/login`, `auth/register`, `auth/me`, `tasks` GET/POST, `tasks/{id}/status` PATCH, `tasks/{id}` PUT/DELETE).
2. `GET /swagger-ui/index.html` → 200 (aset UI tersaji).

```
mvn test
Tests run: 16, Failures: 0, Errors: 0
```

## 5.5 Uji Manual (Swagger UI)

```
mvn spring-boot:run
```

1. buka `http://localhost:8080/swagger-ui/index.html`;
2. `POST /api/v1/auth/login` → `{"username":"admin","password":"admin12345"}`;
3. salin `accessToken` → tombol **Authorize** → tempel → Authorize;
4. `POST /api/v1/auth/register` untuk membuat `ROLE_STAFF`/`ROLE_MANAGER`;
5. login akun tersebut, ganti token di Authorize, lalu uji `GET /api/v1/tasks` (staff hanya melihat tugas miliknya) dan `DELETE` (403 untuk manager/staff).

## 5.6 Struktur Paket (tambahan)

```
config/OpenApiConfig.java
controller/AuthController.java   (anotasi OpenAPI)
controller/TaskController.java   (anotasi OpenAPI)
```
