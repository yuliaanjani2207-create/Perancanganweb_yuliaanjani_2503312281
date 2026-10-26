package database;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * DatabaseManager — satu-satunya kelas yang membuka koneksi ke SQLite.
 * Semua DAO mengambil koneksi dari sini agar pengelolaan terpusat.
 */
public class DatabaseManager {
    private static String dbPath = "data/absensi.db";

    /** Bisa di-override lewat sistem properti, mis. untuk pengujian:
     *  -Dabsensi.db=target/test-absensi.db */
    public static void initFromConfig() {
        String p = System.getProperty("absensi.db");
        if (p != null && !p.isBlank()) dbPath = p;
    }

    public static String getDbPath() { return dbPath; }

    public static synchronized Connection getConnection() throws SQLException {
        File f = new File(dbPath);
        if (f.getParentFile() != null) f.getParentFile().mkdirs();
        Connection c = DriverManager.getConnection("jdbc:sqlite:" + dbPath);
        try (Statement st = c.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;"); // wajib: SQLite butuh perintah eksplisit
        }
        return c;
    }

    /** Membuat seluruh tabel bila belum ada (idempotent). */
    public static void initializeSchema() throws SQLException {
        try (Connection c = getConnection(); Statement st = c.createStatement()) {

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT NOT NULL UNIQUE,
                    password_hash TEXT NOT NULL,
                    role TEXT NOT NULL CHECK (role IN ('DOSEN','MAHASISWA')),
                    mahasiswa_id INTEGER REFERENCES mahasiswa(id) ON DELETE SET NULL,
                    dosen_id INTEGER REFERENCES dosen(id) ON DELETE SET NULL,
                    nama_lengkap TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mahasiswa (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nim TEXT NOT NULL UNIQUE,
                    nama TEXT NOT NULL,
                    prodi TEXT NOT NULL,
                    email TEXT,
                    telepon TEXT,
                    status TEXT NOT NULL DEFAULT 'AKTIF' CHECK (status IN ('AKTIF','CUTI','LULUS','NONAKTIF')),
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS dosen (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    nidn TEXT NOT NULL UNIQUE,
                    nama TEXT NOT NULL,
                    prodi TEXT,
                    email TEXT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS mata_kuliah (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    kode TEXT NOT NULL UNIQUE,
                    nama TEXT NOT NULL,
                    sks INTEGER NOT NULL DEFAULT 3 CHECK (sks BETWEEN 1 AND 6),
                    dosen_id INTEGER NOT NULL REFERENCES dosen(id) ON DELETE RESTRICT,
                    created_at TEXT DEFAULT (datetime('now','localtime'))
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS peserta_mata_kuliah (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mahasiswa_id INTEGER NOT NULL REFERENCES mahasiswa(id) ON DELETE CASCADE,
                    mata_kuliah_id INTEGER NOT NULL REFERENCES mata_kuliah(id) ON DELETE CASCADE,
                    UNIQUE (mahasiswa_id, mata_kuliah_id)
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS sesi_perkuliahan (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mata_kuliah_id INTEGER NOT NULL REFERENCES mata_kuliah(id) ON DELETE CASCADE,
                    pertemuan_ke INTEGER NOT NULL CHECK (pertemuan_ke >= 1),
                    tanggal TEXT NOT NULL,
                    jam_mulai TEXT NOT NULL,
                    jam_selesai TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'TERJADWAL'
                        CHECK (status IN ('TERJADWAL','AKTIF','SELESAI')),
                    topik TEXT,
                    UNIQUE (mata_kuliah_id, pertemuan_ke)
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS absensi (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    mahasiswa_id INTEGER NOT NULL REFERENCES mahasiswa(id) ON DELETE CASCADE,
                    sesi_id INTEGER NOT NULL REFERENCES sesi_perkuliahan(id) ON DELETE CASCADE,
                    waktu_absen TEXT NOT NULL,
                    status TEXT NOT NULL DEFAULT 'HADIR' CHECK (status IN ('HADIR','TERLAMBAT')),
                    metode TEXT NOT NULL DEFAULT 'QR' CHECK (metode IN ('QR','MANUAL')),
                    UNIQUE (mahasiswa_id, sesi_id) -- mencegah absensi ganda pada sesi sama
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS token_qr (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    sesi_id INTEGER NOT NULL REFERENCES sesi_perkuliahan(id) ON DELETE CASCADE,
                    token_hash TEXT NOT NULL UNIQUE, -- hanya hash SHA-256, bukan token asli
                    created_at TEXT NOT NULL,
                    expires_at TEXT NOT NULL
                );""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS app_setting (
                    key TEXT PRIMARY KEY,
                    value TEXT NOT NULL
                );""");

            // Indeks untuk mempercepat query laporan
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_absensi_sesi ON absensi(sesi_id);");
            st.executeUpdate("CREATE INDEX IF NOT EXISTS idx_sesi_matkul ON sesi_perkuliahan(mata_kuliah_id);");
        }
    }
}
