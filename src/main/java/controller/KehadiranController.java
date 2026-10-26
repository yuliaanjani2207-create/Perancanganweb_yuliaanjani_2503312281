package controller;

import dao.AbsensiDao;
import dao.PesertaDao;
import dao.SesiDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import model.Absensi;
import model.PesertaMataKuliah;
import model.SesiPerkuliahan;

/** Halaman daftar kehadiran satu sesi: hadir vs belum hadir + koreksi manual. */
public class KehadiranController implements MainController.Refreshable {

    private final SesiDao sesiDao = new SesiDao();
    private final AbsensiDao absensiDao = new AbsensiDao();
    private final PesertaDao pesertaDao = new PesertaDao();

    @FXML private ComboBox<SesiPerkuliahan> cbSesi;
    @FXML private javafx.scene.control.TableView<Absensi> tableHadir;
    @FXML private javafx.scene.control.TableView<PesertaMataKuliah> tableBelum;
    @FXML private Label lblInfo;

    private final javafx.collections.ObservableList<Absensi> hadirItems = FXCollections.observableArrayList();
    private final javafx.collections.ObservableList<PesertaMataKuliah> belumItems = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        tableHadir.setItems(hadirItems);
        tableBelum.setItems(belumItems);
        // Klik kanan / tombol hapus untuk koreksi data absen yang salah
        tableHadir.setRowFactory(tv -> {
            javafx.scene.control.TableRow<Absensi> row = new javafx.scene.control.TableRow<>();
            row.setOnMouseClicked(ev -> {
                if (ev.getClickCount() == 2 && !row.isEmpty()
                        && AlertUtil.confirm("Hapus Catatan Absensi",
                        "Hapus catatan kehadiran " + row.getItem().getNamaMahasiswa() + " pada sesi ini?\n"
                        + "Mahasiswa dapat mencatat ulang dengan QR baru.")) {
                    try {
                        absensiDao.delete(row.getItem().getId());
                        refresh();
                    } catch (Exception e) { AlertUtil.error(e.getMessage()); }
                }
            });
            return row;
        });
    }

    @Override
    public void onShow() {
        try {
            cbSesi.setItems(FXCollections.observableArrayList(sesiDao.filter(null, null, null, null)));
            cbSesi.setConverter(new javafx.util.StringConverter<>() {
                public String toString(SesiPerkuliahan s) {
                    return s == null ? "" : s.getKodeMatkul() + " | " + s.getLabelSingkat() + " [" + s.getStatus() + "]";
                }
                public SesiPerkuliahan fromString(String v) { return null; }
            });
            if (!cbSesi.getItems().isEmpty()) cbSesi.getSelectionModel().selectFirst();
            refresh();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onSesiChanged() { refresh(); }

    private void refresh() {
        SesiPerkuliahan s = cbSesi.getValue();
        if (s == null) { hadirItems.clear(); belumItems.clear(); lblInfo.setText("Tidak ada sesi."); return; }
        try {
            var hadir = absensiDao.findBySesi(s.getId());
            var peserta = pesertaDao.findByMataKuliah(s.getMataKuliahId());
            hadirItems.setAll(hadir);
            belumItems.setAll(peserta.stream()
                    .filter(p -> hadir.stream().noneMatch(a -> a.getMahasiswaId() == p.getMahasiswaId()))
                    .toList());
            lblInfo.setText(s.getNamaMatkul() + " — Pertemuan ke-" + s.getPertemuanKe() + " (" + s.getTanggal() + ")"
                    + "   Hadir: " + hadir.size() + "/" + peserta.size());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    protected void onExportCsv() { ExportHelper.exportTableToCsv(tableHadir, "daftar_kehadiran"); }
}
