package controller;

import dao.MataKuliahDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableView;
import model.MataKuliah;
import service.RekapService;

/** Halaman rekap kehadiran (persentase per mahasiswa & per pertemuan) + ekspor CSV. */
public class RekapController implements MainController.Refreshable {

    private final RekapService rekap = new RekapService();
    private final MataKuliahDao mkDao = new MataKuliahDao();

    @FXML private TableView<RekapService.RekapRow> tableMahasiswa;
    @FXML private TableView<RekapService.SesiRow> tableSesi;
    @FXML private ComboBox<MataKuliah> cbMatkul;
    @FXML private DatePicker dpAwal, dpAkhir;

    @Override
    public void onShow() {
        try {
            cbMatkul.setItems(FXCollections.observableArrayList(mkDao.search(null)));
            cbMatkul.setConverter(new javafx.util.StringConverter<>() {
                public String toString(MataKuliah m) { return m == null ? "(semua mata kuliah)" : m.getKode() + " - " + m.getNama(); }
                public MataKuliah fromString(String s) { return null; }
            });
            onCalculate();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onCalculate() {
        try {
            Integer mkId = cbMatkul.getValue() == null ? null : cbMatkul.getValue().getId();
            String a = dpAwal.getValue() == null ? null : dpAwal.getValue().toString();
            String b = dpAkhir.getValue() == null ? null : dpAkhir.getValue().toString();
            tableMahasiswa.getItems().setAll(rekap.rekapMahasiswa(mkId));
            tableSesi.getItems().setAll(rekap.rekapPerSesi(mkId, a, b));
        } catch (Exception e) { AlertUtil.error("Gagal menghitung rekap: " + e.getMessage()); }
    }

    @FXML
    protected void onExportCsv() {
        ExportHelper.exportTableToCsv(tableMahasiswa, "rekap_kehadiran_mahasiswa");
    }

    @FXML
    protected void onExportSesiCsv() {
        ExportHelper.exportTableToCsv(tableSesi, "rekap_per_pertemuan");
    }
}
