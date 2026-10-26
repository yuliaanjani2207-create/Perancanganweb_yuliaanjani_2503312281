package service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * QrTokenService — inti mekanisme QR dinamis.
 *
 * Format payload QR:  ABSV1|<sesiId>|<nonce>|<expiryEpochSec>|<hmacBase64url>
 *   - nonce   : 16 byte acak dari SecureRandom (anti-tebak, tiap rotasi baru)
 *   - expiry  : batas waktu berlaku token (epoch detik)
 *   - hmac    : HMAC-SHA256 atas "ABSV1|sesi|nonce|expiry" memakai kunci rahasia.
 *     Kunci rahasia TIDAK ada di QR; penyerang tanpa akses ke komponen tepercaya
 *     tidak dapat memalsukan QR untuk sesi mana pun, termasuk menebak ID sesi.
 *
 * Saat pemindaian, sistem menghitung ulang HMAC dan membandingkan secara
 * konstan-waktu (constant-time), lalu memeriksa hash token terhadap DB.
 */
public class QrTokenService {

    private static final String PREFIX = "ABSV1";
    private static final SecureRandom random = new SecureRandom();

    /** Membuat payload QR baru untuk suatu sesi dengan masa berlaku ttlDetik. */
    public static String generatePayload(int sesiId, int ttlDetik) {
        byte[] nonceBytes = new byte[16];
        random.nextBytes(nonceBytes);
        String nonce = base64Url(nonceBytes);
        long expiry = Instant.now().plus(ttlDetik, ChronoUnit.SECONDS).getEpochSecond();
        String core = PREFIX + "|" + sesiId + "|" + nonce + "|" + expiry;
        String sig = hmacB64(core);
        return core + "|" + sig;
    }

    /** Hasil parsing payload QR. */
    public record Parsed(int sesiId, String nonce, long expiryEpochSec, String signature, boolean formatValid) {}

    public static Parsed parse(String payload) {
        if (payload == null) return new Parsed(0, "", 0, "", false);
        String[] p = payload.trim().split("\\|");
        if (p.length != 5 || !PREFIX.equals(p[0])) return new Parsed(0, "", 0, "", false);
        try {
            return new Parsed(Integer.parseInt(p[1]), p[2], Long.parseLong(p[3]), p[4], true);
        } catch (NumberFormatException e) {
            return new Parsed(0, "", 0, "", false);
        }
    }

    /** Verifikasi tanda tangan HMAC pada payload. */
    public static boolean verifySignature(Parsed parsed) {
        if (!parsed.formatValid()) return false;
        String core = PREFIX + "|" + parsed.sesiId() + "|" + parsed.nonce() + "|" + parsed.expiryEpochSec();
        String expected = hmacB64(core);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                                      parsed.signature().getBytes(StandardCharsets.UTF_8));
    }

    /** Hash SHA-256 hex dari payload penuh — inilah yang disimpan di token_qr. */
    public static String tokenHash(String payload) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] h = md.digest(payload.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : h) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isExpired(Parsed parsed) {
        return Instant.now().getEpochSecond() > parsed.expiryEpochSec();
    }

    /** Sisa detik sampai kedaluwarsa (min 0). */
    public static long secondsRemaining(Parsed parsed) {
        long r = parsed.expiryEpochSec() - Instant.now().getEpochSecond();
        return Math.max(0, r);
    }

    private static String hmacB64(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SecretService.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return base64Url(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException("Gagal menghitung HMAC", e);
        }
    }

    private static String base64Url(byte[] bytes) {
        return java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
