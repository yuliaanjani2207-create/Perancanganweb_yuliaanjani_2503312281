package controller;

import dao.DosenDao;
import javafx.fxml.FXML;
import javafx.scene.control.TextField;
import model.Dosen;
import service.AuthService;
import util.ValidationUtil;

/** Halaman pengelolaan data dosen. */
public class DosenController extends CrudController<Dosen> {

    private final DosenDao dao = new DosenDao();
    private final AuthService auth = new AuthService();

    @FXML private TextField txtId, txtNidn, txtNama, txtProdi, txtEmail;

    @Override
    protected void reload(String keyword) throws Exception { items.setAll(dao.search(keyword)); }

    @FXML
    protected void onNew() { txtId.clear(); txtNidn.clear(); txtNama.clear(); txtProdi.clear(); txtEmail.clear(); }

    @FXML
    protected void onEdit() {
        Dosen d = selected();
        if (d == null) { AlertUtil.warn("Pilih baris dosen terlebih dahulu."); return; }
        txtId.setText(String.valueOf(d.getId()));
        txtNidn.setText(d.getNidn());
        txtNama.setText(d.getNama());
        txtProdi.setText(d.getProdi());
        txtEmail.setText(d.getEmail());
    }

    @FXML
    protected void onSave() {
        String nidn = txtNidn.getText().trim();
        String nama = txtNama.getText().trim();
        String err = ValidationUtil.firstError(
                ValidationUtil.required(nidn, "NIDN"),
                ValidationUtil.pattern(nidn, "\\d{8,20}", "NIDN harus berupa angka (8-20 digit)"),
                ValidationUtil.required(nama, "Nama"),
                ValidationUtil.emailOptional(txtEmail.getText().trim()));
        if (err != null) { AlertUtil.error(err); return; }
        try {
            Dosen d = new Dosen();
            d.setNidn(nidn); d.setNama(nama);
            d.setProdi(txtProdi.getText().trim()); d.setEmail(txtEmail.getText().trim());
            if (txtId.getText().isBlank()) {
                int id = dao.insert(d);
                d.setId(id);
                auth.createAccountForDosen(d, "123456");
                AlertUtil.success("Dosen ditambahkan. Akun login: username " + nidn + ", password 123456.");
            } else {
                d.setId(Integer.parseInt(txtId.getText().trim()));
                dao.update(d);
                AlertUtil.success("Data dosen diperbarui.");
            }
            onNew(); onSearch();
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("unique"))
                AlertUtil.error("NIDN sudah terdaftar: " + nidn);
            else AlertUtil.error("Gagal menyimpan: " + e.getMessage());
        }
    }

    @FXML
    protected void onDelete() {
        Dosen d = selected();
        if (d == null) { AlertUtil.warn("Pilih baris yang akan dihapus."); return; }
        if (!AlertUtil.confirm("Hapus Data", "Hapus dosen " + d.getNama() + "?")) return;
        try { dao.delete(d.getId()); AlertUtil.success("Dosen dihapus."); onSearch(); }
        catch (Exception e) {
            // FK RESTRICT pada mata_kuliah mencegah penghapusan bila masih mengampu
            AlertUtil.error("Gagal menghapus (mungkin masih mengampu mata kuliah): " + e.getMessage());
        }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(table, "dosen"); }
}
