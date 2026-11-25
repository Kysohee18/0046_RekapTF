# Log Kerja RekapTF

[Panduan kerja](agent.md) · [Aturan proyek](rules.md) · [SRS v0.4](../SRS-app/SRS_RekapTF_v0_4.docx)

Tanggal memakai waktu Jakarta. Pakai status **Direncanakan**, **Selesai lokal**, **Diuji lokal**, **Diuji staging**, **Diuji produksi**, **Commit pengembangan**, atau **Commit pengumpulan** sesuai bukti. Jika satu baris mencakup beberapa tahap, sebutkan bagian yang mencapai tiap tahap. Sebutkan repo tempat hash tercatat dan catat publikasi hanya jika ada bukti. Pakai `—` jika belum ada hash yang relevan.

## Kondisi awal yang dicatat 23 November 2026

Tanggal di bawah adalah tanggal pencatatan kondisi awal. Tanggal masing-masing pelaksanaan uji lama tidak tercantum. Hasil uji lama diatribusikan ke catatan terdahulu dan dokumen pendukung.

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 23 Nov 2026 | 1 | Dokumen | SRS v0.4 berbahasa Inggris dengan template Word, footer dan empat diagram. PRD awal menjelaskan alur, batas fitur, dan kriteria penerimaan. | SRS dan [PRD awal](../../rekaptf/docs/PRD_RekapTF_Review_v0_1.md) tersedia. SRS masih draf review dengan keputusan terbuka. | — | Selesai lokal |
| 23 Nov 2026 | 1 | Android dan akun | Kode Kotlin, Compose, Navigation Compose dan ViewModel. Layar akun, bukti, draf, riwayat, kategori, pengirim dan rekap tersedia. Sumber memakai tab Beranda, Bukti, Transaksi dan Rekap. | Log lama merekam pendaftaran dan pemasukan manual sintetis Rp135.000 tampil di rekap pada APK via USB. APK yang diuji masih memakai menu **Lainnya**. Identitas APK dan perangkat belum tercatat. | — | Diuji lokal pada APK lama. Navigasi terbaru diperiksa dari sumber. |
| 23 Nov 2026 | 1–2 | API, autentikasi dan database lokal | Go REST/JSON, JWT, isolasi akun, lima kelompok CRUDS dan konfirmasi draf idempoten. | Log lama dan [catatan implementasi](../../rekaptf/docs/CATATAN_IMPLEMENTASI.md) merekam hasil API demo dan PostgreSQL lokal, termasuk konfirmasi ulang tanpa duplikat dan pembacaan setelah API dimulai ulang. | — | Diuji lokal |
| 23 Nov 2026 | 2 | OCR dan AI | Adapter Baidu General Basic dan DeepSeek tersedia. Mode demo menghasilkan nilai sintetis. | Respons provider disimulasikan. Catatan lama menyebut Rp125.000 dari pembacaan gambar contoh di chat. Baidu dan DeepSeek aktual belum terbukti melalui aplikasi. | — | Diuji lokal dengan simulasi |
| 23 Nov 2026 | 3 | Berkas lokal | Simpan dan buka ulang snapshot JSON. [Snapshot sintetis](../../rekaptf/docs/test-fixtures/rekap-uji-import.json) memuat satu transaksi Rp135.000. | Log lama merekam simpan dan buka ulang lulus di emulator. Uji buka berkas di HP masih terbuka. | — | Diuji lokal pada emulator |
| 23 Nov 2026 | 2 | Konfigurasi staging | [Blueprint Render](../../rekaptf/render.yaml), [Dockerfile](../../rekaptf/backend/Dockerfile), dan [panduan staging](../../rekaptf/docs/PRODUCTION_SETUP.md) tersedia. | Log lama mencatat pemeriksaan statis lulus dengan batasan. Laporan Reviewer terpisah dengan perintah dan hasil tidak tersedia dalam bukti yang diperiksa. Integrasi staging dan APK belum terbukti. | — | Selesai lokal |

