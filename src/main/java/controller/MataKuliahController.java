package controller;

import dao.DosenDao;
import dao.MataKuliahDao;
import dao.PesertaDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import model.Dosen;
import model.Mahasiswa;
import model.MataKuliah;
import model.PesertaMataKuliah;
import util.ValidationUtil;
import dao.MahasiswaDao;

import java.util.List;

/** Halaman pengelolaan mata kuliah + peserta (KRS). */
public class MataKuliahController extends CrudController<MataKuliah> {

    private final MataKuliahDao dao = new MataKuliahDao();
    private final DosenDao dosenDao = new DosenDao();
    private final PesertaDao pesertaDao = new PesertaDao();
    private final MahasiswaDao mahasiswaDao = new MahasiswaDao();

    @FXML private TextField txtId, txtKode, txtNama, txtSks;
    @FXML private ComboBox<Dosen> cbDosen;
    @FXML private javafx.scene.control.TableView<PesertaMataKuliah> tablePeserta;
    @FXML private javafx.collections.ObservableList<PesertaMataKuliah> pesertaItems;
    @FXML private ComboBox<Mahasiswa> cbMahasiswa;
    @FXML private Label lblInfoPeserta;

    @Override
    public void initialize() {
        super.initialize();
        tablePeserta.getSelectionModel().selectedItemProperty().addListener((o, a, b) -> {
            if (b != null && AlertUtil.confirm("Hapus Peserta",
                    "Keluarkan " + b.getNamaMahasiswa() + " dari mata kuliah ini?")) {
                try {
                    pesertaDao.hapus(b.getMahasiswaId(), b.getMataKuliahId());
                    refreshPeserta();
                } catch (Exception e) { AlertUtil.error(e.getMessage()); }
            }
        });
    }

    @Override
    protected void reload(String keyword) throws Exception {
        List<Dosen> dosenList = dosenDao.search(null);
        cbDosen.setItems(FXCollections.observableArrayList(dosenList));
        cbDosen.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Dosen d) { return d == null ? "" : d.getNama() + " (" + d.getNidn() + ")"; }
            public Dosen fromString(String s) { return null; }
        });
        cbMahasiswa.setItems(FXCollections.observableArrayList(mahasiswaDao.search(null)));
        cbMahasiswa.setConverter(new javafx.util.StringConverter<>() {
            public String toString(Mahasiswa m) { return m == null ? "" : m.getNim() + " - " + m.getNama(); }
            public Mahasiswa fromString(String s) { return null; }
        });
        items.setAll(dao.search(keyword));
        refreshPeserta();
    }

    private void refreshPeserta() {
        try {
            MataKuliah mk = selected();
            if (mk == null) { tablePeserta.getItems().clear(); lblInfoPeserta.setText("Pilih mata kuliah untuk melihat pesertanya."); return; }
            tablePeserta.getItems().setAll(pesertaDao.findByMataKuliah(mk.getId()));
            lblInfoPeserta.setText("Peserta " + mk.getKode() + ": " + tablePeserta.getItems().size() + " mahasiswa");
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    protected void onNew() { txtId.clear(); txtKode.clear(); txtNama.clear(); txtSks.clear(); cbDosen.setValue(null); }

    @FXML
    protected void onEdit() {
        MataKuliah m = selected();
        if (m == null) { AlertUtil.warn("Pilih baris mata kuliah."); return; }
        txtId.setText(String.valueOf(m.getId()));
        txtKode.setText(m.getKode());
        txtNama.setText(m.getNama());
        txtSks.setText(String.valueOf(m.getSks()));
        try {
            for (Dosen d : cbDosen.getItems()) if (d.getId() == m.getDosenId()) { cbDosen.setValue(d); break; }
        } catch (Exception ignore) {}
        refreshPeserta();
    }

    @FXML
    protected void tableClicked() { refreshPeserta(); }

    @FXML
    protected void onSave() {
        String kode = txtKode.getText().trim();
        String nama = txtNama.getText().trim();
        String err = ValidationUtil.firstError(
                ValidationUtil.required(kode, "Kode MK"),
                ValidationUtil.required(nama, "Nama MK"),
                ValidationUtil.required(txtSks.getText().trim(), "SKS"),
                cbDosen.getValue() == null ? "Pilih dosen pengampu." : null);
        if (err != null) { AlertUtil.error(err); return; }
        int sks;
        try { sks = Integer.parseInt(txtSks.getText().trim()); }
        catch (NumberFormatException e) { AlertUtil.error("SKS harus angka."); return; }
        if (sks < 1 || sks > 6) { AlertUtil.error("SKS harus antara 1 dan 6."); return; }
        try {
            MataKuliah m = new MataKuliah();
            m.setKode(kode); m.setNama(nama); m.setSks(sks); m.setDosenId(cbDosen.getValue().getId());
            if (txtId.getText().isBlank()) { dao.insert(m); AlertUtil.success("Mata kuliah dibuat."); }
            else { m.setId(Integer.parseInt(txtId.getText().trim())); dao.update(m); AlertUtil.success("Mata kuliah diperbarui."); }
            onNew(); onSearch();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))
                AlertUtil.error("Kode mata kuliah sudah ada: " + kode);
            else AlertUtil.error("Gagal menyimpan: " + e.getMessage());
        }
    }

    @FXML
    protected void onDelete() {
        MataKuliah m = selected();
        if (m == null) { AlertUtil.warn("Pilih baris yang akan dihapus."); return; }
        if (!AlertUtil.confirm("Hapus Data", "Hapus mata kuliah " + m.getKode() + " - " + m.getNama()
                + "?\nSemua sesi & riwayat absensi mata kuliah ini ikut terhapus.")) return;
        try { dao.delete(m.getId()); AlertUtil.success("Mata kuliah dihapus."); onSearch(); }
        catch (Exception e) { AlertUtil.error("Gagal menghapus: " + e.getMessage()); }
    }

    /** Tambah mahasiswa sebagai peserta mata kuliah terpilih. */
    @FXML
    protected void onAddPeserta() {
        MataKuliah mk = selected();
        Mahasiswa mhs = cbMahasiswa.getValue();
        if (mk == null) { AlertUtil.warn("Pilih mata kuliah dulu."); return; }
        if (mhs == null) { AlertUtil.warn("Pilih mahasiswa yang akan ditambahkan."); return; }
        try {
            if (pesertaDao.isTerdaftar(mhs.getId(), mk.getId())) {
                AlertUtil.warn(mhs.getNama() + " sudah terdaftar pada " + mk.getKode() + ".");
                return;
            }
            pesertaDao.tambah(mhs.getId(), mk.getId());
            AlertUtil.success("Peserta ditambahkan.");
            refreshPeserta();
        } catch (Exception e) { AlertUtil.error("Gagal: " + e.getMessage()); }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(table, "mata_kuliah"); }
}
