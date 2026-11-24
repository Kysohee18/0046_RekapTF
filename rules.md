# Aturan Proyek RekapTF

## Acuan dan batas proyek

- [Pedoman Project Akhir PAM 2026](../../../../../download/PEDOMAN%20PROJECT%20AKHIR%20PAM%202026.pdf) memuat ketentuan wajib kuliah. [SRS RekapTF v0.4](../SRS-app/SRS_RekapTF_v0_4.docx) menjadi acuan perilaku aplikasi. Ikuti status persyaratan di SRS. Tandai rancangan percobaan, usulan, contoh teknis, dan keputusan terbuka sesuai statusnya.
- RekapTF mencatat pemasukan UMKM. Satu akun mewakili satu UMKM, dengan data terpisah dan satu peran `UMKM_USER`. Sediakan daftar, masuk dan keluar akun, JWT access token dan refresh token. Backend membatasi setiap operasi bisnis pada akun yang terautentikasi.
- Rilis awal memproses satu foto atau screenshot bukti transfer masuk Livin’ by Mandiri yang dipilih dan dipratinjau dari galeri. Pengguna memeriksa dan mengoreksi nominal, tanggal, dan isian lain sebelum mengonfirmasi draf. Konfirmasi berulang terhadap draf yang sama menghasilkan maksimal satu transaksi. Draf dan ekstraksi gagal dikecualikan dari rekap.
- Tampilkan riwayat dan detail transaksi, serta rekap harian, mingguan, bulanan, dan rentang tanggal pilihan. Rekap berisi total, jumlah, dan daftar transaksi terkonfirmasi yang dihitung. Simpan dan baca kembali snapshot rekap melalui berkas lokal tanpa mengimpor atau menggandakan transaksi. Format JSON dipakai oleh implementasi proyek asal. Definisi rekap tersimpan yang diusulkan menyimpan nama dan filter untuk menghitung ulang hasil dari transaksi terkonfirmasi.
- Pencatatan manual sebagai jalur cadangan berstatus usulan di SRS v0.4. Pengeluaran, kamera langsung, integrasi bank langsung, dan bukti dari aplikasi pembayaran lain berada di luar cakupan rilis awal.

## Ketentuan PAM dan Android