## Riwayat perubahan

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 23 Nov 2026 | 1 | Penyiapan kerja | Menetapkan folder kerja dan pengumpulan, aturan commit, serta kondisi awal. Pada tahap awal `rekaptf` berada di `main` tanpa commit atau remote dan folder pengumpulan masih kosong. | Pemeriksaan filesystem dicatat di log lama. Keadaan pengumpulan selanjutnya terbukti pada dua baris berikut. | — | Selesai lokal |
| 23 Nov 2026 | 1 | Setup Awal Proyek | Kondisi awal disalin ke repo pengumpulan. Proyek kerja dibuka di Android Studio. | Jendela Android Studio tercatat di log lama. Git menunjukkan commit kondisi awal di `main` pada `0046_RekapTF`. | `87c6a8bda6868ee8ee96ee23ab885a09f1ecc7b9` | Commit pengumpulan lokal |
| 23 Nov 2026 | 1 | Alur kerja sistem | Menambahkan diagram aktivitas SRS dan proses tinjauan branch ke README. | `git show` mengonfirmasi perubahan README pada `codex/penyiapan-sprint-awal`. Berkas `docs/diagram-assets/02_activity.png` identik dengan gambar aktivitas tertanam di SRS v0.4. | `bf4bb954b220ddc73afba599bd5e3c965778a431` | Commit pengumpulan lokal |
| 23 Nov 2026 | 2 | Draf OCR | Memperjelas pemilihan bukti, tindakan coba ulang, serta pemeriksaan nominal dan tanggal. | Log lama mencatat pemeriksaan XML dan uji HTTP demo untuk draf sintetis, koreksi, serta konfirmasi idempoten. UI terbaru belum diuji di HP. | — | Selesai lokal. Alur API demo diuji lokal. |
| 23 Nov 2026 | 2 | Persiapan staging | Membangun image backend `rekaptf-api:staging-local` untuk Linux AMD64 dan mendokumentasikan jalur Render Existing Image. | Log lama mencatat tes Go dan HTTP dengan PostgreSQL lokal untuk autentikasi, isolasi akun, CRUDS kategori, pengirim dan rekap tersimpan, transaksi, serta rekap. Akun uji dapat diakses setelah API dimulai ulang. APK staging, Neon, Baidu, dan DeepSeek aktual belum diuji dalam pekerjaan tersebut. | — | Diuji lokal |
| 23 Nov 2026 | 1 | Penetapan workspace | Pengguna menetapkan `TUBES/rekap-utama` sebagai folder pengembangan dan `TUBES/rekap-repo` sebagai tujuan pengumpulan fitur mingguan selama empat minggu. | Pemeriksaan filesystem awal menunjukkan kedua folder kosong. Riwayat pengumpulan sebelumnya diperiksa terpisah di `0046_RekapTF`. | — | Keputusan dicatat |
| 23 Nov 2026 | 1 | Aturan dan bukti kerja | Menata panduan agent, aturan proyek, dan log di `TUBES/rekap-utama` berdasarkan dokumen awal, PAM, SRS dan lampiran. | `git status`, `git log --all` dan `git show` digunakan pada repo asal. PDF PAM dan DOCX SRS dibaca, konfigurasi proyek asal diperiksa, dan diagram README dicocokkan per byte dengan media SRS. | — | Selesai lokal |
| 23 Nov 2026 | 1 | Review dokumentasi | Reviewer menemukan syarat Google ML Kit untuk status staging masih ambigu. Aturan staging diperjelas dan uji Baidu dicatat sebagai bukti parsial. | Review awal `FAIL` pada aturan staging. Pemeriksaan ulang memberi `PASS` untuk tiga dokumen, termasuk 29 tautan lokal dan tabel. Dua hash historis diverifikasi pada review awal. | — | Review dokumen lulus |
| 23 Nov 2026 | 1 | Koreksi alur kerja | Pengguna menetapkan `rekap-utama` untuk pengembangan sampai siap diuji. Perubahan parsial atau fungsi yang bermakna akan di-commit hanya di `rekap-repo` dengan pesan campuran Indonesia dan Inggris. | Sebelum koreksi, Git pengembangan sudah berisi sembilan commit sampai `39267bf`. Hash tersebut dipertahankan sebagai riwayat dan folder pengumpulan masih kosong saat diperiksa. | — | Keputusan dicatat |

## Rencana empat sprint

