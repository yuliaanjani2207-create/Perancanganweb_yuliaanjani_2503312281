package controller;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.Optional;

/** Notifikasi keberhasilan/kesalahan & konfirmasi seragam (dipakai semua controller). */
public final class AlertUtil {
    private AlertUtil() {}

    public static void info(String title, String msg) { show(Alert.AlertType.INFORMATION, title, msg); }
    public static void success(String msg) { show(Alert.AlertType.INFORMATION, "Berhasil", msg); }
    public static void error(String msg) { show(Alert.AlertType.ERROR, "Gagal", msg); }
    public static void warn(String msg) { show(Alert.AlertType.WARNING, "Peringatan", msg); }

    public static boolean confirm(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.CONFIRMATION, msg, ButtonType.YES, ButtonType.NO);
        a.setTitle(title);
        a.setHeaderText(null);
        Optional<ButtonType> r = a.showAndWait();
        return r.isPresent() && r.get() == ButtonType.YES;
    }

    private static void show(Alert.AlertType type, String title, String msg) {
        Alert a = new Alert(type, msg, ButtonType.OK);
        a.setTitle(title);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
