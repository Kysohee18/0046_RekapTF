# API RekapTF

Jalankan API Go dari folder ini dengan `go run .`. API lokal mendengarkan di `127.0.0.1:8080`. Gunakan akun dan data sintetis. Penyimpanan bawaan berada di memori dan hilang saat proses API berhenti.

| Metode | Endpoint | Kebutuhan |
|---|---|---|
| GET | `/health` | Melaporkan mode `demo` dan jenis penyimpanan |
| POST | `/v1/auth/register` | JSON `businessName`, `email`, `password` |
| POST | `/v1/auth/login` | JSON `email` dan `password` |
| POST | `/v1/auth/refresh` | JSON `refreshToken`. Token lama diganti setelah permintaan berhasil. |
| POST | `/v1/auth/logout` | JSON `refreshToken`. Berhasil dengan HTTP 204. |
| GET | `/v1/account` | `Authorization: Bearer <accessToken>`. Menampilkan nama usaha dan peran `UMKM_USER`. |
| GET | `/v1/categories?q=` atau `/v1/payers?q=` | Daftar `items` milik akun. Kueri `q` opsional dan mencari nama tanpa membedakan huruf besar dan kecil. |
| POST | `/v1/categories` atau `/v1/payers` | JSON `name` dan `note` opsional. Mengembalikan data baru dengan HTTP 201. |
| GET, PUT, DELETE | `/v1/categories/{id}` atau `/v1/payers/{id}` | Baca, ubah dengan JSON `name` dan `note` opsional, atau hapus dengan HTTP 204. |
| POST | `/v1/drafts/extract` | Satu JPEG atau PNG dalam field multipart `image`. Mengembalikan draf dengan HTTP 201. |
| POST | `/v1/drafts/extract-text` | JSON `{ "text": "..." }` untuk percobaan ML Kit. Mengembalikan draf dengan HTTP 201. |
| GET | `/v1/drafts?q=` | Daftar draf, dengan pencarian opsional termasuk status, tanggal, data teks, kategori, dan pengirim. |
| GET, PUT, DELETE | `/v1/drafts/{id}` | Baca, ubah isian pemasukan, atau buang draf. Menghapus draf yang sudah dikonfirmasi mempertahankan transaksi. |
| POST | `/v1/drafts/{id}/confirm` | Konfirmasi isian pemasukan. Percobaan pertama menghasilkan HTTP 201. Pengulangan mengembalikan transaksi yang sama dengan HTTP 200. |
| GET | `/v1/transactions?q=&from=&to=` | Riwayat transaksi terkonfirmasi. Cari dan batasi tanggal dengan `YYYY-MM-DD`. Semua filter opsional. |
| GET, PUT, DELETE | `/v1/transactions/{id}` | Baca, koreksi isian pemasukan, atau hapus transaksi. |
| GET | `/v1/recap?from=YYYY-MM-DD&to=YYYY-MM-DD` | Ringkasan `from`, `to`, `total`, `count`, dan `items` untuk rentang inklusif. |
| GET, POST | `/v1/saved-recaps` | Daftar dengan kueri `q` opsional, atau simpan filter dengan `name`, `period`, `from`, dan `to`. |
| GET, PUT, DELETE | `/v1/saved-recaps/{id}` | Baca, ubah, atau hapus filter tersimpan. |

Daftar, masuk, dan refresh mengembalikan `accessToken`, `refreshToken`, dan `businessName`. Keluar di server mencabut token refresh. Access token yang telah diterbitkan tetap berlaku hingga kedaluwarsa dalam 15 menit. Android menghapus kedua token saat keluar secara lokal. Jika permintaan keluar gagal, Android melaporkan bahwa pencabutan sesi server belum terkonfirmasi.

Endpoint data memakai `Authorization: Bearer <accessToken>` dan ruang data dari token. Daftar mengembalikan `{ "items": [] }` ketika kosong. Kategori dan sumber pembayar memiliki `id`, `name`, dan `note`. Jika masih dirujuk oleh draf aktif atau transaksi terkonfirmasi, permintaan hapus kategori atau sumber pembayar mendapat HTTP 409.

Isian pemasukan terdiri dari `amount`, `paidOn`, `payerName`, `reference`, `note`, `categoryId`, dan `payerId`. Backend menetapkan `id`, `source`, dan status draf (`draft` atau `converted`). Konfirmasi memerlukan nominal rupiah bulat positif dan tanggal valid. Transaksi dibuat lewat konfirmasi draf. Definisi rekap hanya menyimpan filter. Nilai `period` menerima `day`, `week`, `month`, atau `custom`.

Mode demo membuat contoh sintetis untuk kedua jalur ekstraksi tanpa menghubungi penyedia. Jalur teks menerima maksimal 8 KiB dan memeriksa penanda Livin’, transfer, serta berhasil. Contoh teks uji berisi `Livin’ by Mandiri`, `Transfer Berhasil`, `Nominal: Rp125.000`, dan `Tanggal: 2026-11-22` pada baris terpisah. Android harus menyaring teks sebelum mengirimnya. Backend menyaring ulang hanya nominal, tanggal, nama pengirim, dan referensi sebelum memanggil DeepSeek di mode penyedia.

Untuk PostgreSQL lokal, atur `DATABASE_URL` dan `JWT_SECRET` minimal 32 byte melalui lingkungan proses. Gunakan database khusus pengembangan. API tetap memakai mode demo dan alamat loopback. `ADDR` dapat memilih port atau alamat loopback lain.

`go test ./...` menjalankan uji API penyimpanan memori. Atur `REKAP_TEST_DATABASE_URL` ke database PostgreSQL uji jika ingin menjalankan skenario yang sama dengan skema uji terpisah. Role PostgreSQL harus diizinkan membuat skema.

## Pengaturan untuk pengujian staging

Untuk mengaktifkan penyedia aktual, tetapkan `REKAP_DEMO_MODE=false` dan pakai `DATABASE_URL` PostgreSQL khusus staging melalui TLS, `JWT_SECRET` minimal 32 byte, dan `DEEPSEEK_API_KEY`. `DEEPSEEK_MODEL` opsional dan bawaan `deepseek-flash`. Untuk menguji jalur gambar Baidu, tetapkan juga `BAIDU_OCR_API_KEY` dan `BAIDU_OCR_SECRET_KEY` berpasangan. Tanpa konfigurasi Baidu, jalur gambar memberikan HTTP 503. Jalur ML Kit masih memakai DeepSeek.

`ADDR` dapat dipakai untuk memilih alamat dan port backend. Jika hanya `PORT` ditentukan di mode penyedia, API mendengarkan pada port tersebut untuk reverse proxy. Gunakan endpoint HTTPS dengan TLS pada layanan hosting atau reverse proxy dan simpan rahasia pada lingkungan server. Label `production` pada `/health` berarti penyedia aktual dikonfigurasi. Uji staging tetap memerlukan bukti database, penyedia, dan APK sesuai [aturan proyek](../rules.md#pengujian-dan-rilis).
