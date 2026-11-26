package com.rspsi.game;

import com.jagex.Client;
import com.jagex.map.SceneGraph;
import com.rspsi.options.KeyboardState;
import com.rspsi.options.Options;
import com.rspsi.core.misc.ToolType;
import com.rspsi.core.misc.BrushType;
import com.rspsi.datasets.ObjectDataset;
import com.jagex.cache.loader.object.ObjectDefinitionLoader;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
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
        client.getGameCanvas().addEventHandler(MouseEvent.ANY, this::mouse);
        client.getGameCanvas().addEventHandler(ScrollEvent.SCROLL, e -> {
            for (int i = 0; i < 3; i++) {
                if (e.getDeltaY() >= 0) client.scrollIn(); else client.scrollOut();
            }
            e.consume();
        });
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

    public void tool(String name) { Options.currentTool.set(ToolType.valueOf(name)); }
    public void brush(int size, String shape) {
        if (size < 1 || size > 64) throw new IllegalArgumentException("Brush size");
        Options.brushSize.set(size);
        Options.brushType.set(BrushType.valueOf(shape));
    }
    public void plane(int plane) {
        if (plane < 0 || plane > 3) throw new IllegalArgumentException("Plane");
        Options.currentHeight.set(plane);
    }
    public void height(int height, boolean absolute) {
        Options.tileHeightLevel.set(height);
        Options.absoluteHeightProperty.set(absolute);
    }
    public void paint(int underlay, int overlay, int shape) {
        Options.underlayPaintId.set(underlay);
        Options.overlayPaintId.set(overlay);
        Options.overlayPaintShapeId.set(shape);
    }
    public String objectName(int id) {
        var def = ObjectDefinitionLoader.lookup(id);
        return def == null ? "" : def.getName();
    }
    public void object(int id, int type, int rotation) {
        var def = ObjectDefinitionLoader.lookup(id);
        if (def == null) throw new IllegalArgumentException("Unknown object");
        Options.currentObject.set(new ObjectDataset(id, type, def.getName()));
        Options.rotation.set(rotation);
        tool("SPAWN_OBJECT");
    }
    public void undo() { Client.runLater.add(SceneGraph::undo); }
    public void redo() { Client.runLater.add(SceneGraph::redo); }

    private void mouse(MouseEvent event) {
        int x = (int) event.getX(), y = (int) event.getY();
        if (event.getEventType() == MouseEvent.MOUSE_PRESSED) {
            viewport.requestFocus();
            client.pressedX = x; client.pressedY = y;
            client.mouseClickTime = System.currentTimeMillis();
            if (event.getButton() == MouseButton.MIDDLE || event.getButton() == MouseButton.SECONDARY) {
                client.mouseWheelX = x; client.mouseWheelY = y; client.mouseWheelDown = true;
            } else {
                client.metaModifierPressed = 1; client.metaModifierHeld = 1;
                SceneGraph.setMousePos(y, x);
                SceneGraph.setMouseIsDown(event.isPrimaryButtonDown());
            }
        } else if (event.getEventType() == MouseEvent.MOUSE_DRAGGED && client.mouseWheelDown) {
            client.mouseWheelDragged(client.mouseWheelX - x, y - client.mouseWheelY);
            client.mouseWheelX = x; client.mouseWheelY = y;
        } else if (event.getEventType() == MouseEvent.MOUSE_MOVED || event.getEventType() == MouseEvent.MOUSE_DRAGGED) {
            client.mouseEventX = x; client.mouseEventY = y;
            SceneGraph.setMousePos(y, x);
        } else if (event.getEventType() == MouseEvent.MOUSE_RELEASED) {
            client.metaModifierHeld = 0; client.mouseWheelDown = false;
            SceneGraph.setMouseIsDown(false);
        } else if (event.getEventType() == MouseEvent.MOUSE_EXITED) {
            client.mouseEventX = -1; client.mouseEventY = -1;
        }
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
