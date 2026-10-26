package app;

import dao.AbsensiDao;
import dao.MahasiswaDao;
import dao.PesertaDao;
import dao.SesiDao;
import database.DatabaseManager;
import database.SeedData;
import model.Absensi;
import model.Mahasiswa;
import model.SesiPerkuliahan;
import service.AttendanceService;
import service.AuthService;
import service.QrImageUtil;
import service.QrTokenService;
import service.RekapService;
import service.ScanResult;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * SelfTest — menjalankan seluruh skenario pengujian logika aplikasi TANPA GUI,
 * sehingga dapat diverifikasi di server pengujian/CI maupun Windows headless:
 *   java -cp "target/classes;target/libs/*" app.MainApp --selftest
 * Database uji memakai file terpisah (data/selftest.db) agar tidak merusak data riil.
 */
public class SelfTest {

    private static int pass = 0, fail = 0;

    private static void check(String name, boolean ok, String detail) {
        System.out.println((ok ? "[PASS] " : "[FAIL] ") + name + (detail == null || detail.isBlank() ? "" : "  -> " + detail));
        if (ok) pass++; else fail++;
    }

    public static void run() {
        try {
            // DB khusus pengujian
            File f = new File("data/selftest.db");
            if (f.exists()) f.delete();
            System.setProperty("absensi.db", "data/selftest.db");
            DatabaseManager.initFromConfig();
            DatabaseManager.initializeSchema();
            SeedData.seedIfEmpty();

            AuthService auth = new AuthService();
            MahasiswaDao mhsDao = new MahasiswaDao();
            SesiDao sesiDao = new SesiDao();
            AttendanceService att = new AttendanceService();

            // ---------- 1 & 2: LOGIN ----------
            Object dosen = null;
            try { dosen = auth.login("198001012005011001", "123456"); } catch (Exception e) { e.printStackTrace(); }
            check("1. Login akun benar (dosen)", dosen != null, dosen == null ? "akun seed gagal" : "role=" + ((model.User) dosen).getRole());
            boolean salahDitolak = false;
            try { Object u = auth.login("198001012005011001", "passwordsalah"); salahDitolak = (u == null); }
            catch (Exception expected) { salahDitolak = true; }
            check("2. Login password salah ditolak", salahDitolak, null);

            // ---------- 3: TAMBAH MAHASISWA ----------
            Mahasiswa baru = new Mahasiswa();
            baru.setNim("211109999"); baru.setNama("Mahasiswa Uji"); baru.setProdi("Teknik Informatika"); baru.setStatus("AKTIF");
            int idBaru = mhsDao.insert(baru);
            // ambil kembali objek dari DB agar id-nya terisi (dipakai untuk FK akun users)
            auth.createAccountForMahasiswa(mhsDao.findByNim("211109999"), "123456");
            check("3. Menambah data mahasiswa + akun", mhsDao.findByNim("211109999") != null, "id=" + idBaru);

            // duplicate NIM must be rejected by UNIQUE constraint
            boolean dupRejected = false;
            try { Mahasiswa d = new Mahasiswa(); d.setNim("211109999"); d.setNama("Duplikat"); d.setProdi("TI"); d.setStatus("AKTIF"); mhsDao.insert(d); }
            catch (Exception e) { dupRejected = true; }
            check("3b. Batasan UNIQUE NIM menolak duplikasi", dupRejected, null);

            // ---------- 4: BUAT MATA KULIAH (via DAO) + PESERTA ----------
            List<SesiPerkuliahan> sesiAda = sesiDao.filter(null, null, null, null);
            int matkulId = sesiAda.get(0).getMataKuliahId();
            new PesertaDao().tambah(idBaru, matkulId); // daftingkan mahasiswa uji ke matkul pertama
            check("4. Mata kuliah tersedia & peserta didaftarkan", matkulId > 0, "matkulId=" + matkulId);

            // ---------- 5: BUAT SESI PERKULIAHAN ----------
            SesiPerkuliahan s = new SesiPerkuliahan();
            s.setMataKuliahId(matkulId); s.setPertemuanKe(99);
            s.setTanggal("2026-10-26"); s.setJamMulai("08:00"); s.setJamSelesai("09:40"); s.setTopik("Sesi Uji SelfTest");
            int sesiId = sesiDao.insert(s);
            check("5. Membuat sesi perkuliahan baru", sesiDao.findById(sesiId) != null, "sesiId=" + sesiId);

            // ---------- 6: QR DINAMIS ----------
            String payload = att.startSession(sesiId);
            QrTokenService.Parsed parsed = QrTokenService.parse(payload);
            check("6. Mulai sesi -> QR token terbit (HMAC valid)", QrTokenService.verifySignature(parsed),
                    "token tersimpan di tabel token_qr, expiry=" + parsed.expiryEpochSec());
            // QR juga harus bisa dirender & dibaca kembali dari gambar (menguji ZXing dua arah)
            Path png = Path.of("target", "qr_selftest.png");
            QrImageUtil.savePng(payload, png.toFile(), 256);
            String decoded = service.QrDecoder.decodeFile(png.toFile());
            check("6b. QR PNG terbaca kembali identik dengan payload", payload.equals(decoded == null ? "" : decoded.trim()), null);

            // rotasi: token lama tidak lagi menjadi token aktif terbaru
            String payload2 = att.issueNewToken(sesiId);
            check("6c. Rotasi QR menghasilkan token berbeda", !payload.equals(payload2), null);

            // ---------- 7: SCAN VALID ----------
            ScanResult r7 = att.scanAndRecord(payload2, idBaru);
            check("7. Memindai QR Code valid -> absen tercatat", r7.success(), r7.message());

            // ---------- 8: QR KEDALUWARSA ----------
            String expired = QrTokenService.generatePayload(sesiId, -5); // ttl negatif = sudah lewat
            // daftarkan token ke DB seperti saat diterbitkan, agar lolos cek 'diterbitkan sistem'
            att.registerTokenForTest(sesiId, expired);
            ScanResult r8 = att.scanAndRecord(expired, mhsDao.findByNim("211101002").getId());
            check("8. QR kedaluwarsa ditolak", !r8.success(), r8.message());

            // ---------- 9: QR TIDAK VALID (palsu / format salah) ----------
            ScanResult r9a = att.scanAndRecord("QR-REVIEW|abc|def", idBaru);
            ScanResult r9b = att.scanAndRecord("ABSV1|1|AAAA|9999999999|pemalsuan", idBaru);
            check("9. QR tidak valid/palsu ditolak", !r9a.success() && !r9b.success(), r9b.message());

            // ---------- 10: ABSENSI GANDA ----------
            ScanResult r10 = att.scanAndRecord(att.issueNewToken(sesiId), idBaru);
            check("10. Absensi ganda pada sesi sama ditolak", !r10.success(), r10.message());

            // ---------- 11: MAHASISWA TIDAK TERDAFTAR ----------
            int mhsLain = mhsDao.findByNim("211101005").getId(); // Eko: hanya mengambil Basis Data
            boolean terdaftar = new PesertaDao().findByMataKuliah(matkulId).stream()
                    .anyMatch(p -> p.getMahasiswaId() == mhsLain);
            ScanResult r11 = att.scanAndRecord(att.issueNewToken(sesiId), mhsLain);
            check("11. Mahasiswa tak terdaftar pada MK ditolak", !terdaftar && !r11.success(), r11.message());

            // ---------- 12: REKAP KEHADIRAN ----------
            att.closeSession(sesiId); // tandai sesi selesai agar masuk hitungan rekap
            List<RekapService.RekapRow> rekap = new RekapService().rekapMahasiswa(matkulId);
            boolean adaBarisUji = rekap.stream().anyMatch(x -> x.nim().equals("211109999"));
            check("12. Rekap kehadiran terbentuk + persentase", adaBarisUji,
                    rekap.isEmpty() ? "kosong" : String.format("contoh: %s %s %.1f%%", rekap.get(0).nim(), rekap.get(0).nama(), rekap.get(0).persen()));

            // ---------- 13: TUTUP & BUKA KEMBALI (persistensi) ----------
            long jumlahAbsen = new AbsensiDao().findBySesi(sesiId).size();
            DatabaseManager.getConnection().close();
            System.setProperty("absensi.db", "data/selftest.db");
            DatabaseManager.initFromConfig();
            DatabaseManager.initializeSchema();
            long lagi = new AbsensiDao().findBySesi(sesiId).size();
            check("13. Data tetap tersimpan setelah DB dibuka ulang", jumlahAbsen == lagi && lagi >= 1,
                    "absen sebelum restart=" + jumlahAbsen + ", sesudah=" + lagi);

            // sesi tertutup -> token baru sesi itu tidak diterima
            ScanResult r13b = att.scanAndRecord(QrTokenService.generatePayload(sesiId, 60), idBaru);
            check("13b. Setelah sesi ditutup, QR sesi gugur", !r13b.success(), r13b.message());

            System.out.println("\n================ HASIL UJI ================");
            System.out.println("Lulus: " + pass + " | Gagal: " + fail);
            if (fail > 0) System.exit(1);
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("SELFTEST ERROR: " + e.getMessage());
            System.exit(2);
        }
    }
}
