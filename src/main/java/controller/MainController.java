package controller;

import dao.MahasiswaDao;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.util.Callback;
import model.User;
import service.BackupService;
import util.Session;

import java.io.File;

/**
 * Kerangka utama aplikasi: header, sidebar navigasi, dan area konten.
 * Sidebar dibangun dinamis sesuai peran pengguna (dosen/mahasiswa).
 */
public class MainController {

    @FXML private BorderPane rootPane;
    @FXML private Label lblUserName;
    @FXML private Label lblRole;
    @FXML private javafx.scene.layout.VBox navBox;
    @FXML private BorderPane contentArea;

    /** Item navigasi: label + file FXML tujuan. */
    private record NavItem(String label, String fxml) {}

    @FXML
    public void initialize() {
        // placeholder; isi sebenarnya dimuat setelah login oleh initForUser()
    }

    public void initForUser() {
        User u = Session.getCurrentUser();
        if (u == null) return;
        lblUserName.setText(u.getNamaLengkap() == null ? u.getUsername() : u.getNamaLengkap());
        lblRole.setText(Session.isDosen() ? "DOSEN" : "MAHASISWA");

        java.util.List<NavItem> items = new java.util.ArrayList<>();
        if (Session.isDosen()) {
            items.add(new NavItem("Dashboard", "/fxml/DashboardDosenView.fxml"));
            items.add(new NavItem("Data Mahasiswa", "/fxml/MahasiswaView.fxml"));
            items.add(new NavItem("Data Dosen", "/fxml/DosenView.fxml"));
            items.add(new NavItem("Mata Kuliah", "/fxml/MataKuliahView.fxml"));
            items.add(new NavItem("Jadwal Perkuliahan", "/fxml/JadwalView.fxml"));
            items.add(new NavItem("Sesi Absensi & QR", "/fxml/SesiView.fxml"));
            items.add(new NavItem("Daftar Kehadiran", "/fxml/KehadiranView.fxml"));
            items.add(new NavItem("Riwayat Absensi", "/fxml/RiwayatView.fxml"));
            items.add(new NavItem("Rekap & Laporan", "/fxml/RekapView.fxml"));
            items.add(new NavItem("Pengaturan", "/fxml/PengaturanView.fxml"));
        } else {
            items.add(new NavItem("Dashboard", "/fxml/DashboardMhsView.fxml"));
            items.add(new NavItem("Profil Saya", "/fxml/ProfilMhsView.fxml"));
            items.add(new NavItem("Mata Kuliah Saya", "/fxml/MatkulMhsView.fxml"));
            items.add(new NavItem("Scan QR Absensi", "/fxml/ScanView.fxml"));
            items.add(new NavItem("Status Kehadiran", "/fxml/KehadiranMhsView.fxml"));
            items.add(new NavItem("Pengaturan Akun", "/fxml/PengaturanView.fxml"));
        }

        for (NavItem item : items) {
            Button b = new Button(item.label());
            b.getStyleClass().add("nav-button");
            b.setMaxWidth(Double.MAX_VALUE);
            b.setOnAction(e -> loadContent(item.fxml()));
            navBox.getChildren().add(b);
        }
        // halaman pertama = dashboard
        if (!items.isEmpty()) loadContent(items.get(0).fxml());
    }

    /** Memuat halaman FXML ke area konten dengan kerangka callback generik. */
    private void loadContent(String fxmlPath) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            javafx.scene.Parent page = loader.load();
            contentArea.setCenter(page);
            Object ctrl = loader.getController();
            if (ctrl instanceof DosenScoped s) s.setDosenScope();
            if (ctrl instanceof Refreshable r) r.onShow();
        } catch (Exception ex) {
            AlertUtil.error("Gagal memuat halaman " + fxmlPath + ": " + ex.getMessage());
        }
    }

    /** Interface penanda untuk controller yang butuh tahu dosen aktif. */
    public interface DosenScoped { void setDosenScope(); }
    /** Interface penanda agar halaman menyegarkan datanya saat dibuka. */
    public interface Refreshable { void onShow(); }

    @FXML
    public void onBackupNow() {
        try {
            File f = BackupService.backupTo(new File("data/backup/manual_" + System.currentTimeMillis() + ".db"));
            AlertUtil.success("Cadangan dibuat:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            AlertUtil.error("Backup gagal: " + ex.getMessage());
        }
    }

    @FXML
    public void onLogout() {
        if (!AlertUtil.confirm("Keluar", "Yakin ingin keluar dari akun?")) return;
        Session.logout();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.stage.Stage stage = (javafx.stage.Stage) rootPane.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root, 960, 640));
            stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        } catch (Exception ex) {
            AlertUtil.error("Gagal kembali ke halaman login: " + ex.getMessage());
        }
    }

    @FXML
    public void onExit() {
        if (AlertUtil.confirm("Keluar Aplikasi", "Tutup aplikasi sekarang?")) {
            BackupService.autoBackupIfEnabled();
            Platform.exit();
        }
    }
}
