package controller;

import dao.AbsensiDao;
import dao.PesertaDao;
import dao.SesiDao;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import model.PesertaMataKuliah;
import model.SesiPerkuliahan;
import service.AttendanceService;
import service.QrImageUtil;
import service.QrTokenService;

import java.util.List;

/**
 * Halaman pembuatan sesi absensi + tampilan QR CODE DINAMIS (halaman 7 & 8).
 * QR dirotasi otomatis setiap TTL; hitung mundur ditampilkan.
 */
public class SesiController implements MainController.Refreshable, MainController.DosenScoped {

    private final SesiDao sesiDao = new SesiDao();
    private final AttendanceService attendance = new AttendanceService();
    private final PesertaDao pesertaDao = new PesertaDao();
    private final AbsensiDao absensiDao = new AbsensiDao();

    @FXML private ComboBox<SesiPerkuliahan> cbSesi;
    @FXML private ImageView imgQr;
    @FXML private Label lblStatus, lblCountdown, lblInfoSesi, lblHadir, lblBelum;
    @FXML private javafx.scene.layout.VBox qrPane;

    private String currentPayload;
    private long expiryEpoch;
    private AnimationTimer timer;
    private int activeSesiId = -1;

    @Override
    public void setDosenScope() {}

    @Override
    public void onShow() {
        try {
            List<SesiPerkuliahan> list = sesiDao.filter(null, null, null, null);
            cbSesi.setItems(javafx.collections.FXCollections.observableArrayList(list));
            cbSesi.setConverter(new javafx.util.StringConverter<>() {
                public String toString(SesiPerkuliahan s) {
                    return s == null ? "" : s.getKodeMatkul() + " | " + s.getLabelSingkat() + " [" + s.getStatus() + "]";
                }
                public SesiPerkuliahan fromString(String v) { return null; }
            });
            startTimer();
            refreshDetail();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    /** Timer 1 detik untuk hitung mundur & rotasi QR otomatis. */
    private void startTimer() {
        if (timer != null) timer.stop();
        timer = new AnimationTimer() {
            long last = 0;
            @Override
            public void handle(long nowNano) {
                if (nowNano - last < 1_000_000_000L) return;
                last = nowNano;
                if (activeSesiId < 0) return;
                long sisa = Math.max(0, expiryEpoch - java.time.Instant.now().getEpochSecond());
                lblCountdown.setText("Berlaku hingga: " + sisa + " detik");
                if (sisa <= 0) rotateQr(true); // kedaluwarsa -> terbitkan token baru otomatis
            }
        };
        timer.start();
    }

    @FXML
    public void onSesiChanged() { refreshDetail(); }

    private void refreshDetail() {
        SesiPerkuliahan s = cbSesi.getValue();
        if (s == null) { lblInfoSesi.setText("Pilih sesi perkuliahan."); return; }
        lblInfoSesi.setText(s.getNamaMatkul() + " — Pertemuan ke-" + s.getPertemuanKe()
                + "\nTanggal: " + s.getTanggal() + "  Jam: " + s.getJamMulai() + "-" + s.getJamSelesai()
                + "\nStatus: " + s.getStatus() + (s.getTopik() == null || s.getTopik().isBlank() ? "" : "  Topik: " + s.getTopik()));
        if ("AKTIF".equals(s.getStatus())) {
            if (activeSesiId != s.getId()) { // tampilkan QR yang sedang aktif pada DB
                try {
                    currentPayload = attendance.issueNewToken(s.getId());
                    activeSesiId = s.getId();
                    showQr();
                } catch (Exception e) { AlertUtil.error(e.getMessage()); }
            }
            refreshAttendanceLists(s.getId());
        } else {
            activeSesiId = -1;
            imgQr.setImage(null);
            lblCountdown.setText("");
            refreshAttendanceLists(s.getId());
        }
    }

    private void rotateQr(boolean auto) {
        if (activeSesiId < 0) return;
        try {
            currentPayload = attendance.issueNewToken(activeSesiId);
            showQr();
            if (!auto) lblStatus.setText("QR Code baru diterbitkan (rotasi manual).");
        } catch (Exception e) { AlertUtil.error("Gagal rotasi QR: " + e.getMessage()); }
    }

    private void showQr() {
        try {
            QrTokenService.Parsed p = QrTokenService.parse(currentPayload);
            expiryEpoch = p.expiryEpochSec();
            imgQr.setImage(QrImageUtil.toFxImage(currentPayload, 320));
            lblStatus.setText("SESI AKTIF — QR berotasi otomatis tiap " + attendance.getQrTtlSeconds() + " detik.");
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    private void refreshAttendanceLists(int sesiId) {
        try {
            SesiPerkuliahan s = sesiDao.findById(sesiId);
            List<PesertaMataKuliah> peserta = pesertaDao.findByMataKuliah(s.getMataKuliahId());
            var hadir = absensiDao.findBySesi(sesiId);
            StringBuilder sbH = new StringBuilder(), sbB = new StringBuilder();
            for (var p : peserta) {
                boolean h = hadir.stream().anyMatch(a -> a.getMahasiswaId() == p.getMahasiswaId());
                if (h) sbH.append("✓ ").append(p.getNim()).append(" ").append(p.getNamaMahasiswa()).append("\n");
                else   sbB.append("• ").append(p.getNim()).append(" ").append(p.getNamaMahasiswa()).append("\n");
            }
            lblHadir.setText("SUDAH HADIR (" + hadir.size() + "/" + peserta.size() + "):\n\n" + sbH);
            lblBelum.setText("BELUM HADIR:\n\n" + (sbB.isEmpty() ? "(semua sudah hadir)" : sbB));
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onStartSession() {
        SesiPerkuliahan s = cbSesi.getValue();
        if (s == null) { AlertUtil.warn("Pilih sesi terlebih dahulu."); return; }
        try {
            currentPayload = attendance.startSession(s.getId());
            activeSesiId = s.getId();
            showQr();
            lblStatus.setText("Sesi dimulai. Silakan mahasiswa memindai QR.");
            onShow(); // segarkan daftar combo (status berubah)
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onRotateNow() {
        if (activeSesiId < 0) { AlertUtil.warn("Tidak ada sesi aktif."); return; }
        rotateQr(false);
    }

    @FXML
    public void onCloseSession() {
        SesiPerkuliahan s = cbSesi.getValue();
        if (s == null) { AlertUtil.warn("Pilih sesi terlebih dahulu."); return; }
        if (!AlertUtil.confirm("Tutup Sesi", "Tutup sesi " + s.getLabelSingkat()
                + "?\nSemua QR Code sesi langsung gugur dan tidak bisa dipakai lagi.")) return;
        try {
            attendance.closeSession(s.getId());
            activeSesiId = -1;
            imgQr.setImage(null);
            lblStatus.setText("Sesi DITUTUP. Token QR sesi digugurkan.");
            lblCountdown.setText("");
            onShow();
            refreshAttendanceLists(s.getId());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    /** Simpan gambar QR ke PNG (untuk proyektor/cetak bila perlu). */
    @FXML
    public void onSaveQrPng() {
        if (currentPayload == null || activeSesiId < 0) { AlertUtil.warn("Belum ada QR aktif."); return; }
        try {
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setInitialFileName("qr_sesi_" + activeSesiId + ".png");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PNG", "*.png"));
            java.io.File f = fc.showSaveDialog(imgQr.getScene().getWindow());
            if (f != null) {
                QrImageUtil.savePng(currentPayload, f, 512);
                AlertUtil.success("QR disimpan ke " + f.getAbsolutePath()
                        + "\nCATATAN: file ini segera kedaluwarsa sesuai TTL-nya.");
            }
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }
}
