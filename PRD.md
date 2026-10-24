# Product Requirement Document (PRD)

## Task Management System with RBAC (Java 25 Edition)

### 1. Ringkasan Eksekutif

**Task Management System with RBAC** adalah sistem backend REST API untuk mengelola penugasan tim. Sistem ini menggunakan **Role-Based Access Control (RBAC)** untuk membatasi wewenang pengguna. Dibangun menggunakan arsitektur modern berbasis **Java 25** dan **Spring Boot 3.4+**, sistem ini dirancang untuk performa tinggi, efisiensi memori, dan keamanan data.

---

### 2. Spesifikasi Teknologi & Infrastruktur

* **Bahasa Pemrograman:** Java 25
* Menggunakan **Java Records** untuk DTO yang *immutable* dan bersih tanpa *boilerplate*.
* Mengaktifkan **Virtual Threads** (`spring.threads.virtual.enabled=true`) untuk *high-concurrency* dan optimasi I/O database.


* **Framework Utama:** Spring Boot 3.4+ (Spring Web, Spring Data JPA, Spring Security 6+).
* **Database:** PostgreSQL (Production) & H2 Database (Testing).
* **Keamanan:** JSON Web Token (JWT) via `jjwt` dan `BCryptPasswordEncoder`.
* **Dokumentasi API:** Springdoc OpenAPI 3 (Swagger UI).
* **Build Tool:** Apache Maven.

---

### 3. Arsitektur Database (ERD)

Sistem menggunakan 4 tabel utama dengan relasi *One-to-Many* dan *Many-to-Many*:

1. **`user_m`**: Menyimpan kredensial (`id`, `username`, `email`, `password`).
2. **`role_m`**: Menyimpan daftar peran sistem (`id`, `name`).
3. **`user_role_m`**: Tabel pivot *Many-to-Many* antara `user_m` dan `role_m`.
4. **`task_t`**: Menyimpan detail tugas (`id`, `title`, `description`, `status`). Memiliki relasi *Many-to-One* ke tabel `user_m` sebagai `created_by` dan `assigned_to`.

---

### 4. Definisi Peran & Wewenang (User Roles)

| Peran | Deskripsi Hak Akses |
| --- | --- |
| **`ROLE_ADMIN`** | Akses absolut. Dapat mendaftarkan pengguna baru, mengubah peran, serta membuat, melihat, memperbarui, dan menghapus semua tugas. |
| **`ROLE_MANAGER`** | Dapat membuat tugas baru, menugaskan pekerjaan kepada `ROLE_STAFF`, dan memantau status seluruh tugas di dalam sistem. |
| **`ROLE_STAFF`** | Akses terbatas. Hanya dapat melihat tugas yang secara spesifik ditugaskan kepadanya (`assigned_to`) dan hanya bisa mengubah status tugas tersebut. |

---

### 5. Kebutuhan Fungsional (Functional Requirements)

**Modul Autentikasi:**

* **FR-1.1:** Sistem memfasilitasi registrasi akun baru (khusus Admin).
* **FR-1.2:** Sistem memvalidasi kredensial login dan menerbitkan JWT *Access Token*.
* **FR-1.3:** Sistem dapat mengekstrak profil pengguna yang sedang login berdasarkan token.

**Modul Manajemen Tugas:**

* **FR-2.1:** Pengguna (Admin/Manager) dapat membuat tugas dengan menyertakan *title*, *description*, dan ID *assignee*.
* **FR-2.2:** Sistem memfilter respons `GET /tasks`. Admin/Manager melihat semua tugas, sedangkan Staff hanya melihat tugas miliknya.
* **FR-2.3:** Sistem membatasi pembaruan tugas. Staff hanya diizinkan mengubah properti `status` (`TODO`, `IN_PROGRESS`, `DONE`).
* **FR-2.4:** Sistem memblokir penghapusan tugas jika *request* tidak datang dari Admin.

---

### 6. Matriks Akses API (Endpoints)

| Method | Endpoint | Fungsi | Akses Minimum |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Mendaftarkan akun | `ROLE_ADMIN` |
| `POST` | `/api/v1/auth/login` | Autentikasi & dapatkan token | Public |
| `GET` | `/api/v1/tasks` | Lihat daftar tugas (terfilter) | Semua Role |
| `POST` | `/api/v1/tasks` | Buat tugas baru | `ROLE_ADMIN`, `MANAGER` |
| `PATCH` | `/api/v1/tasks/{id}/status` | Ubah status pengerjaan | `ROLE_STAFF`, `MANAGER` |
| `PUT` | `/api/v1/tasks/{id}` | Ubah detail (judul/deskripsi) | `ROLE_ADMIN`, `MANAGER` |
| `DELETE` | `/api/v1/tasks/{id}` | Hapus tugas | `ROLE_ADMIN` |

---

### 7. Kebutuhan Non-Fungsional (Non-Functional Requirements)

* **Validasi Input:** Semua *Request Body* divalidasi menggunakan anotasi Jakarta Validation (`@NotBlank`, dll) di dalam struktur Java Record.
* **Keamanan Endpoint:** Semua endpoint `/api/v1/tasks/**` wajib menyertakan *header* `Authorization: Bearer <token>`. Akses ilegal mengembalikan status `401 Unauthorized` atau `403 Forbidden`.
* **Global Error Handling:** Pengecualian (*exceptions*) ditangkap oleh `@RestControllerAdvice` untuk menghasilkan format JSON respons eror yang seragam.

---

### 8. Fase Pengembangan (Milestones)

1. **Inisiasi (Fase 1):** Setup Maven, konfigurasi `application.yml` (aktifkan Virtual Threads), dan pembuatan entitas JPA.
2. **Keamanan (Fase 2):** Implementasi `UserDetailsService`, utilitas JWT, dan konfigurasi `SecurityFilterChain`.
3. **Core API (Fase 3):** Pembuatan Java Record DTO, Repository *custom query*, dan logika bisnis di *Service Layer*.
4. **Validasi (Fase 4):** Penambahan validasi input dan `@RestControllerAdvice` untuk standarisasi eror.
5. **Dokumentasi (Fase 5):** Integrasi Springdoc OpenAPI dan pengujian via Swagger UI.

