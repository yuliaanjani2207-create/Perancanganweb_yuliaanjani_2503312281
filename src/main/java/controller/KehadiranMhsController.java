package controller;

import dao.AbsensiDao;
import dao.MataKuliahDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TableView;
import model.Absensi;
import model.MataKuliah;
import model.User;
import service.RekapService;
import util.Session;

/** Status kehadiran mahasiswa: riwayat absen + persentase per matkul. */
public class KehadiranMhsController implements MainController.Refreshable {

    @FXML private TableView<Absensi> tableRiwayat;
    @FXML private TableView<RekapService.RekapRow> tablePersen;
    @FXML private ComboBox<MataKuliah> cbMatkul;

    @Override
    public void onShow() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) return;
        try {
            var mkList = new MataKuliahDao().findByMahasiswa(u.getMahasiswaId());
            cbMatkul.setItems(FXCollections.observableArrayList(mkList));
            cbMatkul.setConverter(new javafx.util.StringConverter<>() {
                public String toString(MataKuliah m) { return m == null ? "(semua mata kuliah)" : m.getKode() + " - " + m.getNama(); }
                public MataKuliah fromString(String s) { return null; }
            });
            cbMatkul.setValue(null);
            refresh();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onFilterChanged() { refresh(); }

    private void refresh() {
        User u = Session.getCurrentUser();
        try {
            Integer mkId = cbMatkul.getValue() == null ? null : cbMatkul.getValue().getId();
            tableRiwayat.getItems().setAll(new AbsensiDao().findByMahasiswa(u.getMahasiswaId(), mkId));
            tablePersen.getItems().setAll(new RekapService().rekapUntukMahasiswa(u.getMahasiswaId()));
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(tableRiwayat, "riwayat_kehadiran_saya"); }
}
