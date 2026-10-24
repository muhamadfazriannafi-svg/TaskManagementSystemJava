# Fase 4 — Validasi Input & Global Error Handling

Tujuan: semua kegagalan (validasi, otorisasi, data tidak ada, error tak terduga) keluar dengan **satu bentuk JSON** yang sama.

## 4.1 Bentuk Error Seragam

```json
{
  "timestamp": "2026-10-24T15:04:11.512Z",
  "status": 400,
  "error": "Bad Request",
  "message": "email: must not be blank",
  "path": "/api/v1/auth/register",
  "details": [
    { "field": "username", "message": "must not be blank" },
    { "field": "email", "message": "must not be blank" }
  ]
}
```

| Field | Keterangan |
| --- | --- |
| `timestamp` | waktu kejadian (UTC, ISO-8601) |
| `status` / `error` | kode HTTP dan reason phrase |
| `message` | ringkasan yang bisa ditampilkan ke klien |
| `path` | URI request yang gagal |
| `details` | daftar error per field; array kosong bila bukan error validasi |

## 4.2 Pemetaan Exception → Status

| Exception | Status | Pesan |
| --- | --- | --- |
| `MethodArgumentNotValidException` | 400 | `"<field>: <pesan>"` + semua field di `details` |
| `HttpMessageNotReadableException` | 400 | `Malformed request body` (JSON rusak / enum tak dikenal) |
| `MethodArgumentTypeMismatchException` | 400 | `Invalid value for '<param>'` (mis. `/tasks/abc`) |
| `IllegalArgumentException` | 400 | pesan asli (duplikat username/email, role tak dikenal) |
| `ResponseStatusException` | sesuai status | pesan asli (404 task/assignee) |
| `NoResourceFoundException` | 404 | `Resource not found` (endpoint tidak dikenal) |
| `HttpRequestMethodNotSupportedException` | 405 | pesan Spring |
| `AuthenticationException` | 401 | `Invalid username or password` |
| `AccessDeniedException` | 403 | `Access denied` |
| `Exception` (fallback) | 500 | `Unexpected error` — detail tidak dibocorkan |

## 4.3 Dua Jalur Error

`@RestControllerAdvice` hanya menangkap exception yang terjadi **di dalam** penanganan MVC. Penggalan yang gagal di filter chain (sebelum MVC) seperti token hilang/rusak tidak pernah sampai ke sana, sehingga status 401/403 dipaksa lewat `SecurityErrorResponder` yang mengimplementasikan `AuthenticationEntryPoint` + `AccessDeniedHandler` dan menulis `ApiError` langsung ke response.

Keduanya dirakit dari record yang sama (`ApiError`) sehingga bentuk JSON identik.

## 4.4 Validasi

Anotasi Jakarta Validation diletakkan langsung di komponen Java Record:

| Record | Aturan |
| --- | --- |
| `RegisterRequest` | `username` @NotBlank 3–50; `email` @NotBlank @Email ≤100; `password` @NotBlank 8–72; `role` @NotBlank |
| `LoginRequest` | `username`, `password` @NotBlank |
| `CreateTaskRequest` | `title` @NotBlank ≤200; `description` ≤5000; `assignedToId` @NotNull |
| `UpdateTaskRequest` | `title` @NotBlank ≤200; `description` ≤5000 |
| `UpdateTaskStatusRequest` | `status` @NotNull (dipetakan ke enum, nilai asing → 400) |

Controller memakai `@Valid @RequestBody`. Batas `password` 72 mengikuti batas input BCrypt. Pesan error bersumber dari pesan default Hibernate Validator.

## 4.5 Verifikasi

`ErrorEnvelopeTest` (4 test) memeriksa envelope untuk:

1. 401 dari filter chain (tanpa token, token rusak) — termasuk `path`;
2. 401 dari kredensial salah (jalur MVC);
3. 400 validasi dengan `details` berisi 4 field pada body kosong;
4. 403 (staff DELETE), 404 (task tidak ada), 404 (endpoint tidak dikenal), 400 (path variable bukan angka), 400 (JSON rusak), 400 (username duplikat, `message` persis).

```
mvn test
Tests run: 14, Failures: 0, Errors: 0
```

## 4.6 Struktur Paket (tambahan)

```
advice/
├── ApiError.java                (record envelope + FieldError)
├── ApiExceptionHandler.java     (exception → envelope, MVC path)
└── SecurityErrorResponder.java  (401/403, filter-chain path)
```
