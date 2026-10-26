package dao;

import database.DatabaseManager;
import model.PesertaMataKuliah;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/** Akses data tabel peserta_mata_kuliah (KRS per mata kuliah). */
public class PesertaDao {

    private static final String SELECT_JOIN = """
        SELECT p.id, p.mahasiswa_id, p.mata_kuliah_id,
               m.nim, m.nama AS nama_mhs, mk.kode AS kode_mk, mk.nama AS nama_mk
        FROM peserta_mata_kuliah p
        JOIN mahasiswa m ON m.id = p.mahasiswa_id
        JOIN mata_kuliah mk ON mk.id = p.mata_kuliah_id
        """;

    private PesertaMataKuliah map(ResultSet rs) throws SQLException {
        PesertaMataKuliah p = new PesertaMataKuliah();
        p.setId(rs.getInt("id"));
        p.setMahasiswaId(rs.getInt("mahasiswa_id"));
        p.setMataKuliahId(rs.getInt("mata_kuliah_id"));
        p.setNim(rs.getString("nim"));
        p.setNamaMahasiswa(rs.getString("nama_mhs"));
        p.setKodeMatkul(rs.getString("kode_mk"));
        p.setNamaMatkul(rs.getString("nama_mk"));
        return p;
    }

    public List<PesertaMataKuliah> findByMataKuliah(int matkulId) throws SQLException {
        List<PesertaMataKuliah> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE p.mata_kuliah_id=? ORDER BY m.nim")) {
            ps.setInt(1, matkulId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<PesertaMataKuliah> findByMahasiswa(int mhsId) throws SQLException {
        List<PesertaMataKuliah> list = new ArrayList<>();
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(SELECT_JOIN + " WHERE p.mahasiswa_id=? ORDER BY mk.kode")) {
            ps.setInt(1, mhsId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public boolean isTerdaftar(int mahasiswaId, int matkulId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "SELECT 1 FROM peserta_mata_kuliah WHERE mahasiswa_id=? AND mata_kuliah_id=?")) {
            ps.setInt(1, mahasiswaId);
            ps.setInt(2, matkulId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void tambah(int mahasiswaId, int matkulId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO peserta_mata_kuliah(mahasiswa_id,mata_kuliah_id) VALUES(?,?)")) {
            ps.setInt(1, mahasiswaId);
            ps.setInt(2, matkulId);
            ps.executeUpdate();
        }
    }

    public void hapus(int mahasiswaId, int matkulId) throws SQLException {
        try (Connection c = DatabaseManager.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     "DELETE FROM peserta_mata_kuliah WHERE mahasiswa_id=? AND mata_kuliah_id=?")) {
            ps.setInt(1, mahasiswaId);
            ps.setInt(2, matkulId);
            ps.executeUpdate();
        }
    }
}
