package util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Ekspor baris tabel menjadi file CSV (dengan BOM UTF-8 agar rapi dibuka di Excel). */
public final class CsvExporter {
    private CsvExporter() {}

    public static void export(File file, String[] headers, List<String[]> rows) throws Exception {
        try (BufferedWriter w = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8))) {
            w.write('\uFEFF'); // BOM UTF-8
            w.write(join(headers));
            w.newLine();
            for (String[] r : rows) {
                w.write(join(r));
                w.newLine();
            }
        }
    }

    private static String join(String[] cells) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.length; i++) {
            if (i > 0) sb.append(';');
            String v = cells[i] == null ? "" : cells[i];
            if (v.contains(";") || v.contains("\"") || v.contains("\n")) {
                v = '"' + v.replace("\"", "\"\"") + '"';
            }
            sb.append(v);
        }
        return sb.toString();
    }
}
