package controller;

import dao.SettingDao;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import model.User;
import service.AuthService;
import service.BackupService;
import util.Session;
import util.ValidationUtil;

import java.io.File;

/** Pengaturan aplikasi: TTL QR, backup, dan ganti password. */
public class PengaturanController implements MainController.Refreshable {

    private final SettingDao settings = new SettingDao();
    private final AuthService auth = new AuthService();

    @FXML private TextField txtTtl;
    @FXML private CheckBox cbAutoBackup;
    @FXML private PasswordField txtPassBaru, txtPassUlang;

    @Override
    public void onShow() {
        try {
            txtTtl.setText(new service.AttendanceService().getQrTtlSeconds() + "");
            cbAutoBackup.setSelected("true".equalsIgnoreCase(settings.get("auto_backup", "true")));
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onSaveSettings() {
        try {
            String err = ValidationUtil.firstError(
                    ValidationUtil.required(txtTtl.getText().trim(), "Masa berlaku QR"),
                    null);
            if (err != null) { AlertUtil.error(err); return; }
            int ttl;
            try { ttl = Integer.parseInt(txtTtl.getText().trim()); }
            catch (NumberFormatException e) { AlertUtil.error("Masa berlaku QR harus angka."); return; }
            if (ttl < 10 || ttl > 600) { AlertUtil.error("Masa berlaku QR harus antara 10 dan 600 detik."); return; }
            new service.AttendanceService().setQrTtlSeconds(ttl);
            settings.set("auto_backup", cbAutoBackup.isSelected() ? "true" : "false");
            AlertUtil.success("Pengaturan disimpan. Masa berlaku QR: " + ttl + " detik.");
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onChangePassword() {
        User u = Session.getCurrentUser();
        if (u == null) return;
        String p1 = txtPassBaru.getText(), p2 = txtPassUlang.getText();
        if (p1 == null || p1.length() < 6) { AlertUtil.error("Password minimal 6 karakter."); return; }
        if (!p1.equals(p2)) { AlertUtil.error("Konfirmasi password tidak sama."); return; }
        try {
            auth.changePassword(u, p1);
            txtPassBaru.clear(); txtPassUlang.clear();
            AlertUtil.success("Password berhasil diubah.");
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onBackupNow() {
        try {
            File f = BackupService.backupTo(new File("data/backup/pengaturan_" + System.currentTimeMillis() + ".db"));
            AlertUtil.success("Cadangan dibuat:\n" + f.getAbsolutePath());
        } catch (Exception e) { AlertUtil.error("Backup gagal: " + e.getMessage()); }
    }
}
