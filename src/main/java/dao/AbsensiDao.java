package dao;

import database.DatabaseManager;
import model.Absensi;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel absensi. */
public class AbsensiDao {

    private static final String SELECT_JOIN = """
        SELECT a.*, m.nim, m.nama AS nama_mhs, mk.kode AS kode_mk, mk.nama AS nama_mk,
               s.pertemuan_ke, s.tanggal
        FROM absensi a
        JOIN mahasiswa m ON m.id = a.mahasiswa_id
        JOIN sesi_perkuliahan s ON s.id = a.sesi_id
        JOIN mata_kuliah mk ON mk.id = s.mata_kuliah_id
        """;

    private Absensi map(ResultSet rs) throws SQLException {
        Absensi a = new Absensi();
        a.setId(rs.getInt("id"));
        a.setMahasiswaId(rs.getInt("mahasiswa_id"));
        a.setSesiId(rs.getInt("sesi_id"));
        a.setWaktuAbsen(rs.getString("waktu_absen"));
        a.setStatus(rs.getString("status"));
        a.setMetode(rs.getString("metode"));
        a.setNim(rs.getString("nim"));
        a.setNamaMahasiswa(rs.getString("nama_mhs"));
        a.setKodeMatkul(rs.getString("kode_mk"));
        a.setNamaMatkul(rs.getString("nama_mk"));
        a.setPertemuanKe(rs.getInt("pertemuan_ke"));
        a.setTanggal(rs.getString("tanggal"));
        return a;
    }

    public boolean sudahAbsen(int mahasiswaId, int sesiId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM absensi WHERE mahasiswa_id=? AND sesi_id=?")) {
            ps.setInt(1, mahasiswaId);
            ps.setInt(2, sesiId);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    /** Simpan kehadiran. Mengembalikan false bila duplikat (UNIQUE constraint). */
    public boolean catat(Absensi a) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO absensi(mahasiswa_id,sesi_id,waktu_absen,status,metode) VALUES(?,?,?,?,?)")) {
            ps.setInt(1, a.getMahasiswaId());
            ps.setInt(2, a.getSesiId());
            ps.setString(3, a.getWaktuAbsen());
            ps.setString(4, a.getStatus());
            ps.setString(5, a.getMetode());
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            if (isUniqueViolation(e)) return false;
            throw e;
        }
    }

    static boolean isUniqueViolation(SQLException e) {
        String msg = e.getMessage() == null ? "" : e.getMessage().toLowerCase();
        return msg.contains("unique") || msg.contains("constraint");
    }

    public List<Absensi> findBySesi(int sesiId) throws SQLException {
        List<Absensi> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE a.sesi_id=? ORDER BY m.nim")) {
            ps.setInt(1, sesiId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public List<Absensi> findByMahasiswa(int mahasiswaId, Integer matkulId) throws SQLException {
        List<Absensi> list = new ArrayList<>();
        String sql = SELECT_JOIN + " WHERE a.mahasiswa_id=?" + (matkulId != null && matkulId > 0 ? " AND s.mata_kuliah_id=?" : "") + " ORDER BY s.tanggal DESC, s.jam_mulai DESC";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, mahasiswaId);
            if (matkulId != null && matkulId > 0) ps.setInt(2, matkulId);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    /** Riwayat dengan filter: matkul, tanggal awal/akhir, nim/nama. */
    public List<Absensi> riwayat(Integer matkulId, String tglAwal, String tglAkhir, String keyword) throws SQLException {
        StringBuilder sb = new StringBuilder(SELECT_JOIN + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (matkulId != null && matkulId > 0) { sb.append(" AND s.mata_kuliah_id=?"); params.add(matkulId); }
        if (tglAwal != null && !tglAwal.isBlank()) { sb.append(" AND s.tanggal>=?"); params.add(tglAwal); }
        if (tglAkhir != null && !tglAkhir.isBlank()) { sb.append(" AND s.tanggal<=?"); params.add(tglAkhir); }
        if (keyword != null && !keyword.isBlank()) {
            sb.append(" AND (m.nim LIKE ? OR m.nama LIKE ?)");
            String k = "%" + keyword.trim() + "%";
            params.add(k); params.add(k);
        }
        sb.append(" ORDER BY s.tanggal DESC, s.pertemuan_ke DESC, m.nim");
        List<Absensi> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) list.add(map(rs)); }
        }
        return list;
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM absensi WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long countHariIni(String tanggal) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT COUNT(*) FROM absensi a JOIN sesi_perkuliahan s ON s.id=a.sesi_id WHERE s.tanggal=?")) {
            ps.setString(1, tanggal);
            try (ResultSet rs = ps.executeQuery()) { return rs.getLong(1); }
        }
    }

    public long total() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM absensi")) {
            return rs.getLong(1);
        }
    }
}
