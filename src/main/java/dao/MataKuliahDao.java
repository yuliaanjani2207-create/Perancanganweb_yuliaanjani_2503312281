package dao;

import database.DatabaseManager;
import model.MataKuliah;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel mata_kuliah (dengan JOIN nama dosen untuk tampilan). */
public class MataKuliahDao {

    private static final String SELECT_JOIN =
        "SELECT mk.*, d.nama AS nama_dosen FROM mata_kuliah mk JOIN dosen d ON d.id = mk.dosen_id ";

    private MataKuliah map(ResultSet rs) throws SQLException {
        MataKuliah m = new MataKuliah();
        m.setId(rs.getInt("id"));
        m.setKode(rs.getString("kode"));
        m.setNama(rs.getString("nama"));
        m.setSks(rs.getInt("sks"));
        m.setDosenId(rs.getInt("dosen_id"));
        m.setNamaDosen(rs.getString("nama_dosen"));
        return m;
    }

    public List<MataKuliah> search(String keyword) throws SQLException {
        List<MataKuliah> list = new ArrayList<>();
        boolean hasKey = keyword != null && !keyword.isBlank();
        String sql = hasKey
                ? SELECT_JOIN + "WHERE mk.kode LIKE ? OR mk.nama LIKE ? ORDER BY mk.kode"
                : SELECT_JOIN + "ORDER BY mk.kode";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasKey) {
                String k = "%" + keyword.trim() + "%";
                ps.setString(1, k); ps.setString(2, k);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Daftar mata kuliah yang diampu dosen tertentu (untuk dashboard dosen). */
    public List<MataKuliah> findByDosen(int dosenId) throws SQLException {
        List<MataKuliah> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + "WHERE mk.dosen_id=? ORDER BY mk.kode")) {
            ps.setInt(1, dosenId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    /** Daftar mata kuliah yang diikuti mahasiswa tertentu. */
    public List<MataKuliah> findByMahasiswa(int mahasiswaId) throws SQLException {
        List<MataKuliah> list = new ArrayList<>();
        String sql = SELECT_JOIN + "JOIN peserta_mata_kuliah p ON p.mata_kuliah_id = mk.id WHERE p.mahasiswa_id=? ORDER BY mk.kode";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, mahasiswaId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public MataKuliah findById(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + "WHERE mk.id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(MataKuliah m) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO mata_kuliah(kode,nama,sks,dosen_id) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getKode());
            ps.setString(2, m.getNama());
            ps.setInt(3, m.getSks());
            ps.setInt(4, m.getDosenId());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void update(MataKuliah m) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE mata_kuliah SET kode=?,nama=?,sks=?,dosen_id=? WHERE id=?")) {
            ps.setString(1, m.getKode());
            ps.setString(2, m.getNama());
            ps.setInt(3, m.getSks());
            ps.setInt(4, m.getDosenId());
            ps.setInt(5, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM mata_kuliah WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM mata_kuliah")) {
            return rs.getLong(1);
        }
    }
}
