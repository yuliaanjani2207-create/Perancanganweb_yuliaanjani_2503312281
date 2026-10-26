package database;

import dao.*;
import model.*;
import service.AuthService;

import java.sql.Connection;
import java.sql.Statement;

/** Mengisi data contoh agar aplikasi langsung bisa diuji. */
public class SeedData {

    public static boolean isEmpty() throws Exception {
        try (Connection c = DatabaseManager.getConnection();
             Statement st = c.createStatement();
             var rs = st.executeQuery("SELECT COUNT(*) FROM users")) {
            return rs.getInt(1) == 0;
        }
    }

    public static void seedIfEmpty() throws Exception {
        if (!isEmpty()) return;

        // --- Dosen + akun (password: 123456) ---
        DosenDao dosenDao = new DosenDao();
        int dosenId = dosenDao.insert(newDosen("198001012005011001", "Dr. Ahmad Fauzi, M.Kom.", "Teknik Informatika", "ahmad@univ.ac.id"));
        dosenDao.insert(newDosen("197512102003122001", "Prof. Dr. Siti Aminah, M.Sc.", "Teknik Informatika", "siti@univ.ac.id"));
        AuthService auth = new AuthService();
        Dosen d1 = dosenDao.findById(dosenId);
        auth.createAccountForDosen(d1, "123456");

        // --- Mata kuliah ---
        MataKuliahDao mkDao = new MataKuliahDao();
        int IF201 = mkDao.insert(newMK("IF201", "Pemrograman Berorientasi Objek", 3, dosenId));
        int IF202 = mkDao.insert(newMK("IF202", "Basis Data", 3, dosenId));
        int IF301 = mkDao.insert(newMK("IF301", "Kecerdasan Buatan", 3, dosenId));

        // --- Mahasiswa + akun (username = NIM, password: 123456) ---
        MahasiswaDao mhsDao = new MahasiswaDao();
        PesertaDao pesertaDao = new PesertaDao();
        String[][] data = {
            {"211101001", "Budi Santoso",     "Teknik Informatika"},
            {"211101002", "Ani Wijaya",       "Teknik Informatika"},
            {"211101003", "Chandra Pratama",  "Teknik Informatika"},
            {"211101004", "Dewi Lestari",     "Teknik Informatika"},
            {"211101005", "Eko Prabowo",      "Sistem Informasi"},
        };
        for (String[] r : data) {
            Mahasiswa m = new Mahasiswa();
            m.setNim(r[0]); m.setNama(r[1]); m.setProdi(r[2]);
            m.setEmail(r[0].toLowerCase() + "@student.univ.ac.id");
            m.setStatus("AKTIF");
            int id = mhsDao.insert(m);
            m.setId(id);
            auth.createAccountForMahasiswa(m, "123456");
            // 4 mahasiswa Informatika mengambil ketiga MK; Eko hanya Basis Data
            if (r[2].equals("Teknik Informatika")) {
                pesertaDao.tambah(id, IF201);
                pesertaDao.tambah(id, IF202);
                pesertaDao.tambah(id, IF301);
            } else {
                pesertaDao.tambah(id, IF202);
            }
        }

        // --- Jadwal sesi perkuliahan ---
        SesiDao sesiDao = new SesiDao();
        buatSesi(sesiDao, IF201, 1, "2026-10-19", "08:00", "09:40", "Intro JavaFX");
        buatSesi(sesiDao, IF201, 2, "2026-10-26", "08:00", "09:40", "Event & Binding");
        buatSesi(sesiDao, IF202, 1, "2026-10-20", "10:00", "11:40", "Model Relasional");
        buatSesi(sesiDao, IF301, 1, "2026-10-21", "13:00", "14:40", "Pencarian & Heuristik");
    }

    private static void buatSesi(SesiDao dao, int mk, int p, String tgl, String mulai, String selesai, String topik) throws Exception {
        SesiPerkuliahan s = new SesiPerkuliahan();
        s.setMataKuliahId(mk); s.setPertemuanKe(p); s.setTanggal(tgl);
        s.setJamMulai(mulai); s.setJamSelesai(selesai); s.setTopik(topik);
        dao.insert(s);
    }

    private static Dosen newDosen(String nidn, String nama, String prodi, String email) {
        Dosen d = new Dosen();
        d.setNidn(nidn); d.setNama(nama); d.setProdi(prodi); d.setEmail(email);
        return d;
    }

    private static MataKuliah newMK(String kode, String nama, int sks, int dosenId) {
        MataKuliah m = new MataKuliah();
        m.setKode(kode); m.setNama(nama); m.setSks(sks); m.setDosenId(dosenId);
        return m;
    }
}
