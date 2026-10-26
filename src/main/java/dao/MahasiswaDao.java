package dao;

import database.DatabaseManager;
import model.Mahasiswa;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel mahasiswa dengan dukungan pencarian NIM/nama. */
public class MahasiswaDao {

    private Mahasiswa map(ResultSet rs) throws SQLException {
        Mahasiswa m = new Mahasiswa();
        m.setId(rs.getInt("id"));
        m.setNim(rs.getString("nim"));
        m.setNama(rs.getString("nama"));
        m.setProdi(rs.getString("prodi"));
        m.setEmail(rs.getString("email"));
        m.setTelepon(rs.getString("telepon"));
        m.setStatus(rs.getString("status"));
        return m;
    }

    public List<Mahasiswa> search(String keyword) throws SQLException {
        List<Mahasiswa> list = new ArrayList<>();
        String base = "FROM mahasiswa ";
        String sql;
        boolean hasKey = keyword != null && !keyword.isBlank();
        if (hasKey) {
            sql = "SELECT * " + base + "WHERE nim LIKE ? OR nama LIKE ? OR prodi LIKE ? ORDER BY nim";
        } else {
            sql = "SELECT * " + base + "ORDER BY nim";
        }
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            if (hasKey) {
                String k = "%" + keyword.trim() + "%";
                ps.setString(1, k); ps.setString(2, k); ps.setString(3, k);
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public Mahasiswa findById(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM mahasiswa WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public Mahasiswa findByNim(String nim) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM mahasiswa WHERE nim=?")) {
            ps.setString(1, nim);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Mahasiswa m) throws SQLException {
        String sql = "INSERT INTO mahasiswa(nim,nama,prodi,email,telepon,status) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getNim());
            ps.setString(2, m.getNama());
            ps.setString(3, m.getProdi());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getTelepon());
            ps.setString(6, m.getStatus());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void update(Mahasiswa m) throws SQLException {
        String sql = "UPDATE mahasiswa SET nim=?,nama=?,prodi=?,email=?,telepon=?,status=? WHERE id=?";
        try (Connection c = DatabaseManager.getConnection(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getNim());
            ps.setString(2, m.getNama());
            ps.setString(3, m.getProdi());
            ps.setString(4, m.getEmail());
            ps.setString(5, m.getTelepon());
            ps.setString(6, m.getStatus());
            ps.setInt(7, m.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM mahasiswa WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM mahasiswa")) {
            return rs.getLong(1);
        }
    }
}
