package controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import model.User;
import service.AuthService;
import util.Session;

/** Controller halaman login (halaman 1). */
public class LoginController {

    @FXML private TextField txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Hyperlink linkDemo;

    private final AuthService authService = new AuthService();

    @FXML
    public void initialize() {
        // Enter pada kolom password memicu login
        txtPassword.setOnAction(e -> onLogin());
    }

    @FXML
    public void onLogin() {
        String u = txtUsername.getText() == null ? "" : txtUsername.getText().trim();
        String p = txtPassword.getText() == null ? "" : txtPassword.getText();
        if (u.isEmpty() || p.isEmpty()) {
            AlertUtil.error("Username dan password wajib diisi.");
            return;
        }
        try {
            User user = authService.login(u, p);
            if (user == null) {
                AlertUtil.error("Username atau password SALAH.");
                return;
            }
            Session.login(user);
            openMain();
        } catch (Exception ex) {
            AlertUtil.error("Kesalahan sistem: " + ex.getMessage());
        }
    }

    @FXML
    public void onDemo() {
        AlertUtil.info("Akun Pengujian", """
            Akun demo yang tersedia (password semuanya: 123456):

            DOSEN      : 198001012005011001
            MAHASISWA  : 211101001 (Budi Santoso)
                       : 211101002 (Ani Wijaya), 211101003, 211101004
                       : 211101005 (Eko - hanya ambil Basis Data)""");
    }

    private void openMain() throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/MainView.fxml"));
        Parent root = loader.load();
        Scene scene = ((Node) txtUsername).getScene();
        Stage stage = (Stage) scene.getWindow();
        stage.setScene(new Scene(root, 1100, 720));
        stage.getScene().getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        MainController mc = loader.getController();
        mc.initForUser();
    }

    @FXML
    public void onExit() { Platform.exit(); }
}
