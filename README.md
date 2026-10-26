# Aplikasi Absensi Mahasiswa Berbasis Desktop — QR Code Dinamis

**Rancang Bangun Aplikasi Absensi Mahasiswa Berbasis Desktop Menggunakan QR Code Dinamis pada Setiap Pertemuan Perkuliahan**

Teknologi: Java 17 + JavaFX (GUI) + Maven + SQLite + ZXing, arsitektur MVC.

---

## 1. Analisis Kebutuhan

### Kebutuhan Fungsional
| No | Kebutuhan | Aktor |
|----|-----------|-------|
| F1 | Login username/password (password disimpan sebagai hash PBKDF2, bukan teks biasa) | Dosen, Mahasiswa |
| F2 | CRUD data mahasiswa, dosen, mata kuliah | Dosen/Admin |
| F3 | Pendaftaran peserta mata kuliah (FK penentu hak absen) | Dosen |
| F4 | Jadwal & sesi perkuliahan per pertemuan | Dosen |
| F5 | Penerbitan QR Code dinamis (token acak + HMAC-SHA256, rotasi tiap N detik) | Dosen |
| F6 | Pemindaian QR via webcam **atau** dari file gambar (mode uji) | Mahasiswa |
| F7 | Validasi berjenjang: format → HMAC → token terdaftar → kedaluwarsa → status sesi → kepesertaan → absensi ganda | Sistem |
| F8 | Lihat hadir/belum hadir, riwayat, rekap persentase kehadiran | Dosen, Mahasiswa |
| F9 | Ekspor laporan CSV & pencadangan (backup) database | Dosen |
| F10 | Pengaturan masa berlaku QR & auto-backup | Dosen |

### Kebutuhan Non-Fungsional
- Desktop Windows (JDK 17), jendela dapat di-resize (min 880x560).
- Responsif: tabel memakai `TableView` dengan banyak baris, antrian pencarian.
- Keamanan: kunci rahasia HMAC disimpan lokal di `data/secret.key` (permission 600) dan **tidak pernah** masuk ke dalam QR.
- Integritas data: foreign key ON DELETE CASCADE + UNIQUE(mahasiswa_id, sesi_id) mencegah absensi ganda di level database.

## 2. Flowchart Sistem (Absensi QR)

```
Dosen: Login → Buat Sesi (Pertemuan ke-N) → Buka Sesi (status AKTIF)
       → Sistem terbitkan TOKEN UNIK (random 16 byte, base64url)
       → payload = ABSV1|sesiId|token|expiryEpochSec|hmacSha256(payload+kunci)
       → QR dirender & ROTASI otomatis setiap TTL detik → tampil di layar dosen

Mahasiswa: Login → ScanView → webcam/file PNG → decode ZXing
       → validasi berjenjang (lihat F7):
          ├─ gagal format/tanda tangan  → "QR tidak valid/dipalsukan"
          ├─ token tak dikenal          → "bukan token sistem ini"
          ├─ lewat expiry               → "QR KEDALUWARSA"
          ├─ sesi SELESAI               → "Sesi sudah DITUTUP"
          ├─ bukan peserta MK           → "MAHASISWA DITOLAK"
          ├─ sudah absen sesi sama      → "PERINGATAN: absensi ganda ditolak"
          └─ valid → INSERT absensi (HADIR/TERLAMBAT) → pesan sukses
Dosen: tutup sesi → semua hash token sesi digugurkan (tak bisa dipakai lagi)
```

## 3. Use Case Diagram (tekstual)

```
        DOSEN/ADMIN                                MAHASISWA
 ┌──────────────────────────┐            ┌───────────────────────────┐
 │ Login                    │            │ Login                     │
 │ Kelola Data Mahasiswa ───┼──(akun)    │ Lihat Profil              │
 │ Kelola Data Dosen        │  dibuatkan │ Lihat Mata Kuliah saya    │
 │ Kelola Mata Kuliah+Peserta│  otomatis │ Lihat Jadwal              │
 │ Atur Jadwal/Sesi         │            │ Scan QR (kamera/file)     │
 │ Terbitkan & Rotasi QR    │◄──validasi─│ Status & Riwayat Kehadiran│
 │ Lihat Hadir/Belum Hadir  │            │ Persentase kehadiran/MK   │
 │ Riwayat & Rekap, Export  │            └───────────────────────────┘
 │ Pengaturan TTL, Backup DB│
 └──────────────────────────┘
```

## 4. ERD Database (SQLite)

