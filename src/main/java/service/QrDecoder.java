package service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.RGBLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;

import javax.imageio.ImageIO;
import java.io.File;

/** Dekoder QR untuk mode pengujian: baca payload dari file gambar. */
public final class QrDecoder {
    private QrDecoder() {}

    public static String decodeFxImage(Image img) throws Exception {
        java.awt.image.BufferedImage awt = SwingFXUtils.fromFXImage(img, null);
        return decodeAwt(awt);
    }

    public static String decodeFile(File f) throws Exception {
        return decodeAwt(ImageIO.read(f));
    }

    /** Versi publik untuk frame kamera. */
    public static String decodeAwtPublic(java.awt.image.BufferedImage bi) throws Exception {
        return decodeAwt(bi);
    }

    private static String decodeAwt(java.awt.image.BufferedImage bi) throws Exception {
        if (bi == null) throw new IllegalArgumentException("Gambar tidak dapat dibaca.");
        int w = bi.getWidth(), h = bi.getHeight();
        int[] pixels = new int[w * h];
        bi.getRGB(0, 0, w, h, pixels, 0, w);
        RGBLuminanceSource src = new RGBLuminanceSource(w, h, pixels);
        BinaryBitmap bb = new BinaryBitmap(new HybridBinarizer(src));
        try {
            return new MultiFormatReader().decode(bb).getText();
        } catch (NotFoundException e) {
            throw new IllegalArgumentException("QR Code tidak terdeteksi pada gambar.");
        }
    }
}
