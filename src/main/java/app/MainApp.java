package app;

import database.DatabaseManager;
import database.SeedData;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import service.BackupService;

/** Kelas utama JavaFX — titik masuk aplikasi desktop absensi mahasiswa. */
public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        DatabaseManager.initFromConfig();
        DatabaseManager.initializeSchema();
        SeedData.seedIfEmpty();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/LoginView.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 960, 640);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setTitle("Aplikasi Absensi Mahasiswa — QR Code Dinamis");
        stage.setMinWidth(880);
        stage.setMinHeight(560);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        BackupService.autoBackupIfEnabled(); // pencadangan otomatis saat aplikasi ditutup
    }

    /** Titik masuk untuk pengujian tanpa GUI: `java -cp ... app.MainApp --selftest`
     *  menjalankan seluruh skenario uji logika bisnis lalu keluar. */
    public static void main(String[] args) {
        for (String a : args) {
            if ("--selftest".equals(a)) { SelfTest.run(); return; }
        }
        launch(args);
    }
}

