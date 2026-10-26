package service;

/** Hasil pemindaian token QR beserta pesan untuk pengguna. */
public record ScanResult(boolean success, String message, String nim, String nama, String sesiLabel) {
    public static ScanResult ok(String nim, String nama, String sesiLabel) {
        return new ScanResult(true, "Absensi BERHASIL dicatat.", nim, nama, sesiLabel);
    }
    public static ScanResult fail(String msg) {
        return new ScanResult(false, msg, null, null, null);
    }
}
