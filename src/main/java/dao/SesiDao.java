package dao;

import database.DatabaseManager;
import model.SesiPerkuliahan;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel sesi_perkuliahan. */
public class SesiDao {

    private static final String SELECT_JOIN = """
        SELECT s.*, mk.kode AS kode_mk, mk.nama AS nama_mk, d.nama AS nama_dosen
        FROM sesi_perkuliahan s
        JOIN mata_kuliah mk ON mk.id = s.mata_kuliah_id
        JOIN dosen d ON d.id = mk.dosen_id
        """;

    private SesiPerkuliahan map(ResultSet rs) throws SQLException {
        SesiPerkuliahan s = new SesiPerkuliahan();
        s.setId(rs.getInt("id"));
        s.setMataKuliahId(rs.getInt("mata_kuliah_id"));
        s.setPertemuanKe(rs.getInt("pertemuan_ke"));
        s.setTanggal(rs.getString("tanggal"));
        s.setJamMulai(rs.getString("jam_mulai"));
        s.setJamSelesai(rs.getString("jam_selesai"));
        s.setStatus(rs.getString("status"));
        s.setTopik(rs.getString("topik"));
        s.setKodeMatkul(rs.getString("kode_mk"));
        s.setNamaMatkul(rs.getString("nama_mk"));
        s.setNamaDosen(rs.getString("nama_dosen"));
        return s;
    }

    /** Filter gabungan: mata kuliah (0 = semua), rentang tanggal, status. */
    public List<SesiPerkuliahan> filter(Integer matkulId, String tglAwal, String tglAkhir, String status) throws SQLException {
        StringBuilder sb = new StringBuilder(SELECT_JOIN + " WHERE 1=1");
        List<Object> params = new ArrayList<>();
        if (matkulId != null && matkulId > 0) { sb.append(" AND s.mata_kuliah_id=?"); params.add(matkulId); }
        if (tglAwal != null && !tglAwal.isBlank()) { sb.append(" AND s.tanggal>=?"); params.add(tglAwal); }
        if (tglAkhir != null && !tglAkhir.isBlank()) { sb.append(" AND s.tanggal<=?"); params.add(tglAkhir); }
        if (status != null && !status.isBlank()) { sb.append(" AND s.status=?"); params.add(status); }
        sb.append(" ORDER BY s.tanggal DESC, s.jam_mulai DESC");
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {
            for (int i = 0; i < params.size(); i++) ps.setObject(i + 1, params.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                List<SesiPerkuliahan> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        }
    }

    public List<SesiPerkuliahan> findByMataKuliah(int matkulId) throws SQLException {
        return filter(matkulId, null, null, null);
    }

    public SesiPerkuliahan findById(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE s.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(SesiPerkuliahan s) throws SQLException {
        String sql = "INSERT INTO sesi_perkuliahan(mata_kuliah_id,pertemuan_ke,tanggal,jam_mulai,jam_selesai,status,topik) VALUES(?,?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, s.getMataKuliahId());
            ps.setInt(2, s.getPertemuanKe());
            ps.setString(3, s.getTanggal());
            ps.setString(4, s.getJamMulai());
            ps.setString(5, s.getJamSelesai());
            ps.setString(6, s.getStatus());
            ps.setString(7, s.getTopik());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void update(SesiPerkuliahan s) throws SQLException {
        String sql = "UPDATE sesi_perkuliahan SET mata_kuliah_id=?,pertemuan_ke=?,tanggal=?,jam_mulai=?,jam_selesai=?,status=?,topik=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, s.getMataKuliahId());
            ps.setInt(2, s.getPertemuanKe());
            ps.setString(3, s.getTanggal());
            ps.setString(4, s.getJamMulai());
            ps.setString(5, s.getJamSelesai());
            ps.setString(6, s.getStatus());
            ps.setString(7, s.getTopik());
            ps.setInt(8, s.getId());
            ps.executeUpdate();
        }
    }

    public void updateStatus(int id, String status) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE sesi_perkuliahan SET status=? WHERE id=?")) {
            ps.setString(1, status);
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM sesi_perkuliahan WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public int pertemuanTerakhir(int matkulId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COALESCE(MAX(pertemuan_ke),0) FROM sesi_perkuliahan WHERE mata_kuliah_id=?")) {
            ps.setInt(1, matkulId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getInt(1);
            }
        }
    }

    public long countByStatus(String status) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT COUNT(*) FROM sesi_perkuliahan WHERE status=?")) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.getLong(1);
            }
        }
    }
}
