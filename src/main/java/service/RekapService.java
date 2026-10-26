package service;

import database.DatabaseManager;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Perhitungan rekap & persentase kehadiran untuk laporan. */
public class RekapService {

    /** Satu baris rekap kehadiran per mahasiswa per mata kuliah. */
    public record RekapRow(String nim, String nama, String matkul, int totalSesi,
                           int hadir, int terlambat, int tidakHadir, double persen) {}

    /** Baris rekap ringkas per sesi (pertemuan). */
    public record SesiRow(int sesiId, String matkul, int pertemuan, String tanggal,
                          int peserta, int hadir, int terlambat, double persenHadir) {}

    /** Rekap per mahasiswa pada satu mata kuliah (matkulId null = gabungan semua). */
    public List<RekapRow> rekapMahasiswa(Integer matkulId) throws SQLException {
        String sql = """
            SELECT m.nim, m.nama, mk.nama AS matkul,
                   COUNT(s.id) AS total_sesi,
                   SUM(CASE WHEN a.status='HADIR' THEN 1 ELSE 0 END) AS hadir,
                   SUM(CASE WHEN a.status='TERLAMBAT' THEN 1 ELSE 0 END) AS terlambat
            FROM peserta_mata_kuliah p
            JOIN mahasiswa m ON m.id=p.mahasiswa_id
            JOIN mata_kuliah mk ON mk.id=p.mata_kuliah_id
            LEFT JOIN sesi_perkuliahan s ON s.mata_kuliah_id=mk.id AND s.status='SELESAI'
            LEFT JOIN absensi a ON a.sesi_id=s.id AND a.mahasiswa_id=m.id
            WHERE (? IS NULL OR p.mata_kuliah_id=?)
            GROUP BY m.id, mk.id
            HAVING COUNT(s.id) > 0
            ORDER BY mk.nama, m.nim""";
        List<RekapRow> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, matkulId);
            ps.setObject(2, matkulId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_sesi");
                    int h = rs.getInt("hadir");
                    int t = rs.getInt("terlambat");
                    // hadir+terlambat dihitung sebagai hadir karena baris absensi ada
                    boolean tercatat = false;
                    // jumlah baris absensi = hadir + terlambat
                    int tercatatCount = h + t;
                    double persen = total == 0 ? 0 : Math.round(tercatatCount * 1000.0 / total) / 10.0;
                    out.add(new RekapRow(rs.getString("nim"), rs.getString("nama"),
                            rs.getString("matkul"), total, h, t, total - tercatatCount, persen));
                }
            }
        }
        return out;
    }

    /** Rekap khusus satu mahasiswa (untuk dashboard mahasiswa). */
    public List<RekapRow> rekapUntukMahasiswa(int mahasiswaId) throws SQLException {
        String sql = """
            SELECT m.nim, m.nama, mk.nama AS matkul,
                   COUNT(s.id) AS total_sesi,
                   SUM(CASE WHEN a.status='HADIR' THEN 1 ELSE 0 END) AS hadir,
                   SUM(CASE WHEN a.status='TERLAMBAT' THEN 1 ELSE 0 END) AS terlambat
            FROM peserta_mata_kuliah p
            JOIN mahasiswa m ON m.id=p.mahasiswa_id
            JOIN mata_kuliah mk ON mk.id=p.mata_kuliah_id
            LEFT JOIN sesi_perkuliahan s ON s.mata_kuliah_id=mk.id AND s.status='SELESAI'
            LEFT JOIN absensi a ON a.sesi_id=s.id AND a.mahasiswa_id=m.id
            WHERE p.mahasiswa_id=?
            GROUP BY m.id, mk.id
            HAVING COUNT(s.id) > 0
            ORDER BY mk.nama""";
        List<RekapRow> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, mahasiswaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int total = rs.getInt("total_sesi");
                    int h = rs.getInt("hadir");
                    int t = rs.getInt("terlambat");
                    double persen = total == 0 ? 0 : Math.round((h + t) * 1000.0 / total) / 10.0;
                    out.add(new RekapRow(rs.getString("nim"), rs.getString("nama"),
                            rs.getString("matkul"), total, h, t, total - h - t, persen));
                }
            }
        }
        return out;
    }

    /** Rekap per sesi/pertemuan: berapa hadir dari total peserta. */
    public List<SesiRow> rekapPerSesi(Integer matkulId, String tglAwal, String tglAkhir) throws SQLException {
        StringBuilder sb = new StringBuilder("""
            SELECT s.id, mk.nama AS matkul, s.pertemuan_ke, s.tanggal,
                   (SELECT COUNT(*) FROM peserta_mata_kuliah p WHERE p.mata_kuliah_id=s.mata_kuliah_id) AS peserta,
                   (SELECT COUNT(*) FROM absensi a WHERE a.sesi_id=s.id AND a.status='HADIR') AS hadir,
                   (SELECT COUNT(*) FROM absensi a WHERE a.sesi_id=s.id AND a.status='TERLAMBAT') AS terlambat
            FROM sesi_perkuliahan s JOIN mata_kuliah mk ON mk.id=s.mata_kuliah_id
            WHERE s.status <> 'TERJADWAL'""");
        List<Object> params = new ArrayList<>();
        if (matkulId != null && matkulId > 0) { sb.append(" AND s.mata_kuliah_id=?"); params.add(matkulId); }
        if (tglAwal != null && !tglAwal.isBlank()) { sb.append(" AND s.tanggal>=?"); params.add(tglAwal); }
        if (tglAkhir != null && !tglAkhir.isBlank()) { sb.append(" AND s.tanggal<=?"); params.add(tglAkhir); }
        sb.append(" ORDER BY s.tanggal DESC, s.pertemuan_ke DESC");
        List<SesiRow> out = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int peserta = rs.getInt("peserta");
                    int h = rs.getInt("hadir");
                    int t = rs.getInt("terlambat");
                    double persen = peserta == 0 ? 0 : Math.round((h + t) * 1000.0 / peserta) / 10.0;
                    out.add(new SesiRow(rs.getInt("id"), rs.getString("matkul"),
                            rs.getInt("pertemuan_ke"), rs.getString("tanggal"),
                            peserta, h, t, persen));
                }
            }
        }
        return out;
    }
}
