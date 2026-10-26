package controller;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.util.function.Consumer;

/** Helper kecil memilih file gambar untuk mode pengujian pemindaian. */
public final class FileChooserBridge {
    private FileChooserBridge() {}

    public static void chooseImage(Window owner, Consumer<File> onChosen) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Pilih gambar QR Code (mode pengujian)");
        fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Gambar", "*.png", "*.jpg", "*.jpeg", "*.bmp"));
        File f = fc.showOpenDialog(owner);
        if (f != null) onChosen.accept(f);
    }
}
