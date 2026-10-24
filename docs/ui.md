# UI Web

Satu halaman, tanpa build step, tanpa framework. Disajikan Spring Boot sebagai aset statis.

**File:** `src/main/resources/static/index.html` (satu file: HTML + CSS + JS).

**URL:** `http://localhost:8080/` (juga `/index.html`). Dikecualikan dari autentikasi di `SecurityConfig` — yang dilindungi adalah API-nya, bukan halamannya.

## Layar

| Layar | Isi |
| --- | --- |
| Login | username + password → `POST /api/v1/auth/login` |
| Daftar tugas | tabel judul, deskripsi, status, assignee, aksi |
| Dialog tugas | buat (`POST /tasks`) atau ubah judul/deskripsi (`PUT /tasks/{id}`) |
| Dialog pengguna | register akun (`POST /auth/register`), hanya tampil untuk ADMIN |

Tidak ada routing/halaman terpisah: satu tabel yang isinya menyesuaikan role.

## Matriks UI vs Role

| Elemen | ADMIN | MANAGER | STAFF |
| --- | --- | --- | --- |
| Tombol `+ User` | tampil | — | — |
| Tombol `+ Tugas` | tampil | tampil | — |
| Dropdown status | tampil | tampil | hanya baris miliknya |
| Tombol `Ubah` | tampil | tampil | — |
| Tombol `Hapus` | tampil | — | — |

Isi daftar mengikuti `GET /tasks` (staff otomatis hanya melihat tugasnya). Aksi yang disembunyikan tetap akan ditolak 403 oleh backend bila dipaksa.

## Sesi

- Token JWT + profil disimpan di `sessionStorage` (hilang saat tab ditutup) — bukan `localStorage`, untuk mengurangi dampak XSS.
- Pada boot, token dipakai untuk `GET /auth/me`; bila gagal → kembali ke layar login.
- Respons 401 kapan pun → otomatis logout.

## Batasan yang disengaja

- **Assignee diisi ID angka**, karena PRD tidak punya endpoint daftar pengguna. Kalau nanti ada banyak user, tambahkan `GET /api/v1/users` (ADMIN/MANAGER) lalu ganti `<input type=number>` menjadi `<select>`.
- Tanpa pagination/filter/pencarian — baru perlu bila tugas melewati ±50 baris.
- Tanpa dark mode, i18n, atau design system.
- Tanpa optimistic update: setiap perubahan memuat ulang daftar agar status selalu sesuai server.

## Menjalankan

```bash
mvn spring-boot:run
# buka http://localhost:8080/
# login: admin / admin12345 (dari BootstrapConfig)
```

Registrasi akun STAFF/MANAGER lewat tombol `+ User` setelah login sebagai admin.
