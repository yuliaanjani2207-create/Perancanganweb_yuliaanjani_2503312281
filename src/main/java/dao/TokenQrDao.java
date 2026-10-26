package dao;

import database.DatabaseManager;
import model.TokenQr;

import java.sql.*;

/** Akses data tabel token_qr — menyimpan HASH token, bukan token aslinya. */
public class TokenQrDao {

    public boolean exists(String tokenHash) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM token_qr WHERE token_hash=?")) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }

    public TokenQr findByHash(String tokenHash) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("SELECT * FROM token_qr WHERE token_hash=?")) {
            ps.setString(1, tokenHash);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                TokenQr t = new TokenQr();
                t.setId(rs.getInt("id"));
                t.setSesiId(rs.getInt("sesi_id"));
                t.setTokenHash(rs.getString("token_hash"));
                t.setCreatedAt(rs.getString("created_at"));
                t.setExpiresAt(rs.getString("expires_at"));
                return t;
            }
        }
    }

    public void insert(TokenQr t) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO token_qr(sesi_id,token_hash,created_at,expires_at) VALUES(?,?,?,?)")) {
            ps.setInt(1, t.getSesiId());
            ps.setString(2, t.getTokenHash());
            ps.setString(3, t.getCreatedAt());
            ps.setString(4, t.getExpiresAt());
            ps.executeUpdate();
        }
    }

    /** Hapus semua hash token milik satu sesi (dipanggil saat QR dirotasi/ditutup). */
    public void deleteBySesi(int sesiId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement("DELETE FROM token_qr WHERE sesi_id=?")) {
            ps.setInt(1, sesiId);
            ps.executeUpdate();
        }
    }
}
