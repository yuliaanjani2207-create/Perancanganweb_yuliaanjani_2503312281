package service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.io.File;
import java.util.Map;

/** Konversi payload teks menjadi gambar QR Code (JavaFX Image / file PNG). */
public final class QrImageUtil {
    private QrImageUtil() {}

    public static WritableImage toFxImage(String payload, int size) throws Exception {
        BitMatrix m = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size,
                Map.of(EncodeHintType.MARGIN, 1));
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();
        for (int x = 0; x < size; x++)
            for (int y = 0; y < size; y++)
                pw.setArgb(x, y, m.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
        return img;
    }

    public static void savePng(String payload, File out, int size) throws Exception {
        BitMatrix m = new MultiFormatWriter().encode(payload, BarcodeFormat.QR_CODE, size, size,
                Map.of(EncodeHintType.MARGIN, 1));
        MatrixToImageWriter.writeToPath(m, "PNG", out.toPath());
    }

    /** Untuk mode pengujian: memuat kembali gambar QR dari file. */
    public static Image loadImage(File f) {
        return new Image(f.toURI().toString());
    }
}
