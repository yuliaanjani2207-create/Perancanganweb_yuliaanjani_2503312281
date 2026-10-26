package dao;

import database.DatabaseManager;
import model.User;
import util.PasswordUtil;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel users. */
public class UserDao {

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setRole(rs.getString("role"));
        int mid = rs.getInt("mahasiswa_id");
        u.setMahasiswaId(rs.wasNull() ? null : mid);
        int did = rs.getInt("dosen_id");
        u.setDosenId(rs.wasNull() ? null : did);
        u.setNamaLengkap(rs.getString("nama_lengkap"));
        return u;
    }

    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM users WHERE lower(username) = lower(?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** Autentikasi: bandingkan password masukan dengan hash BCrypt tersimpan. */
    public User authenticate(String username, String password) throws SQLException {
        User u = findByUsername(username);
        if (u == null) return null;
        return PasswordUtil.verify(password, u.getPasswordHash()) ? u : null;
    }

    public List<User> findAll() throws SQLException {
        List<User> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM users ORDER BY role, username")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int insert(User u) throws SQLException {
        String sql = "INSERT INTO users(username,password_hash,role,mahasiswa_id,dosen_id,nama_lengkap) VALUES(?,?,?,?,?,?)";
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getRole());
            ps.setObject(4, u.getMahasiswaId());
            ps.setObject(5, u.getDosenId());
            ps.setString(6, u.getNamaLengkap());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getInt(1) : -1;
            }
        }
    }

    public void updatePassword(int userId, String newHash) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("UPDATE users SET password_hash=? WHERE id=?")) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public boolean existsByUsername(String username) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE lower(username)=lower(?)")) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}
