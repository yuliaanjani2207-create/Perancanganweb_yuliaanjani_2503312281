package dao;

import database.DatabaseManager;
import model.Dosen;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel dosen. */
public class DosenDao {

    private Dosen map(ResultSet rs) throws SQLException {
        Dosen d = new Dosen();
        d.setId(rs.getInt("id"));
        d.setNidn(rs.getString("nidn"));
        d.setNama(rs.getString("nama"));
        d.setProdi(rs.getString("prodi"));
        d.setEmail(rs.getString("email"));
        return d;
    }

    public List<Dosen> search(String keyword) throws SQLException {
        List<Dosen> list = new ArrayList<>();
        boolean hasKey = keyword != null && !keyword.isBlank();
        String sql = hasKey
                ? "SELECT * FROM dosen WHERE nidn LIKE ? OR nama LIKE ? ORDER BY nama"
                : "SELECT * FROM dosen ORDER BY nama";
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

    public Dosen findById(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM dosen WHERE id=?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    public int insert(Dosen d) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO dosen(nidn,nama,prodi,email) VALUES(?,?,?,?)",
                     Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, d.getNidn());
            ps.setString(2, d.getNama());
            ps.setString(3, d.getProdi());
            ps.setString(4, d.getEmail());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void update(Dosen d) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE dosen SET nidn=?,nama=?,prodi=?,email=? WHERE id=?")) {
            ps.setString(1, d.getNidn());
            ps.setString(2, d.getNama());
            ps.setString(3, d.getProdi());
            ps.setString(4, d.getEmail());
            ps.setInt(5, d.getId());
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM dosen WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public long count() throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM dosen")) {
            return rs.getLong(1);
        }
    }
}
