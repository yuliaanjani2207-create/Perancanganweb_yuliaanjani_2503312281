package controller;

import dao.AbsensiDao;
import dao.MataKuliahDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;
import model.Absensi;
import model.MataKuliah;

import java.time.LocalDate;

/** Halaman riwayat absensi dosen dengan filter matkul/tanggal/kata kunci. */
public class RiwayatController implements MainController.Refreshable {

    private final AbsensiDao dao = new AbsensiDao();
    private final MataKuliahDao mkDao = new MataKuliahDao();

    @FXML private javafx.scene.control.TableView<Absensi> table;
    @FXML private ComboBox<MataKuliah> cbMatkul;
    @FXML private DatePicker dpAwal, dpAkhir;
    @FXML private TextField txtKeyword;

    @FXML
    public void initialize() {
        table.setPlaceholder(new javafx.scene.control.Label("Tidak ada data riwayat."));
    }

    @Override
    public void onShow() {
        try {
            cbMatkul.setItems(FXCollections.observableArrayList(mkDao.search(null)));
            cbMatkul.setConverter(new javafx.util.StringConverter<>() {
                public String toString(MataKuliah m) { return m == null ? "(semua mata kuliah)" : m.getKode() + " - " + m.getNama(); }
                public MataKuliah fromString(String s) { return null; }
            });
            cbMatkul.setValue(null);
            onFilter();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onFilter() {
        try {
            LocalDate a = dpAwal.getValue(), b = dpAkhir.getValue();
            table.getItems().setAll(dao.riwayat(
                    cbMatkul.getValue() == null ? null : cbMatkul.getValue().getId(),
                    a == null ? null : a.toString(),
                    b == null ? null : b.toString(),
                    txtKeyword.getText()));
        } catch (Exception e) { AlertUtil.error("Gagal memuat riwayat: " + e.getMessage()); }
    }

    @FXML
    public void onReset() {
        cbMatkul.setValue(null); dpAwal.setValue(null); dpAkhir.setValue(null); txtKeyword.clear();
        onFilter();
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(table, "riwayat_absensi"); }
}
