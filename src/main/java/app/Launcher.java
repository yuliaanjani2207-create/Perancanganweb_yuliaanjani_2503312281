package app;

import database.DatabaseManager;
import database.SeedData;
import javafx.application.Application;

/**
 * Launcher terpisah agar aplikasi JavaFX dapat dijalankan dari classpath biasa
 * (tanpa modul), misalnya:  java -cp "target/classes;target/libs/*" app.Launcher
 * Kelas ini TIDAK meng-extend Application sehingga JVM tidak memeriksa
 * ketersediaan toolkit JavaFX saat memuat kelas ini.
 */
public class Launcher {
    public static void main(String[] args) throws Exception {
        // Mode pengujian tanpa GUI: java -cp "target/classes;target/libs/*" app.Launcher --selftest
        for (String a : args) {
            if ("--selftest".equals(a)) { SelfTest.run(); return; }
        }
        DatabaseManager.initFromConfig();
        DatabaseManager.initializeSchema();
        SeedData.seedIfEmpty();
        Application.launch(MainApp.class, args);
    }
}
