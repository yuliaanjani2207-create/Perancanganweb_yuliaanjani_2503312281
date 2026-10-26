package controller;

import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import util.CsvExporter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Ekspor isi TableView apa pun ke CSV dengan pemilih lokasi file. */
public final class ExportHelper {
    private ExportHelper() {}

    public static void exportTableToCsv(TableView<?> table, String suggestedName) {
        try {
            FileChooser fc = new FileChooser();
            fc.setTitle("Ekspor CSV");
            fc.setInitialFileName(suggestedName + ".csv");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV", "*.csv"));
            Window w = table.getScene().getWindow();
            File f = fc.showSaveDialog(w);
            if (f == null) return;

            List<String[]> rows = new ArrayList<>();
            String[] headers = new String[table.getColumns().size()];
            for (int c = 0; c < table.getColumns().size(); c++) {
                TableColumn<?, ?> col = table.getColumns().get(c);
                headers[c] = col.getText();
            }
            for (Object item : table.getItems()) {
                String[] r = new String[headers.length];
                for (int c = 0; c < headers.length; c++) {
                    Object val = ((TableColumn<Object, ?>) table.getColumns().get(c)).getCellData(item);
                    r[c] = val == null ? "" : val.toString();
                }
                rows.add(r);
            }
            CsvExporter.export(f, headers, rows);
            AlertUtil.success("Laporan diekspor ke:\n" + f.getAbsolutePath());
        } catch (Exception e) {
            AlertUtil.error("Ekspor gagal: " + e.getMessage());
        }
    }
}
