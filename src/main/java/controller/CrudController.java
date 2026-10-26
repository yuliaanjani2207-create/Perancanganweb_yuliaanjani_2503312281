package controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;

/** Basis bersama untuk halaman CRUD (pencarian + tabel + muat ulang). */
public abstract class CrudController<T> implements MainController.Refreshable {

    @FXML protected TextField txtSearch;
    @FXML protected TableView<T> table;

    protected final ObservableList<T> items = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        table.setItems(items);
        table.setPlaceholder(new javafx.scene.control.Label("Tidak ada data."));
    }

    /** Muat data dari DAO sesuai kata kunci pencarian. */
    protected abstract void reload(String keyword) throws Exception;

    @FXML
    protected void onSearch() {
        try {
            reload(txtSearch.getText());
        } catch (Exception e) {
            AlertUtil.error("Kesalahan database: " + e.getMessage());
        }
    }

    @Override
    public void onShow() { onSearch(); }

    protected T selected() { return table.getSelectionModel().getSelectedItem(); }
}