Tanggal dan titik tinjau Sprint 1–3 berasal dari rencana lama yang dicatat 23 November 2026. Pengumpulan aktif direncanakan selama empat minggu. Bukti pekerjaan per fitur ada pada riwayat di atas.

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 23–29 Nov 2026, tinjau 23, 25 dan 27–29 Nov | 1 | Setup Awal Proyek, akun, pemasukan, kategori dan pengirim | Struktur repo, debug USB, akun, jalur manual yang diusulkan, kategori dan pengirim. | Catat login, transaksi, navigasi dan CRUDS yang sudah ditinjau. | — | Direncanakan |
| 30 Nov–6 Des 2026, tinjau 30 Nov–1 Des, 3 dan 5–6 Des | 2 | Draf OCR dan staging | Rencana awal mencakup Neon, Render, Baidu, DeepSeek, draf dan konfirmasi. Percobaan Google ML Kit mengikuti syarat di [aturan proyek](rules.md#percobaan-ocr-google-ml-kit). | Uji API dan database, serta bukti sintetis melalui APK hingga draf diperiksa dan rekap bertambah tepat sekali. | — | Direncanakan |
| 7–13 Des 2026, tinjau 7–8, 10 dan 12–13 Des | 3 | Rekap, berkas, SRS dan demo | Rekap tersimpan, snapshot lokal, verifikasi lima CRUDS, SRS dan persiapan demo. | Catat bukti lintas fitur dan hasil Reviewer. README pengumpulan mengikuti izin dan hash yang tersedia. | — | Direncanakan |
| Belum ditetapkan | 4 | Pengumpulan fitur minggu keempat | Fitur dan titik tinjau akan ditetapkan berdasarkan hasil sprint sebelumnya dan jadwal pengguna. | Catat bukti fitur terpilih dan hash pengumpulan aktif jika tersedia. | — | Direncanakan |

## Peta commit Sprint 1

Peta ini dipakai untuk mengaitkan pekerjaan nyata dengan bukti uji di folder pengembangan dan commit fitur terpilih di folder pengumpulan setelah diotorisasi. Bukti historis tetap berada pada tabel riwayat. Pesan dan jumlah commit mengikuti [aturan Git](rules.md#git-dan-pengumpulan).

| Urutan | Fitur | Irisan perubahan yang direncanakan | Bukti yang dicatat |
|---|---|---|---|
| 1 | Setup Awal Proyek | Penyiapan struktur yang dikerjakan pada sprint, aturan dan log, serta konfigurasi debug sesuai lingkup. | Root, inventaris berkas, diff, dan pemeriksaan konfigurasi. |
| 2 | Debug USB dan navigasi | Pemeriksaan alur navigasi dan perbaikan yang benar-benar diperlukan. | APK, perangkat, endpoint, skenario dan hasil pengguna atau agent sesuai [prosedur debug](agent.md#debug-android). |
| 3 | Akun UMKM | Perubahan bertahap pada daftar, masuk, keluar, atau isolasi data akun sesuai temuan. | Uji autentikasi dan akses lintas akun. |
| 4 | Pemasukan, kategori dan pengirim | Perubahan transaksi, kategori, atau pengirim yang dapat ditinjau per fitur. Jalur manual mengikuti status usulan SRS. | Hasil operasi CRUDS yang tersentuh dan dampaknya pada riwayat atau rekap. |

## Pelacakan commit

Target 40–70 per fitur mengikuti [aturan Git](rules.md#git-dan-pengumpulan). Pada awal sesi dokumentasi 23 November 2026, `rekap-utama` berisi tiga dokumen dan `rekap-repo` kosong. Keduanya saat itu belum memiliki repo Git. Pada repo asal, `rekaptf` masih di `main` tanpa commit. `0046_RekapTF` memiliki dua commit lokal untuk kondisi awal, penyiapan sprint, dan diagram. `git remote` tidak menampilkan remote pada kedua repo asal. Dua commit historis tersebut merekam kondisi awal dan penyiapan sprint. Riwayat GitHub dan progres mingguan yang dipublikasikan belum terverifikasi.

## Hasil pelaksanaan Sprint 1 di rekap-utama

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 23 Nov 2026 | 1 | Setup Awal Proyek | Inisialisasi Git lokal pada `main`, scaffold Compose, katalog dependensi, Gradle Wrapper dan README. Diagram aktivitas diambil dari SRS v0.4. | Validasi XML dan katalog, pemeriksaan Git, serta kecocokan byte diagram dengan media SRS. | `e03fdbc` | Commit pengembangan |
| 23 Nov 2026 | 1 | Debug USB | URL debug untuk emulator dan HP melalui USB. HTTP dibatasi ke `10.0.2.2` dan `127.0.0.1` pada debug. | Manifest dan aturan jaringan XML diperiksa. Skrip USB lolos parser PowerShell. Perangkat dan APK belum diuji. | `f14b663` | Commit pengembangan |
| 23 Nov 2026 | 1 | API akun UMKM | API Go 1.26.0 menyediakan daftar, masuk, rotasi refresh, keluar, profil dan otorisasi satu peran `UMKM_USER`. | `go test -count=1 -v ./...` dan uji HTTP loopback lulus pada demo/memori. PostgreSQL dilewati karena DSN uji belum tersedia. | `7f3a387` | Commit pengembangan. API diuji lokal. |
| 23 Nov 2026 | 1 | Layar akun dan navigasi | Compose dan ViewModel menghubungkan daftar, masuk, Beranda dan keluar ke API. Token disimpan di memori sesi. | Referensi resource dan konfigurasi diperiksa. Build Android dan navigasi dari APK menyusul. | `d5d343f` | Commit pengembangan |
| 23 Nov 2026 | 1 | API kategori dan pengirim | API melakukan tambah, baca, ubah, hapus dan cari pada ruang akun dari JWT. | Tes Go dan HTTP loopback lulus pada demo/memori. Permintaan baca, ubah dan hapus lintas akun mendapat HTTP 404. | `6b70f52` | Commit pengembangan. API diuji lokal. |
| 23 Nov 2026 | 1 | Layar kategori dan pengirim | Navigasi Compose, daftar, pencarian nama, editor, dan konfirmasi hapus terhubung ke API. Client memperbarui token akses ketika sesi perlu disegarkan. | Pemeriksaan statis Android lulus. `go vet ./...`, `go test -count=1 -v ./...`, build Go dengan `-buildvcs=false` dan HTTP loopback lulus untuk aturan cari nama. Uji APK dan PostgreSQL masih terbuka. | `2bed8e8` | Commit pengembangan. API diuji lokal. |
| 23 Nov 2026 | 1 | Perbaikan hasil review | Tambah konfirmasi kata sandi, akhiri sesi lokal ketika server tidak terjangkau, gunakan label deskripsi kategori dan catatan pengirim, serta pertahankan dua penanda akun dalam uji isolasi. | Review awal `FAIL`. Pemeriksaan ulang pada perbaikan memberi `PASS WITH LIMITS`. Diff Git, resource Android, dan pemeriksaan Go lulus dalam lingkup sumber dan API memori. | `54503f3` | Commit pengembangan. Uji perangkat dan PostgreSQL masih terbuka. |

Tujuh commit implementasi di atas tercatat di repo pengembangan, termasuk tiga yang menangani akun dan dua yang mengerjakan kategori serta pengirim. Satu commit perbaikan mencakup lintas fitur dan dihitung satu kali. Catatan panduan APK tercatat terpisah pada `02f1246`. Fitur draf, transaksi, dan rekap tersimpan masih memiliki 0 commit implementasi Sprint 1. Target 40–70 per fitur masih terbuka karena pekerjaan baru mencakup irisan awal. Hash pada tabel ini adalah commit pengembangan di `rekap-utama` dan tidak dapat di-resolve di repo pengumpulan karena isinya dipindahkan sebagai potongan berkas; padanan publikasinya ada pada tabel Pengumpulan Sprint 1 ke GitHub di bawah.

## Implementasi menuju uji pra-produksi

Kolom hash untuk irisan di bawah diisi setelah ada commit pengumpulan yang diotorisasi. Git pengembangan tetap di `39267bf`.

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 23 Nov 2026 | 2 | Draf OCR | Alur satu gambar, pratinjau, pemberitahuan, praproses dan ekstraksi. Build debug memakai ML Kit Latin *bundled* dan mengirim teks terpilih. Jalur Baidu tersedia untuk pembandingan. | Uji Go memvalidasi kedua endpoint dan simulasi DeepSeek menerima bidang terpilih. Tiga berkas gambar sintetis tersedia untuk perangkat. OCR dari APK dan layanan asli masih terbuka. | — | Implementasi lokal. API diuji lokal. |
| 23 Nov 2026 | 2 | Draf dan transaksi | Penyimpanan, pencarian dan koreksi draf, konfirmasi idempoten, riwayat, detail, edit, dan hapus transaksi. Kategori dan pengirim yang dipakai terlindung dari penghapusan. | Uji Go memeriksa isolasi dan konfirmasi serentak. Uji HTTP loopback mode demo/memori mengonfirmasi koreksi Rp35.000 dan pengulangan konfirmasi menghasilkan satu transaksi. | — | Implementasi lokal. API diuji lokal. |
| 23 Nov 2026 | 3 | Rekap dan berkas lokal | Rekap periode, CRUDS filter yang menghitung ulang hasil, serta simpan dan baca snapshot JSON melalui pemilih berkas Android. | Uji Go dan HTTP loopback lulus dengan penyimpanan memori untuk filter dan rekap transaksi terkonfirmasi. Tulis dan buka berkas dari APK akan diuji pada perangkat. | — | Implementasi lokal. API diuji lokal. |
| 23 Nov 2026 | 3 | Persiapan uji | Menambahkan tiga ukuran gambar sintetis dan skenario perangkat di README. Konfigurasi aplikasi memisahkan demo loopback, staging HTTPS, dan rilis. | `go test -buildvcs=false -count=1 -v ./...`, `go vet ./...`, dan build Go lulus. Subuji PostgreSQL dilewati karena DSN uji belum tersedia. | — | Siap untuk build APK pengguna. Staging belum diuji. |
| 23 Nov 2026 | 3 | Pemeriksaan ulang pra-produksi | Memperketat deteksi status transfer sukses dan menahan rekap yang dipilih saat pemilih berkas terbuka, termasuk saat rotasi. | Review awal `FAIL` untuk dua temuan. Pemeriksaan ulang `PASS WITH LIMITS`. Dua status gagal mendapat HTTP 422 pada uji lokal, dan kedua jalur demo menghasilkan draf. Parser Kotlin 2.2.10 menerima 16 sumber Kotlin dan 3 skrip Gradle secara statis. | — | Siap untuk build APK pengguna. Rotasi perangkat dan layanan aktual masih perlu diuji. |
| 23 Nov 2026 | 3 | Koneksi API lewat USB | Pemeriksaan DEX pada APK terpasang `id.ac.rekaptf` versi `0.1.0`, diperbarui pukul 19.06 WIB, menemukan endpoint emulator `http://10.0.2.2:8080`. API lokal awalnya menolak koneksi. Backend Go demo dinyalakan dan port `8080` dipetakan melalui ADB reverse. | `/health` merespons dari PC dan melalui USB pada HP model `24069PC21G`, Android 16 (API 36). HP menerima HTTP 200 dengan `mode: demo` dan `storage: memory`. Hasil ini menguji jalur USB melalui ADB shell. | — | Koneksi USB teruji. APK dengan endpoint `http://127.0.0.1:8080` dan alur daftar atau masuk masih menunggu build dan uji pengguna. |
| 23 Nov 2026 | 3 | Konfigurasi Run untuk debug USB | Setelah laporan gagal ulang, HP masih menunjukkan APK yang diperbarui pukul 19.06 WIB. Konfigurasi debug khusus mesin ini memakai `rekap-debug.local.properties` dengan endpoint USB `http://127.0.0.1:8080`. Argumen `-PrekapApiBaseUrl` tetap diprioritaskan. | ADB shell dari HP menerima HTTP 200 dari `/health` dan HTTP 401 yang diharapkan dari `/v1/auth/login` dengan kredensial sintetis yang salah. Kode konfigurasi endpoint dikompilasi terpisah terhadap API Gradle 9.4.1 dan AGP 9.2.1 tanpa menjalankan task Gradle. | — | Konfigurasi lokal siap. Gradle Sync, Run, verifikasi endpoint APK baru, serta uji daftar dan masuk dari aplikasi masih menunggu build pengguna. |
| 23 Nov 2026 | 3 | Struktur dan performa Compose | Kelompokkan UI per fitur, pisahkan navigasi dan tema dari `MainActivity`, dan pertahankan satu modul Android. Rekap dan snapshot memakai `LazyColumn` untuk baris transaksi. Repository memetakan daftar dan rekap di `Dispatchers.Default` tanpa daftar JSON perantara. | Parser Kotlin lulus untuk 16 sumber baseline dan 21 sumber terbaru. Pemeriksaan 96 impor proyek dan 194 referensi resource lulus. `go test -buildvcs=false -count=1 ./...` lulus. APK debug sebelum refaktor berukuran 27.389.403 byte, termasuk sekitar 10,6 MiB library native OCR arm64. ADB tidak mendeteksi perangkat saat pemeriksaan ini. | — | Pemeriksaan sumber lokal selesai. Build pengguna, pengujian navigasi dan snapshot, serta pengukuran scroll dan memori pada perangkat masih terbuka. |
| 23 Nov 2026 | 3 | Debug autentikasi APK lewat USB | Pulihkan ADB reverse port 8080 dan perbarui APK terpasang dengan APK debug hasil build pengguna pukul 19.40 WIB. APK `0.1.0`, SHA-256 `5e9184adeeb464d273a3bd75d01040a7b702938539f67320facb341c85f6cd4f`, terpasang pada HP `24069PC21G` dengan Android 16 (API 36). APK memuat endpoint USB `http://127.0.0.1:8080`. | SHA-256 APK di HP cocok dengan artefak build. Activity dibuka melalui ADB dan Beranda dengan sesi aktif teramati. Pada API demo/memori lewat ADB shell, `/health` 200, daftar 201, login 200, kredensial salah 401, profil `UMKM_USER` 200, refresh 200, logout 204, dan refresh token lama atau yang dicabut ditolak dengan 401. Uji API memakai akun sintetis. | — | APK terbaru berjalan dan Beranda teramati. Transisi daftar dan masuk masing-masing dari antarmuka belum diuji ulang secara terpisah. |
| 23 Nov 2026 | 3 | Adaptasi UI Compose dari PRD | Telaah 22 referensi Figma L02–L23. Terapkan palet, tipografi, autentikasi, navigasi empat tab, Beranda dengan rekap bulanan aktual, serta tampilan bukti, draf, transaksi, rekap, snapshot, kategori, pelanggan, dan filter tersimpan untuk fitur yang tersedia. Bukti memberi disclosure sesuai mode OCR aktual. | Pemeriksaan sintaks 21 sumber Kotlin, 115 impor proyek, 247 referensi resource, dan `git diff --check` lulus. `go test -buildvcs=false -count=1 ./...` lulus. URL aset sementara Figma gagal diakses dari workspace. Implementasi memakai kontrol native dan font sistem. | — | Implementasi lokal. Build dan pemeriksaan visual, TalkBack, keyboard, serta perpindahan tab pada APK desain baru masih terbuka. |
| 23 Nov 2026 | 3 | Run UI baru dan pemulihan API USB | APK debug `0.1.0` pukul 20.43 WIB berukuran 27.423.011 byte dan SHA-256 `3d7afea9ac3db2e37ac8bf8d243c7bbfff9b50218ef590da06c1e5d811c71351`. APK yang sama terpasang di HP `24069PC21G` Android 16 (API 36). DEX memuat UI baru dan `http://127.0.0.1:8080`. Pulihkan pemetaan ADB reverse port 8080 setelah koneksi USB berganti. | Backend PC aktif dalam mode demo/memori. HP mendapat HTTP 200 dari `/health` setelah pemetaan dipulihkan. Formulir Daftar dan kesalahan koneksi lama terlihat di perangkat. Beranda dengan sesi aktif teramati setelah koneksi pulih. Percobaan Gradle Wrapper dari terminal berhenti karena unduhan distribusi Gradle ditolak oleh sandbox. | — | Build UI baru terpasang dan pemeriksaan awal koneksi serta akses Beranda lulus. Uji lengkap fitur, keyboard dan TalkBack masih terbuka. |

## Adaptasi UI sesuai PRD desain v1.0 (24 November 2026)

Pekerjaan minggu 23–29 November 2026 menyelaraskan tampilan Compose dengan token dan alur layar pada PRD desain RekapTF v1.0. Bukti di bawah adalah pemeriksaan build lokal tanpa APK: `:app:compileDebugKotlin` dan `:app:processDebugResources` lulus pada Gradle 9.4.1 mode `--offline`, serta `go test -buildvcs=false -count=1 ./...` lulus. Uji perangkat, TalkBack, dan tangkapan layar belum dijalankan.

Catatan: hash pada tabel di bawah berasal dari riwayat `main` lokal lama yang tidak dipublikasikan. Padanan yang dipublikasikan: `ddba0e6` (token dan komponen), `d5e26f3` (Beranda), `43b4258` (kerangka bukti, rekap, dan draf).

| Tanggal | Sprint | Fitur | Perubahan | Bukti uji | Hash commit | Status |
|---|---|---|---|---|---|---|
| 24 Nov 2026 | 1 | Design system | Token warna, jarak, bentuk, dan ukuran sesuai PRD 5.1–5.4. Tema dipaksa terang agar identitas desain tidak berubah oleh tema sistem. Komponen dasar: bilah layar, panel info, badge status, kartu hero, tile komposisi, pemilih chip, baris metadata, dan tahap pemrosesan. | `:app:compileDebugKotlin` dan `:app:processDebugResources` lulus. Aset Inter belum ada sehingga tipografi memakai fallback sistem dan dicatat sebagai batas. | `11db578` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Beranda L04 | Kartu hero total bulan aktif, komposisi dua kategori teratas, panel draf yang belum masuk rekap, dan pemasukan terbaru dengan tautan Semua. Nama pribadi tidak ditebak dari nama usaha. | Data berasal dari `/v1/recap` dan `/v1/categories`; kompilasi lulus. Angka dan navigasi belum diperiksa pada APK. | `d992626` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Bukti L06, L07, L23 | Bingkai pratinjau, nama berkas, badge satu gambar, disclosure AI sebelum proses, tiga tahap pemrosesan tanpa persen palsu, serta layar teks tidak terbaca dengan pilih ulang. | Kompilasi lulus. OCR ML Kit, DeepSeek, dan perilaku offline belum diuji pada perangkat. | `4bdb040` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Draf dan transaksi L08–L11 | Daftar bertanggal dengan ringkasan total dan jumlah hasil filter, saringan Semua dan Perlu diperiksa, baris 68/72 dp dengan badge, detail bermetadata, formulir bersection, dan peringatan perubahan belum disimpan. | Kompilasi dan penggabungan resource lulus. Konfirmasi idempoten serta batas periode belum diuji ulang lewat APK. | `6947004` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Rekap, snapshot, filter, direktori L13–L21 | Hero rekap, chip periode, penjelasan snapshot hanya baca, daftar filter tersimpan dengan target Buka dan Ubah terpisah, serta daftar kategori/pelanggan dengan jumlah data. | Kompilasi lulus. Simpan dan buka snapshot dari APK serta hitung ulang filter masih terbuka. | `c992d9b`, `e14e719` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Akun dan navigasi L02, L03, L15 | Jarak autentikasi 22 dp, bilah RekapTF, CTA penuh dengan panah, panel Data & akun tanpa peran tambahan, dan navigasi bawah dengan penanda aktif tanpa indikator kapsul. | Kompilasi lulus. Uji TalkBack, keyboard, dan rotasi perangkat belum dijalankan. | `a1fedbd`, `57264ea` | Commit pengumpulan lokal |
| 24 Nov 2026 | 1 | Aksesibilitas tombol | Panah dekoratif pada CTA tidak lagi dibacakan TalkBack; makna tindakan tetap dibawa label tombol. | Kompilasi dan penggabungan resource lulus. Uji TalkBack pada perangkat belum dijalankan. | `a1fc796`, `a38688e` | Commit pengumpulan lokal |

Batas yang masih terbuka: aset Inter belum tersedia; field dinamis kategori dan pelanggan belum didukung backend sehingga editor hanya menyimpan nama dan catatan; total per kategori/pelanggan belum dihitung; hapus transaksi masih berupa dialog, bukan layar L22 tersendiri; tautan bukti asli pada detail belum tersedia karena referensi gambar hanya disimpan selama proses.

### Pengumpulan Sprint 1 ke GitHub

Isi `rekap-utama` disalin massal dengan `tar` ke `rekap-repo`, lalu dipotong menjadi berkas per fitur. Riwayat publikasi disusun sebagai branch orphan lokal `sprint-1` dari kondisi kerja akhir dan dipush ke `refs/heads/main` pada `https://github.com/Kysohee18/0046_RekapTF.git`. Remote tadinya kosong, jadi push pertama tidak menimpa commit mana pun. `main` lokal lama (25 commit, tip `a24ea56`) tidak dipublikasikan karena memuat irisan Sprint 2 dan 3.

| Hash | Isi |
|---|---|
| `8933723` | Scaffold Compose, katalog versi, Gradle Wrapper, diagram aktivitas |
| `1211596` | Konfigurasi endpoint debug emulator dan USB |
| `16e2e91` | Sesi JWT `UMKM_USER`, profil, penyimpanan |
| `cab0366` | CRUDS kategori dan pengirim |
| `1d00045` | Uji isolasi akun, CRUDS lintas akun, nama unik |
| `05c8870` | Klien REST dan repository Android |
| `ddba0e6` | Token desain, tipografi, komponen, string |
| `9d95268` | Layar Masuk dan Daftar UMKM |
| `f3cfc94` | Navigasi empat tab, pemulihan state, `MainActivity` |
| `9f613ab` | Daftar, detail, editor transaksi |
| `368b44c` | CRUDS kategori dan pelanggan |
| `d5e26f3` | Beranda ringkasan bulanan |
| `ae4a1d9` | Panduan agent, aturan, README, log |
| `2538f13` | Skrip pemeriksaan API lokal dan ADB reverse |
| `43b4258` | Kerangka OCR, bukti, dan rekap untuk sprint berikutnya |

Jumlah commit yang benar-benar ada di GitHub saat pengumpulan pertama: 15, tip `43b42582380ef11345ecbdd53dfa698d8ff340d9`. Kecocokan hash diverifikasi dengan `git ls-remote origin refs/heads/main`. Berkas gambar `docs/fixtures/**` milik Sprint 2 dan riwayat `main` lokal lama tidak disertakan.

**Batas riwayat.** Commit dalam riwayat publikasi adalah potongan berkas, bukan build bertahap: `AndroidManifest.xml` sudah menyebut `.MainActivity` pada `8933723` sedangkan kelasnya masuk pada `f3cfc94`, sehingga hanya tip yang berdiri sendiri. Kompilasi yang tercatat di log ini dijalankan pada kondisi kerja akhir di `rekap-utama`, bukan per commit.

## Pekerjaan aktif dan tindakan berikutnya

- **APK pra-produksi.** Konfigurasi aktif memakai Kotlin 2.2.10, Gradle 9.4.1, AGP 9.2.1, Compose BOM 2026.09.00, `minSdk 24`, `targetSdk 36`, dan `compileSdk 37`. Tampilan terbaru sudah diselaraskan dengan PRD desain v1.0 dan lulus kompilasi, tetapi APK-nya belum dibuat. Lanjutkan dengan build pengguna, lalu uji layar L02–L23 memakai akun sintetis, termasuk TalkBack, keyboard, dan rotasi.
- **PostgreSQL lokal.** Jalankan skenario backend yang sama pada database uji terpisah saat DSN uji tersedia. Bukti saat ini berasal dari penyimpanan memori dalam mode demo.
- **Transaksi dan snapshot.** Uji konfirmasi ulang, perlindungan referensi transaksi, rekap tanggal batas, dan simpan serta buka snapshot dari APK aktif. Pencatatan manual menunggu keputusan atas usulan SRS.
- **OCR dan staging.** Backend Go 1.26.0 mendukung demo dan konfigurasi penyedia eksternal. Ukur ML Kit Latin *bundled* pada sampel sintetis di HP kelas bawah. Setelah uji lokal, validasi APK melalui HTTPS dengan Neon staging dan DeepSeek. Pilih Baidu atau ML Kit untuk rilis berdasarkan hasil perangkat dan layanan aktual.
- **PAM dan pengumpulan.** Konfirmasikan lima kelompok CRUDS dan arti *dynamic fields* dengan dosen serta konfirmasikan pemilik tindakan publikasi mingguan. Untuk pemeriksaan repo, gunakan peta lokasi di [panduan agent](agent.md#lokasi-kerja).
