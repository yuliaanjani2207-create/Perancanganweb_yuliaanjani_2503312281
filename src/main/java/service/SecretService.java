package service;

import dao.SettingDao;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * SecretService — komponen tepercaya (trusted component) penyimpan kunci rahasia HMAC.
 *
 * Kunci TIDAK PERNAH dimasukkan ke dalam QR Code dan TIDAK pernah dikirim
 * ke perangkat pemindai. Kunci disimpan di tabel app_setting pada database
 * lokal (dibuat acak 256-bit saat pertama kali aplikasi dijalankan).
 *
 * Catatan keamanan untuk penggunaan nyata: pada aplikasi desktop lokal,
 * database berada di mesin yang sama dengan aplikasi sehingga proteksi
 * setingkat HSM tidak mungkin. Untuk produksi, pindahkan validasi token
 * ke server terpusat dan simpan secret hanya di sisi server.
 */
public class SecretService {
    private static final String KEY_NAME = "qr_secret_key";
    private static final SettingDao settings = new SettingDao();
    private static final SecureRandom random = new SecureRandom();

    /** Ambil (atau buat sekali) kunci rahasia HMAC. */
    public static synchronized String getSecret() {
        String secret = settings.get(KEY_NAME, null);
        if (secret == null || secret.isBlank()) {
            byte[] buf = new byte[32]; // 256 bit
            random.nextBytes(buf);
            secret = Base64.getEncoder().encodeToString(buf);
            try {
                settings.set(KEY_NAME, secret);
            } catch (Exception e) {
                // Bila gagal menyimpan, pakai secret sementara selama proses berjalan
            }
        }
        return secret;
    }
}