- Gunakan Kotlin, Jetpack Compose untuk UI dan navigasi antarlayar, MVVM, antarmuka berbahasa Indonesia, serta akses API Go melalui REST/JSON. Gunakan PostgreSQL cloud Neon sebagai penyimpanan server. PAM mengizinkan PostgreSQL cloud dan melarang Room.
- PAM mewajibkan sedikitnya empat entitas utama yang disimpan, saling berelasi, tampil di UI, dan masing-masing memiliki *dynamic fields*. Kandidat SRS adalah draf, transaksi pemasukan, kategori, dan sumber pembayar. Konfirmasikan ke dosen apakah field yang dapat diedit dan disimpan memenuhi istilah *dynamic fields*.
- PAM mewajibkan minimal lima fitur dengan Create, Read, Update, Delete, dan Search. Pertahankan lima kelompok usulan SRS untuk penelusuran dan verifikasi, yaitu draf bukti, transaksi pemasukan, kategori pemasukan, sumber pembayar atau pelanggan, dan definisi rekap tersimpan. Uji tiap operasi per kelompok. Lacak simpan dan baca berkas lokal sebagai ketentuan PAM tersendiri. Lacak autentikasi, isolasi akun, navigasi, dan staging sebagai pekerjaan pendukung. Hitung perubahan satu kali.
- Gunakan [dokumentasi utama Compose](https://developer.android.com/develop/ui/compose/documentation) dan buka halaman topik resmi sebelum memilih pola. Pisahkan UI, state, dan akses data secara proporsional dengan aliran data satu arah. Gunakan ViewModel dan repository sesuai kebutuhan. Tangani input, pemulihan state, loading, keadaan kosong, error, dan navigasi. Terapkan Material 3, layout adaptif bila relevan, semantics dan aksesibilitas, serta pemeriksaan performa dan UI. Pertahankan struktur folder yang jelas dan hindari dependensi atau lapisan yang tidak diperlukan.
- Periksa versi Kotlin, Gradle Wrapper, Android Gradle Plugin, Compose BOM, dan SDK dari konfigurasi proyek. Jelaskan alasan perubahan versi dan verifikasi API serta kompatibilitasnya pada dokumentasi resmi.
- PAM mewajibkan proyek individu dengan topik unik, selaras dengan SRS, dikumpulkan tepat waktu dan dipresentasikan sesuai jadwal. Persiapkan penjelasan, demo, dan wawancara selama 15–30 menit. Hadir 15 menit lebih awal dengan proyek siap dijalankan.

## Percobaan OCR Google ML Kit

SRS v0.4 menetapkan Baidu Cloud OCR di backend. Lampiran mengusulkan Google ML Kit Text Recognition v2 di Android sebagai penggantinya. Jalankan perubahan tersebut sebagai percobaan setelah ada tugas implementasi. Keputusan yang diperlukan adalah menerima hasil uji dan mengesahkan ML Kit melalui revisi SRS, atau mempertahankan Baidu sebagai alur rilis. [Panduan resmi ML Kit untuk Android](https://developers.google.com/ml-kit/vision/text-recognition/v2/android?hl=id)

- Uji model Latin *bundled* untuk memproses satu gambar langsung di Android. OCR dapat berjalan offline dan gambar sumber tetap di perangkat. `minSdk 24` pada scaffold memenuhi minimum API 23 pada panduan ML Kit. Profil perangkat minimum dan target performa masih perlu ditetapkan berdasarkan hasil uji.
- Tampilkan pratinjau dan status pemrosesan. Jaga layar tetap responsif. Kendalikan resolusi bila perlu sambil menjaga teks kecil terbaca. Sediakan pilihan untuk memilih ulang gambar ketika teks tidak terbaca.
- Beri tahu pengguna sebelum teks diproses melalui layanan AI. Kirim hanya teks OCR yang relevan melalui HTTPS ke API Go. API memvalidasi input dan sesi, meneruskan bagian yang diperlukan ke DeepSeek untuk menyusun draf, serta menyimpan data sesuai ruang UMKM di Neon. Autentikasi, DeepSeek, sinkronisasi, dan penyimpanan backend membutuhkan internet. Simpan kredensial layanan pada server.
- Uji pada HP kelas bawah menggunakan screenshot biasa, screenshot panjang, dan foto sintetis beresolusi tinggi. Catat ukuran APK, waktu OCR lokal, waktu hingga draf siap, puncak penggunaan memori, serta ketepatan nominal dan tanggal. Gunakan prosedur [debug Android](agent.md#debug-android).

## Git dan pengumpulan

- Selesaikan implementasi dan siapkan uji di `rekap-utama`. Seluruh commit baru hanya dilakukan di `rekap-repo` pada tahap pengumpulan yang diarahkan pengguna. Riwayat yang sudah ada di folder pengembangan dipertahankan. Gunakan [peta folder](agent.md#lokasi-kerja) untuk memeriksa lokasi.
- `rekap-repo` menerima fitur yang dipilih untuk pengumpulan mingguan selama empat minggu. Pemindahan hasil terpilih dan commit pengumpulan memerlukan arahan pengguna. Sampai ada arahan, gunakan folder itu untuk inspeksi. Dilarang menyalin atau menyinkronkan seluruh folder pengembangan, mengganti branch pengumpulan, atau menjalankan `git push` ke remote mana pun. Tindakan PR, unggah image, publish, deploy, dan penghapusan data memerlukan arahan terpisah.
- PAM mewajibkan repo GitHub bernama `4NIMBelakang_NamaProject`, minimal 30 commit, dan setidaknya satu pembaruan progres yang di-commit ke README GitHub setiap minggu secara bertahap. Pengumpulan satu kali ketika proyek sudah selesai tidak memenuhi rubrik progres. PAM mengizinkan beberapa branch. Pengguna perlu menentukan dan menjalankan alur publikasi untuk memenuhi kewajiban GitHub selama push oleh agent dilarang.
- Setelah pengumpulan diizinkan, commit setiap perubahan parsial yang bermakna atau perubahan fungsi sebagai satu irisan yang dapat ditinjau di `rekap-repo`. Pakai [peta Sprint 1](log-kerja.md#peta-commit-sprint-1) untuk mengaitkan tiap irisan dengan fitur dan bukti. Catat kode yang sudah ada sebelum riwayat sebagai kondisi awal. Hindari commit tunggal berisi seluruh aplikasi yang sudah jadi.
- Setelah commit diizinkan, targetkan 40–70 commit bermakna per fitur dan rencanakan sekitar 40–70 per minggu mengikuti pekerjaan nyata. Jumlah dapat melebihi target jika tiap commit bermakna. Jika target belum tercapai, catat jumlah aktual dan alasannya. Tinjau progres setiap 2–3 hari. Gunakan satu perubahan yang bisa dijelaskan per commit. Hindari commit kosong, pemecahan artifisial, dan pemunduran tanggal.
- Gunakan pesan commit campuran Indonesia dan Inggris yang alami dan relevan, misalnya `Perbaiki retry saat refresh token gagal`. Gunakan pesan tanpa trailer, termasuk `Signed-off-by` dan `Co-authored-by`. Jika repo mewajibkan trailer, laporkan pertentangannya sebelum commit dan minta keputusan.
- Periksa berkas yang akan dicatat Git. Jauhkan kredensial, `.env`, token, keystore, data pelanggan, bukti transaksi pribadi, data uji sensitif, cache, dan hasil build dari Git. Gunakan penyimpanan rahasia server untuk kredensial Neon, Baidu ketika alur tersebut dipakai, dan DeepSeek. Lindungi token sesi sesuai platform dan jangan mencatat token, kata sandi, teks OCR, atau gambar bukti lengkap ke log.

## README dan pencatatan

- Gunakan tabel sprint tanpa tanggal yang berisi fitur, status, serta bukti. Sprint 1 memuat **Setup Awal Proyek**. Tampilkan tepat satu diagram alur dari SRS yang berlaku.
- Gunakan ✅ hanya jika pekerjaan terkait sudah tercatat dalam repo pengumpulan aktif `rekap-repo` dan hash-nya dapat diperiksa. Tampilkan hash pada baris terkait. Gunakan ☐ untuk rencana, pekerjaan lokal, atau uji yang belum masuk riwayat pengumpulan aktif. Catat tanggal, bukti uji, dan hash historis di [log kerja](log-kerja.md).

## Pengujian dan rilis

| Lingkungan | Bukti yang diperlukan |
|---|---|
| Lokal | Catat mode backend, skenario, dan artefak yang dipakai. Gunakan data sintetis untuk demo serta pengujian lokal. Mode demo, respons simulasi provider, dan PostgreSQL lokal adalah bukti lokal. Ikuti [prosedur debug](agent.md#debug-android) bila memakai APK. |
| Staging | Nyatakan staging teruji hanya setelah tersedia bukti Neon melalui TLS, OCR Google ML Kit pada APK, layanan DeepSeek yang sebenarnya, serta uji APK ke API Go melalui endpoint HTTPS. Gunakan data sintetis untuk alur draf, koreksi, satu konfirmasi efektif, dan rekap. Selama Baidu masih menjadi acuan SRS, laporkan uji alur Baidu sebagai bukti parsial dan tandai syarat Google masih terbuka. `/health` hanya memeriksa respons API dan database. |
| Produksi | Setelah staging lulus dan pengguna memberi arahan rilis, verifikasi APK rilis dan endpoint HTTPS produksi. Gunakan lingkungan, database, dan kredensial terpisah. Periksa konfigurasi akses, pemantauan, dan pemulihan sesuai kebutuhan rilis sebelum menerima data pelanggan. Sebutkan lingkungan aktual ketika mode API bernama `production`. |

Lihat [konfigurasi staging backend aktif](backend/README.md#pengaturan-untuk-pengujian-staging) untuk mode penyedia aktual serta konfigurasi ML Kit dan Baidu.

## Revisi SRS

Catat alasan dan keputusan saat kebutuhan aplikasi berubah. Gunakan [template Word asli](../../../../../download/SRS_PAM/Software%20Requirement%20Specification%20Document%20Template.docx), tulis isi SRS dalam bahasa Inggris, dan pertahankan footer. Jika percobaan ML Kit diterima, perbarui alur OCR, pemberitahuan pemrosesan AI, kontrak API, diagram terkait, serta skenario uji lokal dan staging.
