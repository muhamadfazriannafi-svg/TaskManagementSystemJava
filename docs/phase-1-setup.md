# Fase 1 — Inisiasi &amp; Pemetaan Entitas

Tujuan: proyek bisa di-build, koneksi datasource terkonfigurasi, dan 4 tabel PRD terpetakan ke JPA.

## 1.1 Dependensi (`pom.xml`)


| Dependensi                                         | Alasan                                 |
| -------------------------------------------------- | -------------------------------------- |
| `spring-boot-starter-web`                          | REST controller                        |
| `spring-boot-starter-data-jpa`                     | ORM + repository                       |
| `spring-boot-starter-security`                     | autentikasi/otorisasi (dipakai Fase 2) |
| `spring-boot-starter-validation`                   | Jakarta Validation                     |
| `jjwt-api` / `jjwt-impl` / `jjwt-jackson` 0.12.6   | terbit &amp; parsing JWT               |
| `springdoc-openapi-starter-webmvc-ui` 2.7.0        | Swagger UI                             |
| `postgresql` (runtime)                             | database production                    |
| `h2` (runtime)                                     | database test                          |
| `lombok` 1.18.48                                   | getter/setter entitas                  |
| `spring-boot-starter-test`, `spring-security-test` | testing                                |


Versi penting:

- **Spring Boot 3.5.16**, bukan 3.4. Spring 6.2 gagal membaca bytecode Java 25 (`Incompatible class format`). Boot 3.5 memakai Spring 6.2.x yang sudah mendukung class file versi 69.
- **Lombok 1.18.48**. Rilis 1.18.38 tidak bisa berjalan sebagai annotation processor di JDK 25 sehingga getter/setter tidak dihasilkan. Selain menaikkan versi, Lombok didaftarkan eksplisit di `maven-compiler-plugin` → `<annotationProcessorPaths>`; tanpa itu processor tidak dipanggil walau ada di classpath.

## 1.2 Konfigurasi

`src/main/resources/application.yml`

- `spring.threads.virtual.enabled: true` → setiap request ditangani di virtual thread (sesuai PRD §2).
- `spring.jpa.open-in-view: false` → transaksi DB hanya di service layer, tidak menggantung di view.
- `ddl-auto: update` untuk kemudahan development. **Production: ganti ke `validate`** dan kelola skema lewat migrasi (Flyway/Liquibase).
- `app.jwt.secret` / `app.jwt.expiration-ms` dibaca Fase 2.

`src/test/resources/application.yml` menimpa datasource dengan H2 mode PostgreSQL dan `ddl-auto: create-drop`, sehingga `mvn test` tidak butuh database eksternal.

## 1.3 Entitas


| PRD           | Class   | Tabel         | Catatan                                                            |
| ------------- | ------- | ------------- | ------------------------------------------------------------------ |
| `user_m`      | `User`  | `user_m`      | `username`, `email` unik                                           |
| `role_m`      | `Role`  | `role_m`      | `name` unik, mis. `ROLE_ADMIN`                                     |
| `user_role_m` | (pivot) | `user_role_m` | `@ManyToMany` di `User.roles`, kolom `user_id`/`role_id`           |
| `task_t`      | `Task`  | `task_t`      | `created_by` (NOT NULL), `assigned_to` (nullable) → FK ke `user_m` |


Keputusan desain:

- `User.roles` memakai `FetchType.EAGER` karena `UserDetails` dibangun pada setiap request terautentikasi; Lazy akan memicu `LazyInitializationException` di luar transaksi.
- `Task.createdBy`/`assignedTo` memakai `LAZY` karena hanya dibutuhkan saat memetakan DTO.
- `Task.status` disimpan sebagai `EnumType.STRING` (lebih tahan terhadap perubahan urutan enum) dengan nilai `TODO`, `IN_PROGRESS`, `DONE`.
- `User.equals/hashCode` berbasis id dengan `hashCode` konstan per class — aman untuk entitas yang id-nya belum ada saat dimasukkan ke `HashSet`.
- Pakai Lombok, bukan Java Record: JPA butuh class mutable dengan constructor no-arg. Record tetap dipakai untuk DTO (Fase 3).

## 1.4 Verifikasi

`src/test/java/.../EntityMappingTest.java` — `@DataJpaTest`:

1. persist `Role`, `User`, `Task`;
2. assert relasi many-to-many dan default status `TODO` terbaca setelah `em.clear()`;
3. assert keempat tabel benar-benar dibuat (`information_schema`, nama di-`lower()` karena H2 uppercase sementara PostgreSQL lowercase).

```
mvn test
Tests run: 1, Failures: 0, Errors: 0
```

## 1.5 Struktur Paket

```
com.example.taskmanagement
├── TaskManagementApplication.java
└── entity
    ├── Role.java
    ├── User.java
    ├── Task.java
    └── TaskStatus.java
```

