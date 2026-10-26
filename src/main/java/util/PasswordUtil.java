package util;

import org.mindrot.jbcrypt.BCrypt;

/** Utilitas hashing password. Password TIDAK PERNAH disimpan sebagai teks biasa. */
public final class PasswordUtil {
    private PasswordUtil() {}

    public static String hash(String plain) {
        return BCrypt.hashpw(plain, BCrypt.gensalt(10));
    }

    public static boolean verify(String plain, String hash) {
        try {
            return BCrypt.checkpw(plain, hash);
        } catch (IllegalArgumentException e) {
            return false; // hash tidak valid
        }
    }
}
