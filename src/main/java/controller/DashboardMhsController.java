package controller;

import dao.AbsensiDao;
import dao.MataKuliahDao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.User;
import service.RekapService;
import util.Session;

/** Dashboard mahasiswa: profil singkat + persentase kehadiran per mata kuliah. */
public class DashboardMhsController implements MainController.Refreshable {

    @FXML private Label lblWelcome, lblNim, lblProdi, lblSummary;

    @Override
    public void onShow() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) return;
        try {
            var mhs = new dao.MahasiswaDao().findById(u.getMahasiswaId());
            lblWelcome.setText("Halo, " + mhs.getNama());
            lblNim.setText("NIM: " + mhs.getNim() + "   Prodi: " + mhs.getProdi());
            var rekapList = new RekapService().rekapUntukMahasiswa(mhs.getId());
            StringBuilder sb = new StringBuilder();
            int totalSesi = 0, totalHadir = 0;
            for (var r : rekapList) {
                sb.append(String.format("%s: %.1f%% (%d/%d sesi)%n", r.matkul(), r.persen(), r.hadir() + r.terlambat(), r.totalSesi()));
                totalSesi += r.totalSesi(); totalHadir += r.hadir() + r.terlambat();
            }
            if (sb.isEmpty()) sb.append("Belum ada sesi yang selesai tercatat.");
            else sb.insert(0, String.format("Total kehadiran gabungan: %d/%d sesi\n\n", totalHadir, totalSesi));
            lblSummary.setText(sb.toString());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }
}
