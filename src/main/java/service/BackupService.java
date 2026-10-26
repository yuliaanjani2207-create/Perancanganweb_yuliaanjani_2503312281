package service;

import database.DatabaseManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.Statement;

/** Pencadangan basis data SQLite ke file lain (fitur Backup). */
public final class BackupService {
    private BackupService() {}

    /** Cara portabel: gunakan perintah VACUUM INTO bawaan SQLite. */
    public static File backupTo(File dest) throws Exception {
        File parent = dest.getParentFile();
        if (parent != null) parent.mkdirs();
        try (Connection c = DatabaseManager.getConnection(); Statement st = c.createStatement()) {
            String esc = dest.getAbsolutePath().replace("'", "''");
            st.execute("VACUUM INTO '" + esc + "'");
        }
        return dest;
    }

    /** Cadangkan juga saat aplikasi ditutup bila pengaturan aktif. */
    public static void autoBackupIfEnabled() {
        try {
            dao.SettingDao sd = new dao.SettingDao();
            String enabled = sd.get("auto_backup", "true");
            if (!"true".equalsIgnoreCase(enabled)) return;
            String dirPath = sd.get("backup_dir", "data/backup");
            File dir = new File(dirPath);
            dir.mkdirs();
            String name = "absensi_backup_" + java.time.LocalDateTime.now()
                    .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".db";
            backupTo(new File(dir, name));
        } catch (Exception ignore) {
            // backup bukan fitur kritis; kegagalan tidak boleh menghentikan aplikasi
        }
    }
}
