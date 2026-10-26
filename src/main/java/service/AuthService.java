package service;

import dao.MahasiswaDao;
import dao.UserDao;
import model.Mahasiswa;
import model.User;
import util.PasswordUtil;

import java.sql.SQLException;

/** Proses login & pembuatan akun pengujian. */
public class AuthService {
    private final UserDao userDao = new UserDao();
    private final MahasiswaDao mahasiswaDao = new MahasiswaDao();

    public User login(String username, String password) throws SQLException {
        return userDao.authenticate(username, password);
    }

    /** Membuat akun untuk mahasiswa baru (username = NIM). */
    public void createAccountForMahasiswa(Mahasiswa m, String password) throws SQLException {
        if (userDao.existsByUsername(m.getNim())) return; // sudah punya akun
        User u = new User();
        u.setUsername(m.getNim());
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setRole("MAHASISWA");
        u.setMahasiswaId(m.getId());
        u.setNamaLengkap(m.getNama());
        userDao.insert(u);
    }

    /** Membuat akun dosen (username = NIDN). */
    public void createAccountForDosen(model.Dosen d, String password) throws SQLException {
        if (userDao.existsByUsername(d.getNidn())) return;
        User u = new User();
        u.setUsername(d.getNidn());
        u.setPasswordHash(PasswordUtil.hash(password));
        u.setRole("DOSEN");
        u.setDosenId(d.getId());
        u.setNamaLengkap(d.getNama());
        userDao.insert(u);
    }

    public void changePassword(User u, String newPassword) throws SQLException {
        userDao.updatePassword(u.getId(), PasswordUtil.hash(newPassword));
    }
}
