package controller;

import dao.*;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.SesiPerkuliahan;
import util.Session;

import java.time.LocalDate;

/** Dashboard dosen: kartu ringkasan + sesi hari ini. */
public class DashboardDosenController implements MainController.Refreshable, MainController.DosenScoped {

    @FXML private Label cardMahasiswa, cardMatkul, cardSesiAktif, cardHadirHariIni;
    @FXML private Label lblWelcome, lblSesiHariIni;

    @Override public void setDosenScope() {}

    @Override
    public void onShow() {
        try {
            var u = Session.getCurrentUser();
            lblWelcome.setText("Selamat datang, " + (u == null ? "" : u.getNamaLengkap()));
            long mhs = new MahasiswaDao().count();
            long mk = new MataKuliahDao().count();
            SesiDao sesiDao = new SesiDao();
            long aktif = sesiDao.countByStatus("AKTIF");
            long hadir = new AbsensiDao().countHariIni(LocalDate.now().toString());
            cardMahasiswa.setText(String.valueOf(mhs));
            cardMatkul.setText(String.valueOf(mk));
            cardSesiAktif.setText(String.valueOf(aktif));
            cardHadirHariIni.setText(String.valueOf(hadir));

            StringBuilder sb = new StringBuilder();
            for (SesiPerkuliahan s : sesiDao.filter(null, LocalDate.now().toString(), LocalDate.now().toString(), null)) {
                sb.append("• ").append(s.getLabelSingkat()).append("  [").append(s.getStatus()).append("]\n");
            }
            lblSesiHariIni.setText(sb.isEmpty() ? "Tidak ada sesi terjadwal hari ini." : sb.toString());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }
}
