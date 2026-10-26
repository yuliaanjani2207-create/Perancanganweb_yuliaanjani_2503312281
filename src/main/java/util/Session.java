package util;

import model.User;

/** Menyimpan state pengguna yang sedang login dalam satu proses aplikasi. */
public final class Session {
    private static User currentUser;
    private Session() {}

    public static void login(User u) { currentUser = u; }
    public static void logout() { currentUser = null; }
    public static User getCurrentUser() { return currentUser; }
    public static boolean isDosen() { return currentUser != null && "DOSEN".equals(currentUser.getRole()); }
    public static boolean isMahasiswa() { return currentUser != null && "MAHASISWA".equals(currentUser.getRole()); }
}
