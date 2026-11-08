package com.rspsi.dialogs;

import com.jagex.Cache;
import com.rspsi.util.FXDialogs;
import java.io.IOException;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

/** Small, lossless editor for the display-name field in index 19/details. */
public final class WorldMapAreaDialog extends Stage {
    private final Cache cache;
    private final ComboBox<Cache.WorldMapArea> areas = new ComboBox<>();
    private final TextField displayName = new TextField();

    public WorldMapAreaDialog(Stage owner, Cache cache) {
        this.cache = cache;
        if (owner != null && owner.isShowing()) {
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);
        } else {
            // Studio embeds RSPSi without showing its host Stage. An owned window attached
            // to that hidden Stage never becomes visible on Windows.
            initModality(Modality.NONE);
        }
        setTitle("World Map Areas");

        GridPane content = new GridPane();
        content.setPadding(new Insets(16));
        content.setHgap(10);
        content.setVgap(10);
        areas.setPrefWidth(430);
        displayName.setPrefWidth(430);
        Button save = new Button("Save Display Name");
        Button close = new Button("Close");
        content.addRow(0, new Label("Map area"), areas);
        content.addRow(1, new Label("Displayed name"), displayName);
        content.addRow(2, save, close);
        setScene(new javafx.scene.Scene(content));

        areas.valueProperty().addListener((ignored, oldArea, area) -> {
            displayName.setText(area == null ? "" : area.displayName());
        });
        save.setOnAction(event -> saveSelected());
        close.setOnAction(event -> hide());
        setOnShowing(event -> reload());
        sizeToScene();
    }

    private void reload() {
        try {
            Integer selected = areas.getValue() == null ? null : areas.getValue().fileId();
            areas.setItems(FXCollections.observableArrayList(cache.worldMapAreas()));
            if (selected != null) areas.getItems().stream()
                .filter(area -> area.fileId() == selected).findFirst().ifPresent(areas::setValue);
            if (areas.getValue() == null && !areas.getItems().isEmpty()) areas.getSelectionModel().selectFirst();
        } catch (IOException exception) {
            FXDialogs.showError(this, "Unable to load world-map areas", exception.getMessage());
        }
    }

    private void saveSelected() {
        Cache.WorldMapArea area = areas.getValue();
        if (area == null) return;
        try {
            cache.renameWorldMapArea(area.fileId(), displayName.getText().trim());
            reload();
        } catch (IOException | IllegalArgumentException exception) {
            FXDialogs.showError(this, "Unable to save world-map area", exception.getMessage());
        }
    }
}