```
users(id PK, username UK, password_hash, role[DOSEN|MAHASISWA],
      mahasiswa_id FK→mahasiswa, dosen_id FK→dosen, nama_lengkap)
dosen(id PK, nidn UK, nama, prodi, email)
mahasiswa(id PK, nim UK, nama, prodi, email, telepon, status)
mata_kuliah(id PK, kode UK, nama, sks, dosen_id FK→dosen)
peserta_mata_kuliah(id PK, mahasiswa_id FK, matkul_id FK, UNIQUE(mahasiswa_id,matkul_id))
sesi_perkuliahan(id PK, matkul_id FK, tanggal, pertemuan_ke, jam_mulai, jam_selesai,
                 topik, status[SEBELUMNYA|AKTIF|SELESAI], UNIQUE(matkul_id,pertemuan_ke))
token_qr(id PK, sesi_id FK, token_hash UK, created_at, expires_at)
absensi(id PK, mahasiswa_id FK, sesi_id FK, waktu_absen, status[HADIR|TERLAMBAT],
        metode[QR|MANUAL], UNIQUE(mahasiswa_id,sesi_id))  ← anti absensi ganda
settings(key PK, value)  ← qr_ttl_seconds, auto_backup, secret fallback
```
Semua FK memakai `ON DELETE CASCADE`; `PRAGMA foreign_keys = ON` diaktifkan pada koneksi.

## 5. Struktur Folder Project

```
src/main/java/
  app/         MainApp, Launcher (tanpa JavaFX-check), SelfTest (uji tanpa GUI)
  controller/  17 controller FXML (Login, Dashboard dosen/mhs, CRUD, Sesi,
               QR dinamis, Scan, Kehadiran, Riwayat, Rekap, Pengaturan)
  service/     AuthService, AttendanceService, QrTokenService (HMAC),
               QrImageUtil (render), QrDecoder (ZXing), SecretService,
               RekapService, BackupService, ScanResult
  dao/         UserDao, MahasiswaDao, DosenDao, MataKuliahDao, PesertaDao,
               SesiDao, AbsensiDao, TokenQrDao, SettingDao
  model/       User, Mahasiswa, Dosen, MataKuliah, PesertaMataKuliah,
               SesiPerkuliahan, Absensi, TokenQr, AppSetting
  database/    DatabaseManager (koneksi+skema), SeedData (data uji)
  util/        PasswordUtil (PBKDF2-HMAC-SHA256), Session (login aktif),
               CsvExporter, ValidationUtil
src/main/resources/
  fxml/  css/style.css  images/
pom.xml  data/absensi.db (dibuat otomatis)  data/secret.key
```

## 6–8. Konfigurasi Maven & Database
- `pom.xml`: dependensi `org.openjfx:javafx-*:17`, `org.xerial:sqlite-jdbc`, `com.google.zxing:core+javase`; plugin `javafx-maven-plugin` (jalankan) dan `jaxpypi jpackage-maven-plugin` (paket EXE/MSI).
- Database: `jdbc:sqlite:data/absensi.db`, dibuat + dischema + di-seed otomatis saat aplikasi pertama kali dijalankan. Override lokasi: `-Dabsensi.db=path`.

## 9. Panduan Menjalankan (Windows)

Prasyarat: JDK 17 (mis. Adoptium/Temurin) dan Maven.

```bat
:: dari folder project
mvn clean javafx:run
```
Alternatif tanpa plugin JavaFX (classpath biasa):
```bat
mvn compile dependency:copy-dependencies -DoutputDirectory=target/libs
java -cp "target/classes;target/libs/*" app.Launcher
```

## 10. Akun Pengujian (dibuat otomatis oleh SeedData, password: `123456`)
| Peran | Username | Nama |
|-------|----------|------|
| Dosen | `198001012005011001` | Dr. Ahmad Fauzi, M.Kom. |
| Mahasiswa | `211101001` | Budi Santoso (3 MK) |
| Mahasiswa | `211101005` | Eko Prabowo (hanya Basis Data → untuk uji penolakan) |

Setiap mahasiswa/dosen baru yang dibuat via aplikasi otomatis mendapat akun (username = NIM/NIDN, password default `123456`).

## 11. Panduan Fitur Singkat
1. **Login** → dashboard sesuai peran (sidebar kiri, header, kartu ringkasan).
2. **Data Mahasiswa/Dosen/MK**: isi form → Simpan; Cari dengan kata kunci; Ubah/Hapus dengan konfirmasi; Ekspor CSV.
3. **MK → Peserta**: pilih MK, centang mahasiswa agar berhak absen.
4. **Jadwal/Sesi**: buat sesi (tanggal, pertemuan ke-N). Tombol **Mulai Absensi** → status AKTIF + QR pertama terbit.
5. **QR Dinamis**: tampil besar + hitung mundur; tombol *Rotasi manual* & *Tutup Sesi*.
6. **Scan (mahasiswa)**: kamera web otomatis memindai; jika tidak ada kamera gunakan *Muat dari File PNG* (mode uji — bukan bukti keamanan di penggunaan nyata).
7. **Kehadiran**: daftar hadir vs belum hadir per sesi.
8. **Riwayat**: filter matkul/tanggal/pertemuan + ekspor CSV.
9. **Rekap**: persentase kehadiran per mahasiswa per MK.
10. **Pengaturan**: ubah TTL QR (detik), backup DB sekarang/otomatis saat keluar.

