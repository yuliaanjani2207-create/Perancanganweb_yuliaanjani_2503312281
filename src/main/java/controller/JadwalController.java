package controller;

import dao.MataKuliahDao;
import dao.SesiDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import model.MataKuliah;
import model.SesiPerkuliahan;
import util.Session;
import util.ValidationUtil;

import java.time.LocalDate;
import java.util.List;

/** Halaman jadwal perkuliahan: membuat/mengubah sesi pertemuan per mata kuliah. */
public class JadwalController extends CrudController<SesiPerkuliahan> implements MainController.DosenScoped {

    private final SesiDao dao = new SesiDao();
    private final MataKuliahDao mkDao = new MataKuliahDao();
    private int dosenId = -1;

    @FXML private ComboBox<MataKuliah> cbMatkul;
    @FXML private TextField txtId, txtPertemuan, txtJamMulai, txtJamSelesai, txtTopik;
    @FXML private DatePicker dpTanggal;
    @FXML private ComboBox<String> cbStatus;

    @Override
    public void setDosenScope() {
        if (Session.getCurrentUser() != null && Session.getCurrentUser().getDosenId() != null)
            dosenId = Session.getCurrentUser().getDosenId();
    }

    @Override
    protected void reload(String keyword) throws Exception {
        List<MataKuliah> list = dosenId > 0 ? mkDao.findByDosen(dosenId) : mkDao.search(null);
        cbMatkul.setItems(FXCollections.observableArrayList(list));
        cbMatkul.setConverter(new javafx.util.StringConverter<>() {
            public String toString(MataKuliah m) { return m == null ? "" : m.getKode() + " - " + m.getNama(); }
            public MataKuliah fromString(String s) { return null; }
        });
        cbStatus.setItems(FXCollections.observableArrayList("TERJADWAL", "AKTIF", "SELESAI"));
        items.setAll(dao.filter(null, null, null, null));
    }

    @FXML
    protected void onNew() {
        txtId.clear(); txtTopik.clear();
        dpTanggal.setValue(LocalDate.now());
        txtJamMulai.setText("08:00"); txtJamSelesai.setText("09:40");
        cbStatus.setValue("TERJADWAL");
        if (cbMatkul.getValue() != null) {
            try { txtPertemuan.setText(String.valueOf(dao.pertemuanTerakhir(cbMatkul.getValue().getId()) + 1)); }
            catch (Exception ignore) {}
        }
    }

    @FXML
    protected void onEdit() {
        SesiPerkuliahan s = selected();
        if (s == null) { AlertUtil.warn("Pilih baris sesi."); return; }
        txtId.setText(String.valueOf(s.getId()));
        for (MataKuliah m : cbMatkul.getItems())
            if (m.getId() == s.getMataKuliahId()) { cbMatkul.setValue(m); break; }
        txtPertemuan.setText(String.valueOf(s.getPertemuanKe()));
        dpTanggal.setValue(LocalDate.parse(s.getTanggal()));
        txtJamMulai.setText(s.getJamMulai());
        txtJamSelesai.setText(s.getJamSelesai());
        cbStatus.setValue(s.getStatus());
        txtTopik.setText(s.getTopik());
    }

    @FXML
    protected void onSave() {
        MataKuliah mk = cbMatkul.getValue();
        String err = ValidationUtil.firstError(
                mk == null ? "Pilih mata kuliah." : null,
                ValidationUtil.required(txtPertemuan.getText().trim(), "Pertemuan ke-"),
                dpTanggal.getValue() == null ? "Tanggal wajib dipilih." : null,
                ValidationUtil.pattern(txtJamMulai.getText().trim(), "\\d{2}:\\d{2}", "Jam mulai format HH:mm"),
                ValidationUtil.pattern(txtJamSelesai.getText().trim(), "\\d{2}:\\d{2}", "Jam selesai format HH:mm"));
        if (err != null) { AlertUtil.error(err); return; }
        try {
            SesiPerkuliahan s = new SesiPerkuliahan();
            s.setMataKuliahId(mk.getId());
            s.setPertemuanKe(Integer.parseInt(txtPertemuan.getText().trim()));
            s.setTanggal(dpTanggal.getValue().toString());
            s.setJamMulai(txtJamMulai.getText().trim());
            s.setJamSelesai(txtJamSelesai.getText().trim());
            s.setStatus(cbStatus.getValue());
            s.setTopik(txtTopik.getText().trim());
            if (txtId.getText().isBlank()) { dao.insert(s); AlertUtil.success("Sesi/jadwal dibuat."); }
            else { s.setId(Integer.parseInt(txtId.getText().trim())); dao.update(s); AlertUtil.success("Jadwal diperbarui."); }
            onNew(); onSearch();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))
                AlertUtil.error("Pertemuan tsb sudah ada untuk mata kuliah ini.");
            else AlertUtil.error("Gagal menyimpan: " + e.getMessage());
        }
    }

    @FXML
    protected void onDelete() {
        SesiPerkuliahan s = selected();
        if (s == null) { AlertUtil.warn("Pilih baris yang akan dihapus."); return; }
        if (!AlertUtil.confirm("Hapus Jadwal", "Hapus " + s.getLabelSingkat() + "? Riwayat absensi sesi ikut terhapus.")) return;
        try { dao.delete(s.getId()); AlertUtil.success("Jadwal dihapus."); onSearch(); }
        catch (Exception e) { AlertUtil.error("Gagal menghapus: " + e.getMessage()); }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(table, "jadwal_sesi"); }
}
