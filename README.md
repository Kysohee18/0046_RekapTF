# RekapTF

RekapTF membantu UMKM mencatat pemasukan dari bukti transfer Livin’ by Mandiri dan melihat rekap per periode. Pengembangan dilakukan bertahap sesuai [log kerja](log-kerja.md).

## Progres sprint

| Sprint | Fitur | Status | Bukti |
|---|---|---|---|
| 1 | Setup Awal Proyek | ✅ Commit `1933332` | Scaffold Compose, katalog dependensi, dan Gradle Wrapper ada di `rekap-repo`. Build lokal lulus. |
| 1 | Debug USB dan navigasi | ✅ Commit `8c42a85`, `57264ea` | Konfigurasi emulator/USB ada di repo. Uji APK dengan perangkat menyusul. |
| 1 | Akun UMKM | ✅ Commit `561cce9`, `a1fedbd` | API daftar, masuk, rotasi refresh, keluar, dan profil diuji lokal; layar Masuk/Daftar mengikuti desain. |
| 1 | Kategori pemasukan dan daftar pengirim | ✅ Commit `b855422`, `e14e719` | CRUDS dan pencarian nama lulus di penyimpanan memori; daftar kategori/pelanggan memakai komponen desain baru. |
| 1 | Transaksi pemasukan | ✅ Commit `6947004` | Riwayat bertanggal, ringkasan filter, detail, edit, dan hapus. Konfirmasi tetap berasal dari draf. |
| 1 | UI sesuai PRD desain v1.0 | ✅ Commit `11db578`, `d992626`, `4bdb040` | Token warna, tipografi, komponen, Beranda, dan alur bukti diselaraskan. Kompilasi dan penggabungan resource lulus; uji perangkat belum. |
| 2 | Draf OCR dan staging | ✅ Commit `012b205`, `4bdb040` | Jalur Baidu dan ML Kit tersedia. Staging menunggu bukti APK, HTTPS, Neon, dan DeepSeek. |
| 3 | Rekap, berkas lokal, dan demo | ✅ Commit `c992d9b` | Rekap periode, filter tersimpan, serta tulis dan buka snapshot siap diuji lewat APK. |
| 4 | Pengumpulan fitur terpilih | ☐ Direncanakan | Cakupan ditetapkan setelah hasil sprint sebelumnya. |

## Alur aplikasi

![Diagram aktivitas RekapTF dari SRS v0.4](docs/alur-rekaptf.png)

Sumber diagram adalah [SRS RekapTF v0.4](../SRS-app/SRS_RekapTF_v0_4.docx).

## Pengembangan

Jalankan API lokal dari root proyek dengan Go 1.26:

```powershell
Set-Location backend
go run .
```

API mendengarkan di `127.0.0.1:8080` dalam mode demo. Gunakan data sintetis. Akun, kategori, dan pengirim dalam penyimpanan memori hilang saat API berhenti. [Kontrak API dan opsi PostgreSQL lokal](backend/README.md)

### Uji APK sebelum produksi

1. Buka [root pengembangan](agent.md#lokasi-kerja) di Android Studio dengan SDK Platform 37. Pengguna menjalankan Gradle Sync dan build Android.
2. Untuk **Run dari Android Studio ke HP lewat USB**, buat `rekap-debug.local.properties` di root proyek dengan isi `rekapDebugApiBaseUrl=http://127.0.0.1:8080`, lalu jalankan Run pada varian `debug`. Berkas ini diabaikan Git. Saat kembali memakai emulator, kosongkan nilai atau hapus berkas agar debug kembali ke `http://10.0.2.2:8080`.

   Untuk build dari terminal, `-PrekapApiBaseUrl` diprioritaskan di atas konfigurasi lokal. Contoh untuk HP lewat USB:

   ```powershell
   .\gradlew.bat :app:assembleDebug "-PrekapApiBaseUrl=http://127.0.0.1:8080"
   ```

   Jalankan [skrip debug USB](scripts/prepare-usb-debug.ps1) dari root saat APK dan perangkat siap. Skrip memeriksa kesehatan API lokal dan pemetaan port ADB.
3. Daftar akun sintetis A. Periksa pesan saat konfirmasi kata sandi berbeda. Tambah kategori dan pengirim penanda A. Keluar, buat akun sintetis B dan penandanya sendiri, lalu periksa bahwa setiap akun hanya melihat penandanya.
4. Dari akun A, pilih [screenshot sintetis](docs/fixtures/bukti-sintetis.png). Baca pemberitahuan pemrosesan, lalu buat draf. Periksa nominal Rp125.000 dan tanggal 22 November 2026, koreksi nilai jika perlu, lalu simpan draf. Periksa rekap rentang tanggal draf sebelum konfirmasi. Setelah konfirmasi hanya satu transaksi yang harus masuk. Uji cari, buka detail, edit, dan hapus transaksi. Saat transaksi masih merujuk kategori atau pengirim, uji penolakan hapus referensi tersebut.
5. Coba rekap harian, mingguan, bulanan, dan rentang pilihan. Simpan filter bernama, cari, edit, buka dan hapus filter. Putar perangkat saat pemilih tujuan snapshot JSON terbuka, lanjutkan penyimpanan, lalu buka berkas tersebut. Isi dan total snapshot harus sesuai dengan rekap yang dipilih ketika ekspor dimulai. Jumlah transaksi aktif tetap sama.
6. Ulangi percobaan ML Kit pada HP kelas bawah dengan [screenshot panjang](docs/fixtures/bukti-sintetis-panjang.png) dan [foto sintetis resolusi tinggi](docs/fixtures/bukti-sintetis-resolusi-tinggi.jpg). Uji pilih ulang gambar, kegagalan jaringan, dan keluar saat API berhenti. Catat varian dan ukuran APK, perangkat, waktu OCR lokal, waktu hingga draf, puncak memori, serta ketepatan nominal dan tanggal di [log kerja](log-kerja.md).

Build debug memakai ML Kit Latin *bundled* secara bawaan. Untuk membandingkan jalur Baidu pada build debug, tambahkan `-PrekapLocalOcr=false`. Mode demo menghasilkan draf sintetis. Ketepatan nominal dan tanggal dari penyedia sebenarnya hanya dapat dinilai saat staging aktif. Gunakan [aturan pengujian](rules.md#pengujian-dan-rilis) dan [kontrak server](backend/README.md) saat menyiapkan HTTPS, Neon dan kredensial server.