## 12. Penjelasan Kode (ringkas)
- `QrTokenService`: inti keamanan QR. Payload `ABSV1|sesiId|tokenAcak|expiry|hmac`. Kunci rahasia (`SecretService`, file `data/secret.key`) **tidak ikut ke dalam QR**; scanner mengirim seluruh payload, server-tepercaya (aplikasi) menghitung ulang HMAC dan mencocokkan — sehingga ID sesi yang ditebak atau QR hasil rekayasa foto lama ditolak.
- `AttendanceService.scanAndRecord`: rantai validasi 7 langkah (lihat flowchart) lalu insert `absensi`; error UNIQUE ditangkap sebagai penolakan absensi ganda.
- `SesiController`: `Timeline` JavaFX merotasi QR tiap TTL detik selama sesi AKTIF.
- DAO: satu kelas per tabel, semua SQL parameterized (anti SQL injection); `DatabaseManager` membuka koneksi SQLite + PRAGMA foreign_keys.
- Controller hanya mengatur tampilan; logika bisnis di service; akses data di dao (pemisahan MVC).

## 13. Membangun JAR / Installer Desktop

```bat
:: JAR executable (perlu JavaFX di classpath libs/)
mvn clean package
java -cp "target/absensi-mahasiswa-desktop-1.0.0.jar;target/libs/*" app.Launcher

:: Installer Windows .exe/.msi (butuh JDK 17 + WiX Toolset utk msi)
mvn jpackage:jpackage
:: hasil di folder target/dist/AbsensiMahasiswa (APP_IMAGE)
```

## Hasil Pengujian Otomatis (sudah benar-benar dijalankan)

Perintah: `java -cp "target/classes;target/libs/*" app.Launcher --selftest`
(database uji terpisah `data/selftest.db`)

```
[PASS] 1. Login akun benar (dosen)  -> role=DOSEN
[PASS] 2. Login password salah ditolak
[PASS] 3. Menambah data mahasiswa + akun
[PASS] 3b. Batasan UNIQUE NIM menolak duplikasi
[PASS] 4. Mata kuliah tersedia & peserta didaftarkan
[PASS] 5. Membuat sesi perkuliahan baru
[PASS] 6. Mulai sesi -> QR token terbit (HMAC valid)
[PASS] 6b. QR PNG terbaca kembali identik dengan payload (ZXing render+decode)
[PASS] 6c. Rotasi QR menghasilkan token berbeda
[PASS] 7. Memindai QR Code valid -> absen tercatat HADIR
[PASS] 8. QR kedaluwarsa ditolak
[PASS] 9. QR tidak valid/palsu ditolak (tanda tangan HMAC tidak cocok)
[PASS] 10. Absensi ganda pada sesi sama ditolak
[PASS] 11. Mahasiswa tak terdaftar pada MK ditolak
[PASS] 12. Rekap kehadiran terbentuk + persentase
[PASS] 13. Data tetap tersimpan setelah DB dibuka ulang
[PASS] 13b. Setelah sesi ditutup, QR sesi gugur
================ HASIL UJI ================
Lulus: 17 | Gagal: 0
```

GUI (JavaFX) memerlukan layar; pengujian GUI dilakukan manual di Windows dengan `mvn clean javafx:run`.

## Keterbatasan Arsitektur Lokal (Penting)
Aplikasi desktop lokal **tanpa server terpusat** menyimpan database di satu komputer.
Konsekuensi:
1. Validasi bersama (shared validation) antar perangkat mahasiswa tidak terjadi real-time;
   scanning pada PC yang sama dengan PC dosen (mis. laptop berpindah/beramai-ramai) adalah
   keterbatasan model ini. Untuk produksi nyata, pindahkan validasi token ke server/API pusat.
2. Mode "scan dari file gambar" disediakan untuk pengujian/komputer tanpa kamera; mode ini
   **tidak boleh dianggap bukti keamanan** pada penggunaan nyata karena gambar QR dapat
   diteruskan antar mahasiswa selama masih dalam window TTL. Mitigasi: TTL pendek (30–60 dtk),
   rotasi otomatis, dan pengawasan dosen.
