package controller;

import dao.MataKuliahDao;
import dao.PesertaDao;
import dao.SesiDao;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import model.MataKuliah;
import model.SesiPerkuliahan;
import model.User;
import util.Session;

/** Mahasiswa: daftar mata kuliahnya + jadwal sesi tiap mata kuliah. */
public class MatkulMhsController implements MainController.Refreshable {

    @FXML private ComboBox<MataKuliah> cbMatkul;
    @FXML private Label lblInfo;

    @Override
    public void onShow() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) return;
        try {
            var list = new MataKuliahDao().findByMahasiswa(u.getMahasiswaId());
            cbMatkul.setItems(FXCollections.observableArrayList(list));
            cbMatkul.setConverter(new javafx.util.StringConverter<>() {
                public String toString(MataKuliah m) { return m == null ? "(pilih mata kuliah)" : m.getKode() + " - " + m.getNama() + " (" + m.getSks() + " SKS)"; }
                public MataKuliah fromString(String s) { return null; }
            });
            if (!list.isEmpty()) cbMatkul.getSelectionModel().selectFirst();
            onChanged();
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }

    @FXML
    public void onChanged() {
        MataKuliah m = cbMatkul.getValue();
        if (m == null) { lblInfo.setText("Belum terdaftar pada mata kuliah apa pun."); return; }
        try {
            StringBuilder sb = new StringBuilder("Jadwal & sesi " + m.getKode() + " — " + m.getNama() + "\n\n");
            for (SesiPerkuliahan s : new SesiDao().findByMataKuliah(m.getId())) {
                sb.append(String.format("Pertemuan ke-%d | %s %s-%s | %s%s%n",
                        s.getPertemuanKe(), s.getTanggal(), s.getJamMulai(), s.getJamSelesai(), s.getStatus(),
                        (s.getTopik() == null || s.getTopik().isBlank()) ? "" : " | " + s.getTopik()));
            }
            lblInfo.setText(sb.toString());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }
}
