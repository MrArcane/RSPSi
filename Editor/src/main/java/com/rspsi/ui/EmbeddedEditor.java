package com.rspsi.ui;

import com.rspsi.options.Config;
import com.rspsi.util.Settings;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

/** Integration entry point for hosting the complete editor inside another JavaFX scene. */
public final class EmbeddedEditor {
    private EmbeddedEditor() {}

    public static Parent create(Path cache, Path activePlugins) throws Exception {
        Files.createDirectories(Path.of(System.getProperty("user.home"), ".rspsi"));
        Settings.loadSettings();
        String cachePath = cache.toAbsolutePath().normalize() + File.separator;
        Config.cacheLocation.set(cachePath);
        Settings.properties.put("cacheLocation", cachePath);
        System.setProperty("rspsi.plugins.path", activePlugins.toAbsolutePath().normalize().toString());
        System.setProperty("rspsi.embedded", "true");

        MainWindow window = new MainWindow();
        Stage host = new Stage();
        window.start(host);
        Scene scene = host.getScene();
        Parent content = scene.getRoot();
        content.getStyleClass().add("studio-embedded");
        content.getStylesheets().add(EmbeddedEditor.class
            .getResource("/css/studio_embedded.css").toExternalForm());
        content.setStyle("-fx-background-color: #1c1e21;");
        // MainWindow's standalone hide handler exits its client and closes the cache. Studio
        // only hides this temporary stage to transfer the scene into JFXPanel, so detach that
        // lifecycle hook first and let Studio own shutdown.
        host.setOnHiding(null);
        host.hide();
        scene.setRoot(new javafx.scene.layout.Pane());
        host.setScene(null);
        return content;
    }
}
