package controller;

import dao.MahasiswaDao;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import model.Mahasiswa;
import model.User;
import util.Session;

/** Halaman profil mahasiswa (lihat saja). */
public class ProfilMhsController implements MainController.Refreshable {

    @FXML private Label lblNim, lblNama, lblProdi, lblEmail, lblTelepon, lblStatus;

    @Override
    public void onShow() {
        User u = Session.getCurrentUser();
        if (u == null || u.getMahasiswaId() == null) return;
        try {
            Mahasiswa m = new MahasiswaDao().findById(u.getMahasiswaId());
            if (m == null) return;
            lblNim.setText(m.getNim());
            lblNama.setText(m.getNama());
            lblProdi.setText(m.getProdi());
            lblEmail.setText(m.getEmail() == null ? "-" : m.getEmail());
            lblTelepon.setText(m.getTelepon() == null ? "-" : m.getTelepon());
            lblStatus.setText(m.getStatus());
        } catch (Exception e) { AlertUtil.error(e.getMessage()); }
    }
}
