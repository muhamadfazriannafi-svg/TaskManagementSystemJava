# Task Management System — Dokumentasi Teknis

Backend REST API untuk manajemen tugas bertim dengan RBAC. Java 25 + Spring Boot 3.5 + PostgreSQL.

Dokumen ini dipecah per fase. Setiap fase punya file sendiri di `docs/`.

| Fase | Isi | Status | Dokumen |
| --- | --- | --- | --- |
| 1 | Maven, konfigurasi, entitas JPA | Selesai | [phase-1-setup.md](phase-1-setup.md) |
| 2 | JWT, UserDetailsService, SecurityFilterChain | Selesai | [phase-2-security.md](phase-2-security.md) |
| 3 | DTO Record, repository, service | Selesai | [phase-3-api.md](phase-3-api.md) |
| 4 | Validasi + Global Error Handling | Selesai | [phase-4-validation.md](phase-4-validation.md) |
| 5 | OpenAPI/Swagger | Selesai | [phase-5-docs.md](phase-5-docs.md) |

Referensi silang: [endpoint-file-map.md](endpoint-file-map.md) — endpoint mana menyentuh file mana.

Seluruh milestone PRD selesai. `mvn test` → **18 test, 0 gagal**.

| Test | Cakupan |
| --- | --- |
| `EntityMappingTest` | pemetaan 4 tabel + default status |
| `AuthFlowTest` | login, `/me`, 401 token, register hanya ADMIN |
| `TaskApiTest` | CRUD + filter role + 403/404/400 (FR-2.1…2.4) |
| `ErrorEnvelopeTest` | bentuk `ApiError` untuk 400/401/403/404 |
| `OpenApiTest` | `/v3/api-docs` + Swagger UI + UI bawaan |
| `UserListTest` | `GET /users` hanya ADMIN/MANAGER |

### UI Web

`http://localhost:8080/` — halaman tunggal (`src/main/resources/static/index.html`), tanpa build step, tanpa framework. Lihat [ui.md](ui.md).

### Swagger

`http://localhost:8080/swagger-ui/index.html` — login `admin` / `admin12345` → Authorize.

### Sebelum production

- ganti `JWT_SECRET`, hapus/lindungi seed admin di `BootstrapConfig`;
- `spring.jpa.hibernate.ddl-auto: validate` + migrasi (Flyway/Liquibase);
- kredensial DB via secret manager.

## Prasyarat

- JDK 25
- Maven 3.9+
- PostgreSQL 16+ (production); H2 in-memory dipakai untuk test

## Menjalankan

```bash
mvn test          # unit test (H2)
mvn spring-boot:run
```

Konfigurasi via environment variable (lihat `.env.example`):

| Variable | Default |
| --- | --- |
| `DB_URL` | `jdbc:postgresql://localhost:5432/taskdb` |
| `DB_USER` | `postgres` |
| `DB_PASSWORD` | `postgres` |
| `JWT_SECRET` | dev-only placeholder, **wajib diganti** di production |

## Konvensi Penamaan Tabel

| Tabel | Jenis | Isi |
| --- | --- | --- |
| `user_m` | master | kredensial pengguna |
| `role_m` | master | daftar peran |
| `user_role_m` | pivot | relasi many-to-many user–role |
| `task_t` | transaksi | data tugas |

Nama class Java tetap pendek (`User`, `Role`, `Task`); nama tabel ditetapkan eksplisit lewat `@Table`.
