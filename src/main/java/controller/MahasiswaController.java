package controller;

import dao.MahasiswaDao;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import model.Mahasiswa;
import service.AuthService;
import util.ValidationUtil;

/** Halaman pengelolaan data mahasiswa (tambah/ubah/hapus/cari). */
public class MahasiswaController extends CrudController<Mahasiswa> {

    private final MahasiswaDao dao = new MahasiswaDao();
    private final AuthService auth = new AuthService();

    @FXML private TextField txtId, txtNim, txtNama, txtProdi, txtEmail, txtTelepon;
    @FXML private ComboBox<String> cbStatus;

    @Override
    public void initialize() {
        super.initialize();
        // Opsi status mahasiswa (sesuai CHECK constraint pada tabel SQLite)
        cbStatus.setItems(javafx.collections.FXCollections.observableArrayList("AKTIF", "CUTI", "LULUS", "NONAKTIF"));
        cbStatus.setValue("AKTIF");
    }

    @Override
    protected void reload(String keyword) throws Exception {
        items.setAll(dao.search(keyword));
    }

    @FXML
    protected void onNew() {
        txtId.clear(); txtNim.clear(); txtNama.clear(); txtProdi.clear();
        txtEmail.clear(); txtTelepon.clear();
        cbStatus.setValue("AKTIF");
    }

    @FXML
    protected void onEdit() {
        Mahasiswa m = selected();
        if (m == null) { AlertUtil.warn("Pilih baris mahasiswa terlebih dahulu."); return; }
        txtId.setText(String.valueOf(m.getId()));
        txtNim.setText(m.getNim());
        txtNama.setText(m.getNama());
        txtProdi.setText(m.getProdi());
        txtEmail.setText(m.getEmail());
        txtTelepon.setText(m.getTelepon());
        cbStatus.setValue(m.getStatus());
    }

    @FXML
    protected void onSave() {
        String nim = txtNim.getText().trim();
        String nama = txtNama.getText().trim();
        String prodi = txtProdi.getText().trim();
        // Validasi formulir
        String err = ValidationUtil.firstError(
                ValidationUtil.required(nim, "NIM"),
                ValidationUtil.pattern(nim, "\\d{6,20}", "NIM harus berupa angka (6-20 digit)"),
                ValidationUtil.required(nama, "Nama"),
                ValidationUtil.required(prodi, "Program Studi"),
                ValidationUtil.emailOptional(txtEmail.getText().trim()));
        if (err != null) { AlertUtil.error(err); return; }

        try {
            Mahasiswa m = new Mahasiswa();
            m.setNim(nim); m.setNama(nama); m.setProdi(prodi);
            m.setEmail(txtEmail.getText().trim());
            m.setTelepon(txtTelepon.getText().trim());
            m.setStatus(cbStatus.getValue());
            if (txtId.getText().isBlank()) {
                int id = dao.insert(m);
                // buat akun login otomatis: username = NIM, password default 123456
                m.setId(id);
                auth.createAccountForMahasiswa(m, "123456");
                AlertUtil.success("Mahasiswa ditambahkan. Akun login dibuat: username " + nim + ", password 123456.");
            } else {
                m.setId(Integer.parseInt(txtId.getText().trim()));
                dao.update(m);
                AlertUtil.success("Data mahasiswa diperbarui.");
            }
            onNew();
            onSearch();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))
                AlertUtil.error("NIM sudah terdaftar: " + nim);
            else
                AlertUtil.error("Gagal menyimpan: " + e.getMessage());
        }
    }

    @FXML
    protected void onDelete() {
        Mahasiswa m = selected();
        if (m == null) { AlertUtil.warn("Pilih baris yang akan dihapus."); return; }
        if (!AlertUtil.confirm("Hapus Data",
                "Hapus mahasiswa " + m.getNim() + " - " + m.getNama() + "?\nRiwayat absensinya juga ikut terhapus.")) return;
        try {
            dao.delete(m.getId());
            AlertUtil.success("Mahasiswa dihapus.");
            onSearch();
        } catch (Exception e) {
            AlertUtil.error("Gagal menghapus: " + e.getMessage());
        }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(table, "mahasiswa"); }
}
