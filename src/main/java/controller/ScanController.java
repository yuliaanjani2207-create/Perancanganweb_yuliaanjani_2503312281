package controller;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import model.User;
import service.AttendanceService;
import service.QrDecoder;
import service.ScanResult;
import util.Session;
import com.github.sarxos.webcam.Webcam;
import javafx.animation.AnimationTimer;

import java.io.File;

/**
 * Halaman pemindaian QR Code oleh mahasiswa (halaman 9).
 * - Kamera web via webcam-capture + ZXing.
 * - Mode pengujian via unggah file gambar PNG/JPG (bila tidak ada kamera).
 *   Mode uji BUKAN bukti keamanan pada penggunaan nyata.
 */
public class ScanController implements MainController.Refreshable {

    private final AttendanceService attendance = new AttendanceService();

    @FXML private ImageView camView;
    @FXML private Label lblCamStatus, lblResult;
    @FXML private TextArea txtLog;

    private boolean cameraRunning = false;
    private AnimationTimer camTimer;
    private long lastGrabNs = 0;

    @Override
    public void onShow() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) {
            if (lblResult != null) lblResult.setText("Halaman ini khusus akun mahasiswa.");
            return;
        }
        lblResult.setText("Pemindai siap. Login sebagai: " + u.getNamaLengkap() + " (" + u.getUsername() + ")");
    }

    // ============================ KAMERA ============================

    @FXML
    public void onStartCamera() {
        if (cameraRunning) return;
        try {
            Webcam webcam = Webcam.getDefault();
            if (webcam == null) {
                AlertUtil.warn("Kamera tidak ditemukan pada komputer ini.\n"
                        + "Gunakan MODE PENGUJIAN: pindai gambar QR dari file.\n"
                        + "(Mode pengujian bukan bukti keamanan kehadiran.)");
                return;
            }
            try {
                java.awt.Dimension[] views = webcam.getViewSizes();
                if (views.length > 0) webcam.setViewSize(views[views.length - 1]); // ukuran kecil agar hemat CPU
            } catch (Exception ignore) {}
            webcam.open();
            cameraRunning = true;
            lblCamStatus.setText("KAMERA AKTIF — arahkan ke QR Code dosen.");
            camTimer = new AnimationTimer() {
                @Override
                public void handle(long now) {
                    if (now - lastGrabNs < 500_000_000L) return; // ~2 fps agar hemat CPU
                    lastGrabNs = now;
                    // grab & decode di thread latar agar UI tidak macet
                    Thread th = new Thread(() -> {
                        try {
                            java.awt.image.BufferedImage bi = webcam.getImage();
                            if (bi == null) return;
                            String payload = QrDecoder.decodeAwtPublic(bi);
                            if (payload != null) Platform.runLater(() -> { stopCamera(); processPayload(payload); });
                        } catch (Exception notFound) {
                            // belum terdeteksi, abaikan frame ini
                        }
                    }, "qr-camera-scan");
                    th.setDaemon(true);
                    th.start();
                }
            };
            camTimer.start();
        } catch (Exception e) {
            AlertUtil.error("Gagal membuka kamera: " + e.getMessage()
                    + "\nSilakan gunakan mode pengujian dari file gambar.");
        }
    }

    @FXML
    public void onStopCamera() { stopCamera(); }

    private void stopCamera() {
        cameraRunning = false;
        if (camTimer != null) camTimer.stop();
        try { if (Webcam.getDefault() != null && Webcam.getDefault().isOpen()) Webcam.getDefault().close(); }
        catch (Exception ignore) {}
        lblCamStatus.setText("Kamera mati.");
    }

    // ======================= MODE PENGUJIAN =======================

    @FXML
    public void onScanFromFile() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) { AlertUtil.warn("Khusus akun mahasiswa."); return; }
        FileChooserBridge.chooseImage(camView.getScene().getWindow(), f -> {
            try {
                String payload = QrDecoder.decodeFile(f);
                processPayload(payload);
            } catch (Exception ex) {
                appendLog("GAGAL membaca QR dari file: " + ex.getMessage());
                lblResult.setText("QR pada gambar tidak terbaca: " + ex.getMessage());
            }
        });
    }

    /** Tempel payload manual juga didukung (untuk pengujian cepat). */
    @FXML
    public void onPastePayload() {
        try {
            javafx.scene.control.TextInputDialog d = new javafx.scene.control.TextInputDialog();
            d.setContentText("Tempel isi (payload) QR Code:");
            var opt = d.showAndWait();
            if (opt.isPresent() && !opt.get().isBlank()) processPayload(opt.get().trim());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    private void processPayload(String payload) {
        User u = Session.getCurrentUser();
        ScanResult r = attendance.scanAndRecord(payload, u.getMahasiswaId());
        if (r.success()) {
            AlertUtil.success(r.message() + "\n\nSesi: " + r.sesiLabel());
        } else {
            AlertUtil.error(r.message());
        }
        lblResult.setText(r.message());
        appendLog((r.success() ? "[OK]   " : "[REJECT] ") + r.message());
    }

    private void appendLog(String line) {
        txtLog.appendText(java.time.LocalDateTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss")) + "  " + line + "\n");
    }
}
