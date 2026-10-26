package util;

/** Validasi formulir sederhana; mengembalikan pesan error pertama atau null bila valid. */
public final class ValidationUtil {
    private ValidationUtil() {}

    public static String required(String value, String fieldName) {
        return (value == null || value.isBlank()) ? ("Field wajib diisi: " + fieldName) : null;
    }

    public static String pattern(String value, String regex, String message) {
        if (value == null || value.isBlank()) return null; // cek required terpisah
        return value.matches(regex) ? null : message;
    }

    public static String emailOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.matches("[\\w.+-]+@[\\w-]+\\.[\\w.-]+") ? null : "Format email tidak valid.";
    }

    /** Kembalikan pesan pertama yang bukan null. */
    public static String firstError(String... errors) {
        for (String e : errors) if (e != null) return e;
        return null;
    }
}
