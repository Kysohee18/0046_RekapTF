# Panduan Agent RekapTF

Mulai dari `AGENTS.md` yang berlaku untuk workspace. Baca [AGENTS.md proyek asal](../../rekaptf/AGENTS.md) sebagai konteks, lalu gunakan [aturan proyek](rules.md) dan [log kerja](log-kerja.md) di folder kerja aktif. Gunakan kaye-mode untuk pekerjaan non-trivial.

## Lokasi kerja

| Lokasi | Path absolut | Penggunaan |
|---|---|---|
| Folder pengembangan aktif `rekap-utama` | `D:\KULIAH\semester-5\pam\TUBES\rekap-utama` | Selesaikan implementasi dan siapkan pengujian Android serta API di sini. Periksa diff. Riwayat Git yang ada berfungsi sebagai referensi. |
| Folder pengumpulan aktif `rekap-repo` | `D:\KULIAH\semester-5\pam\TUBES\rekap-repo` | Satu-satunya lokasi commit untuk fitur terpilih per minggu selama empat minggu. Pemindahan dan commit mengikuti arahan pengumpulan pengguna. |
| Proyek pengembangan asal `rekaptf` | `D:\KULIAH\semester-5\pam\rekaptf` | Akses baca untuk dokumen, kode, serta bukti kondisi awal. |
| Repo pengumpulan asal `0046_RekapTF` | `D:\KULIAH\semester-5\pam\0046_RekapTF` | Akses baca saja untuk riwayat Git, hash commit historis, dan perbandingan. |

Periksa path absolut sebelum membuka proyek atau menulis. Jika folder sudah memakai Git, cocokkan root dengan `git -C "<path-repo>" rev-parse --show-toplevel`. Laporkan lokasi yang keliru, repo yang belum tersedia, atau akses yang ditolak kepada Lead. Lanjutkan bagian yang independen. Untuk membaca repo pengumpulan, gunakan `git --no-optional-locks -C "<path-repo>"` dengan subperintah baca seperti `status`, `log`, `show` atau `diff`.

## Urutan kerja

1. Baca aturan, log, bagian SRS yang terkait, dan instruksi tugas. Periksa root, status Git, perubahan yang sedang dikerjakan pengguna, serta bukti dan artefak yang sudah tersedia.
2. Tetapkan lingkup, kepemilikan berkas, kriteria penerimaan yang bisa diamati, serta bukti yang dibutuhkan. Jika ketentuan bertentangan, sebutkan kedua butir dan keputusan yang diperlukan. Lanjutkan pekerjaan yang tidak bergantung pada keputusan itu.
3. Terapkan perubahan terkecil yang memenuhi kriteria. Gunakan pola proyek. Bagi kepemilikan berkas tanpa tumpang tindih dan pertahankan perubahan yang sudah ada.
4. Periksa hasil sesuai lingkup. Catat versi atau mode, perintah dan hasil aktual, serta bagian yang belum diuji di [log kerja](log-kerja.md). Ketentuan tiap lingkungan ada di [aturan pengujian](rules.md#pengujian-dan-rilis).
5. Setelah semua penulis berhenti, minta Reviewer independen memeriksa kondisi akhir secara baca saja. Lead mengambil keputusan berdasarkan hasil review. Jika ada perbaikan, minta Reviewer memeriksa ulang bagian yang terdampak.
6. Selesaikan dan uji alur aplikasi di folder pengembangan sebelum menyiapkan pengumpulan atau rilis. Catat hasil uji yang masih menunggu APK pengguna. Ikuti [aturan Git](rules.md#git-dan-pengumpulan) bila pengguna mengarahkan pengumpulan fitur terpilih.

## Delegasi dan review

- Untuk pekerjaan aplikasi non-trivial, gunakan peran native secara bertahap, dengan maksimal dua spesialis aktif bersamaan. UI/UX Designer mengkaji alur, Material 3, responsivitas, dan aksesibilitas lalu menyerahkan spesifikasi ringkas kepada Builder. Builder mengimplementasikan sesuai lingkup berkas dan kriteria. Peran UI/UX dapat dilewati untuk satu layar sederhana jika tidak menambah manfaat.
- Berikan **kedua path aktif absolut** kepada setiap agent. Sertakan kedua path historis jika tugas membutuhkan pemeriksaan bukti lama. Tetapkan tujuan, kepemilikan berkas, kondisi lulus, dan format hasil. Minta agent memeriksa lokasi sebelum menulis. Reviewer menerima kondisi akhir setelah penulis selesai.
- Reviewer memeriksa diff, kesesuaian dengan SRS dan lingkup, kompatibilitas, potensi regresi, aksesibilitas UI jika relevan, serta hasil pemeriksaan yang tersedia. Berikan tepat satu verdict (`PASS`, `PASS WITH LIMITS`, atau `FAIL`) dengan temuan, path, bukti, perintah, hasil, dan bagian yang belum diverifikasi. Hentikan penerimaan bila ditemukan overengineering dan laporkan bagian yang perlu disederhanakan. Jika Reviewer tidak tersedia, laporkan bahwa QA independen belum selesai.
- Pastikan peran dan model yang benar-benar tersedia di runtime. Jika pilihan model tersedia, utamakan Terra high untuk UI/UX serta Sol 6.2 high untuk Builder dan Reviewer. Lead memakai reasoning tinggi bila tersedia. Jika pilihan itu tidak tersedia, pakai peran native yang dapat dijalankan dan laporkan batasnya. Penggunaan free-pi dengan `deepseek-v4.1-flash` ditangani sendiri oleh pengguna bila dipilih.

## Debug Android

- Untuk sesi debug, buka root absolut `rekap-utama` di Android Studio setelah sumber aplikasi tersedia. Cocokkan lokasi proyek sebelum pengujian.
- Pengguna menjalankan build Android, termasuk Gradle Sync, Run, dan semua task Gradle. Agent boleh memeriksa sumber dan konfigurasi serta menggunakan ADB dan Logcat pada APK yang disediakan pengguna sesuai skenario yang disepakati.
- Catat identitas APK, versi atau varian dan endpoint bila diketahui, perangkat atau emulator beserta versi Android/API, skenario, dan hasil yang benar-benar diamati. Tandai identitas yang tidak tercatat. Bedakan pemeriksaan sumber dan APK yang berjalan.

Laporan Lead merangkum hasil, berkas yang berubah, asumsi dan keputusan, perintah build dan uji beserta hasil yang tersedia, verdict Reviewer, batas verifikasi, serta tindakan selanjutnya.
