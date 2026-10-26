package service;

import dao.*;
import model.Absensi;
import model.SesiPerkuliahan;
import model.TokenQr;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * AttendanceService — logika bisnis absensi & validasi QR dinamis.
 *
 * Rantai validasi saat pemindaian:
 *  1. Format payload dikenali (ABSV1|...)
 *  2. Tanda tangan HMAC-SHA256 cocok (QR tidak palsu / ID sesi tidak ditebak)
 *  3. Hash token terdaftar di tabel token_qr (token pernah diterbitkan aplikasi)
 *  4. Token belum lewat masa berlaku
 *  5. Status sesi masih AKTIF (sesi ditutup = semua token sesi gugur)
 *  6. Mahasiswa terdaftar pada mata kuliah sesi tersebut
 *  7. Mahasiswa belum pernah absen pada sesi yang sama (UNIQUE constraint)
 */
public class AttendanceService {

    private final SesiDao sesiDao = new SesiDao();
    private final TokenQrDao tokenDao = new TokenQrDao();
    private final AbsensiDao absensiDao = new AbsensiDao();
    private final PesertaDao pesertaDao = new PesertaDao();
    private final SettingDao settingDao = new SettingDao();

    public static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** Masa berlaku satu QR (detik), dapat diatur dari halaman Pengaturan. */
    public int getQrTtlSeconds() {
        try { return Integer.parseInt(settingDao.get("qr_ttl_seconds", "60")); }
        catch (NumberFormatException e) { return 60; }
    }

    public void setQrTtlSeconds(int v) throws SQLException {
        settingDao.set("qr_ttl_seconds", String.valueOf(v));
    }

    /** Membuat payload QR baru untuk sesi aktif. Hash-nya dicatat ke token_qr.
     *  Token lama milik sesi tetap disimpan sampai sesi ditutup, sehingga
     *  mahasiswa yang memindai dalam TTL masih bisa tercatat. */
    public String issueNewToken(int sesiId) throws SQLException {
        SesiPerkuliahan s = sesiDao.findById(sesiId);
        if (s == null) throw new SQLException("Sesi tidak ditemukan.");
        if (!"AKTIF".equals(s.getStatus())) throw new SQLException("Sesi tidak dalam status AKTIF.");
        int ttl = getQrTtlSeconds();
        String payload = QrTokenService.generatePayload(sesiId, ttl);
        TokenQr t = new TokenQr();
        t.setSesiId(sesiId);
        t.setTokenHash(QrTokenService.tokenHash(payload));
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now.format(TS));
        t.setExpiresAt(now.plusSeconds(ttl).format(TS));
        tokenDao.insert(t);
        return payload;
    }

    /** Validasi + pencatatan absensi dari payload hasil pemindaian. */
    public ScanResult scanAndRecord(String payload, int mahasiswaId) {
        try {
            // 1. Format
            QrTokenService.Parsed parsed = QrTokenService.parse(payload);
            if (!parsed.formatValid())
                return ScanResult.fail("QR Code tidak valid (format tidak dikenali).");

            // 2. Tanda tangan HMAC
            if (!QrTokenService.verifySignature(parsed))
                return ScanResult.fail("QR Code TIDAK VALID — tanda tangan tidak cocok (kemungkinan dipalsukan).");

            // 3. Token harus pernah diterbitkan oleh aplikasi ini
            String hash = QrTokenService.tokenHash(payload);
            TokenQr stored = tokenDao.findByHash(hash);
            if (stored == null)
                return ScanResult.fail("QR Code tidak dikenal oleh sistem (bukan token sesi aktif).");

            // 4. Kedaluwarsa
            if (QrTokenService.isExpired(parsed))
                return ScanResult.fail("QR Code KEDALUWARSA. Silakan minta mahasiswa memindai QR yang baru (rotasi tiap " + getQrTtlSeconds() + " detik).");

            // 5. Status sesi
            SesiPerkuliahan sesi = sesiDao.findById(stored.getSesiId());
            if (sesi == null || !"AKTIF".equals(sesi.getStatus()))
                return ScanResult.fail("Sesi absensi sudah DITUTUP. Kehadiran tidak dapat dicatat.");

            // 6. Kepesertaan mata kuliah
            if (!pesertaDao.isTerdaftar(mahasiswaId, sesi.getMataKuliahId()))
                return ScanResult.fail("MAHASISWA DITOLAK: Anda tidak terdaftar pada mata kuliah " + sesi.getNamaMatkul() + ".");

            // 7. Absensi ganda
            if (absensiDao.sudahAbsen(mahasiswaId, sesi.getId()))
                return ScanResult.fail("PERINGATAN: Anda sudah absen pada sesi ini. Absensi ganda ditolak.");

            // Simpan kehadiran; tentukan HADIR / TERLAMBAT (lewat 15 menit dari jam mulai)
            String status = "HADIR";
            try {
                LocalTime mulai = LocalTime.parse(sesi.getJamMulai());
                if (LocalTime.now().isAfter(mulai.plusMinutes(15))) status = "TERLAMBAT";
            } catch (Exception ignore) {}

            Absensi a = new Absensi();
            a.setMahasiswaId(mahasiswaId);
            a.setSesiId(sesi.getId());
            a.setWaktuAbsen(LocalDateTime.now().format(TS));
            a.setStatus(status);
            a.setMetode("QR");
            boolean ok = absensiDao.catat(a);
            if (!ok) return ScanResult.fail("PERINGATAN: data Anda baru saja tercatat. Absensi ganda ditolak.");

            String label = "Pertemuan ke-" + sesi.getPertemuanKe() + " " + sesi.getNamaMatkul() + " (" + sesi.getTanggal() + ")";
            ScanResult r = ScanResult.ok(null, null, label);
            return new ScanResult(true,
                    "Absensi BERHASIL dicatat sebagai " + status + ".", null, null, label);
        } catch (SQLException e) {
            return ScanResult.fail("Kesalahan database: " + e.getMessage());
        }
    }

    /** Hanya untuk pengujian otomatis (SelfTest): mendaftarkan payload QR ke tabel
     *  token_qr tanpa mengubah status sesi, misalnya untuk membuat token kedaluwarsa. */
    public void registerTokenForTest(int sesiId, String payload) throws SQLException {
        TokenQr t = new TokenQr();
        t.setSesiId(sesiId);
        t.setTokenHash(QrTokenService.tokenHash(payload));
        LocalDateTime now = LocalDateTime.now();
        t.setCreatedAt(now.format(TS));
        t.setExpiresAt(now.plusSeconds(getQrTtlSeconds()).format(TS));
        tokenDao.insert(t);
    }

    /** Menutup sesi: seluruh hash token sesi digugurkan agar tak bisa dipakai lagi. */
    public void closeSession(int sesiId) throws SQLException {
        sesiDao.updateStatus(sesiId, "SELESAI");
        tokenDao.deleteBySesi(sesiId);
    }

    /** Membuka sesi (mulai absensi): status menjadi AKTIF dan terbitkan token pertama. */
    public String startSession(int sesiId) throws SQLException {
        sesiDao.updateStatus(sesiId, "AKTIF");
        return issueNewToken(sesiId);
    }
}
