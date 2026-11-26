package com.rspsi.game;

import com.jagex.Client;
import com.jagex.map.SceneGraph;
import com.rspsi.options.KeyboardState;
import java.nio.file.Path;
import java.util.Arrays;
import javafx.application.Platform;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.Parent;
import javafx.scene.input.KeyEvent;

/** Standalone rendering session. Does not initialize the Editor module or its windows. */
public final class StudioMapEngine implements AutoCloseable {
    private final Client client;
    private final CanvasPane viewport;
    private boolean closed;

    public StudioMapEngine(Path cache, Path plugins) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("FX thread required");
        System.setProperty("rspsi.plugins.path", plugins.toAbsolutePath().toString());
        client = Client.initialize(800, 600);
        client.loadCache(cache.toAbsolutePath());
        if (client.getCache() == null) {
            client.exit();
            throw new IllegalStateException("Map cache could not be opened");
        }
        viewport = new CanvasPane(client.getGameCanvas());
        viewport.setMinSize(1, 1);
        client.visible = true;
        viewport.setOnMousePressed(e -> viewport.requestFocus());
        viewport.addEventHandler(KeyEvent.KEY_PRESSED, e -> camera(e, 1));
        viewport.addEventHandler(KeyEvent.KEY_RELEASED, e -> camera(e, 0));
        viewport.focusedProperty().addListener((obs, old, focused) -> {
            if (!focused) {
                KeyboardState.reset();
                SceneGraph.setMouseIsDown(false);
                Arrays.fill(client.keyStatuses, 0);
            }
        });
    }

    public Parent viewport() { return viewport; }
    public ObservableBooleanValue ready() { return Client.gameLoaded; }

    public void loadRegion(int regionX, int regionY, int width, int height) {
        if (closed || !ready().get()) throw new IllegalStateException("Engine not ready");
        if (regionX < 0 || regionY < 0 || width < 1 || height < 1
                || width > 8 || height > 8 || regionX + width > 256 || regionY + height > 256)
            throw new IllegalArgumentException("Invalid region bounds");
        Client.runLater.add(() -> client.loadCoordinates(regionX * 64, regionY * 64, width, height));
    }

    private void camera(KeyEvent event, int value) {
        int key = switch (event.getCode()) {
            case W -> 'w'; case A -> 'a'; case S -> 's'; case D -> 'd';
            case UP -> 'k'; case DOWN -> 'l'; case LEFT -> 2; case RIGHT -> 1;
            case PAGE_UP -> 'o'; case PAGE_DOWN -> 'p'; default -> -1;
        };
        if (key >= 0) { client.keyStatuses[key] = value; event.consume(); }
    }

    @Override public void close() {
        if (closed) return;
        closed = true;
        client.exit();
    }
}
