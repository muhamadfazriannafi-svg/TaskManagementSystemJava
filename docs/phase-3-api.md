# Fase 3 — Core API (DTO, Repository, Service)

Tujuan: seluruh endpoint `/api/v1/tasks` berfungsi dengan aturan RBAC bisnis (FR-2.1 … FR-2.4) di atas lapisan HTTP Fase 2.

## 3.1 Lapisan

```
Controller  → validasi request (@Valid) + ekstrak UserPrincipal
Service     → aturan bisnis & otorisasi data-level, @Transactional
Repository  → query filtering per role
Entity      → mapping tabel (Fase 1)
```

Otorisasi dibagi dua lapis: **HTTP-level** (method + role, `SecurityConfig`) dan **data-level** (`TaskService`, mis. staff vs task milik siapa). Lapis kedua tidak bisa diwakili role saja karena bergantung pada isi baris.

## 3.2 DTO (Java Record)

| Record | Arah | Field |
| --- | --- | --- |
| `CreateTaskRequest` | in | `title` (@NotBlank, ≤200), `description` (≤5000), `assignedToId` (@NotNull) |
| `UpdateTaskRequest` | in | `title` (@NotBlank), `description` — PUT = replace penuh field yang boleh diedit |
| `UpdateTaskStatusRequest` | in | `status` (@NotNull, enum) |
| `TaskResponse` | out | `id, title, description, status, createdById, assignedToId` |

`TaskResponse` hanya memuat id user (bukan objek user) agar respons tidak meledak dan tidak memicu lazy-load.

## 3.3 Repository

`TaskRepository`:

- `findByAssignedToIdOrderById(Long)` — query filtering staff (FR-2.2).
- `findAll()` di-override dengan `@EntityGraph({"createdBy","assignedTo"})` — menghindari N+1 saat memetakan `TaskResponse`.
- Dua-duanya `@EntityGraph` karena `open-in-view` dimatikan.

## 3.4 Aturan Service

| Operasi | Aturan |
| --- | --- |
| `list` | staff → hanya `assignedTo = caller.id`; selain itu semua tugas |
| `create` | assignee wajib ada (404 bila tidak); `createdBy` diambil dari `getReferenceById(caller.id)` (tanpa SELECT tambahan) |
| `updateStatus` | staff hanya boleh untuk task miliknya (403 bila bukan), selain itu bebas selama role diizinkan |
| `update` | ganti `title` + `description` |
| `delete` | task harus ada (404), hapus permanen |

`isStaff()` cukup mengecek keberadaan `ROLE_STAFF`; akun dengan banyak role diperlakukan lebih permisif — sesuai semangat "akses absolut" untuk ADMIN.

## 3.5 Kontrak Endpoint

### GET `/api/v1/tasks`

```json
[
  { "id": 1, "title": "write report", "description": "desc",
    "status": "TODO", "createdById": 2, "assignedToId": 3 }
]
```

### POST `/api/v1/tasks` — ADMIN/MANAGER

```json
{ "title": "write report", "description": "desc", "assignedToId": 3 }
```
→ `201` + `Location: /api/v1/tasks/{id}` + body `TaskResponse`. `status` dimulai dari `TODO`.

### PATCH `/api/v1/tasks/{id}/status` — STAFF/MANAGER/ADMIN

```json
{ "status": "IN_PROGRESS" }
```
Nilai valid: `TODO`, `IN_PROGRESS`, `DONE`. Staff pada task orang lain → `403`.

### PUT `/api/v1/tasks/{id}` — ADMIN/MANAGER

```json
{ "title": "final", "description": "v2" }
```

### DELETE `/api/v1/tasks/{id}` — ADMIN

→ `204 No Content`.

## 3.6 Kode Status

| Kode | Kondisi |
| --- | --- |
| 200 / 201 / 204 | sukses |
| 400 | body tidak valid / enum tidak dikenal / `assignedToId` hilang |
| 401 | token tidak ada/rusak |
| 403 | role kurang, atau staff menyentuh task yang bukan miliknya |
| 404 | task/assignee tidak ada (`ResponseStatusException`, bodi standar Spring) |

Bodi 404 masih memakai format bawaan Spring; penyeragaman penuh dilakukan di **Fase 4**.

## 3.7 Verifikasi

`TaskApiTest` — 5 skenario end-to-end:

1. manager membuat task, semua role melihatnya;
2. daftar staff terfilter sesuai `assigned_to`;
3. staff tidak bisa create/put/delete, bisa PATCH miliknya, gagal (403) PATCH milik orang lain, manager bisa PATCH;
4. manager bisa PUT, tidak bisa DELETE; admin bisa DELETE (204);
5. assignee tidak dikenal → 404, task tidak dikenal → 404, status invalid → 400.

```
mvn test
Tests run: 10, Failures: 0, Errors: 0
```

## 3.8 Struktur Paket (tambahan)

```
dto/
├── CreateTaskRequest.java
├── UpdateTaskRequest.java
├── UpdateTaskStatusRequest.java
└── TaskResponse.java
repository/TaskRepository.java
service/TaskService.java
controller/TaskController.java
```
