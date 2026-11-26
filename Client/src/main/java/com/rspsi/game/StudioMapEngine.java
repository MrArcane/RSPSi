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
    private volatile boolean closed;
    private final Path cachePath;
    private boolean cacheWriteFailed;

    public StudioMapEngine(Path cache, Path plugins) {
        if (!Platform.isFxApplicationThread()) throw new IllegalStateException("FX thread required");
        System.setProperty("rspsi.plugins.path", plugins.toAbsolutePath().toString());
        cachePath = cache.toAbsolutePath().normalize();
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
                client.mouseWheelDown = false;
                client.metaModifierHeld = 0;
                SceneGraph.setMouseIsDown(false);
                Arrays.fill(client.keyStatuses, 0);
            }
        });
    }

    public Parent viewport() { return viewport; }
    public Parent minimap() {
        var pane = new CanvasPane(client.mapCanvas);
        client.mapCanvas.addEventHandler(MouseEvent.MOUSE_CLICKED,e -> {
            if (e.getButton() != MouseButton.PRIMARY) return;
            double fx = e.getX()/client.mapCanvas.getWidth();
            double fy = e.getY()/client.mapCanvas.getHeight();
            if (fx < 0 || fy < 0 || fx >= 1 || fy >= 1) return;
            Client.runLater.add(() -> {
                var chunk = client.getCurrentChunk();
                if (chunk != null) client.moveCamera(chunk.offsetX + (int)(fx*64),
                    chunk.offsetY + 63 - (int)(fy*64));
            });
            viewport.requestFocus();
        });
        return pane;
    }

    public void cameraPreset(String preset) {
        Client.runLater.add(() -> {
            if (client.chunks.isEmpty()) return;
            switch (preset) {
                case "center" -> {
                    var chunk = client.chunks.get(0);
                    client.moveCamera(chunk.offsetX+32,chunk.offsetY+32);
                    client.cameraYaw = 0;
                    client.cameraRoll = 128;
                }
                case "overhead" -> client.cameraRoll = 383;
                case "perspective" -> client.cameraRoll = 128;
                default -> throw new IllegalArgumentException("Unknown camera preset");
            }
        });
    }

    /** Explicit save for existing cache regions only. A full cache backup precedes mutation. */
    public java.util.concurrent.CompletableFuture<String> saveCache() {
        var future = new java.util.concurrent.CompletableFuture<String>();
        Client.runLater.add(() -> {
            Path backup = null;
            boolean writing = false;
            int x = client.xCameraPos, y = client.yCameraPos;
            try {
                if (closed || cacheWriteFailed || !ready().get() || client.chunks.isEmpty())
                    throw new IllegalStateException("Cache save unavailable");
                record Region(int id, byte[] terrain, byte[] objects) {}
                var regions = new java.util.ArrayList<Region>();
                var ids = new java.util.HashSet<Integer>();
                var cache = client.getCache();
                for (var chunk : client.chunks) {
                    int id = (chunk.regionX << 8) | chunk.regionY;
                    if (!chunk.hasLoaded() || chunk.regionX < 0 || chunk.regionX > 255
                        || chunk.regionY < 0 || chunk.regionY > 255
                        || chunk.tileMapId != id || chunk.objectMapId != id || !ids.add(id))
                        throw new IllegalStateException("Only existing, fully loaded cache regions can be saved");
                    if (cache.readMap(id,0,id) == null || cache.readMap(id,1,id) == null)
                        throw new IllegalStateException("Missing original region " + id);
                    client.xCameraPos = (chunk.offsetX + 32) * 128;
                    client.yCameraPos = (chunk.offsetY + 32) * 128;
                    regions.add(new Region(id,chunk.mapRegion.save_terrain_block(chunk),client.sceneGraph.saveObjects(chunk)));
                }
                backup = java.nio.file.Files.createTempDirectory(cachePath.getParent(),"map-cache-backup-");
                try (var paths = java.nio.file.Files.walk(cachePath)) {
                    for (Path source : paths.toList()) {
                        if (java.nio.file.Files.isSymbolicLink(source)) throw new java.io.IOException("Cache contains symbolic link");
                        Path target = backup.resolve(cachePath.relativize(source));
                        if (java.nio.file.Files.isDirectory(source)) java.nio.file.Files.createDirectories(target);
                        else {
                            java.nio.file.Files.copy(source,target);
                            if (java.nio.file.Files.mismatch(source,target) != -1)
                                throw new java.io.IOException("Backup verification failed: " + source);
                        }
                    }
                }
                writing = true;
                for (Region region : regions) {
                    cache.writeMap(region.id(),0,region.terrain());
                    cache.writeMap(region.id(),1,region.objects());
                }
                cache.save();
                for (Region region : regions) {
                    if (!Arrays.equals(region.terrain(),cache.readMap(region.id(),0,region.id()))
                        || !Arrays.equals(region.objects(),cache.readMap(region.id(),1,region.id())))
                        throw new java.io.IOException("Region verification failed: " + region.id());
                }
                javafx.application.Platform.runLater(() -> Options.unsavedChanges.set(false));
                future.complete("Saved " + regions.size() + " regions. Backup: " + backup);
            } catch (Exception ex) {
                if (writing) cacheWriteFailed = true;
                future.completeExceptionally(new IllegalStateException(ex.getMessage()
                    + (backup == null ? "" : "; backup: " + backup)
                    + (writing ? "; further saves blocked; close Studio before restoring backup" : ""),ex));
            } finally { client.xCameraPos = x; client.yCameraPos = y; }
        });
        return future;
    }
    public ObservableBooleanValue ready() { return Client.gameLoaded; }

    public void loadRegion(int regionX, int regionY, int width, int height) {
        if (closed || !ready().get()) throw new IllegalStateException("Engine not ready");
        if (regionX < 0 || regionY < 0 || width < 1 || height < 1
                || width > 8 || height > 8 || regionX + width > 256 || regionY + height > 256)
            throw new IllegalArgumentException("Invalid region bounds");
        Client.runLater.add(() -> client.loadCoordinates(regionX * 64, regionY * 64, width, height));
    }

    public javafx.beans.value.ObservableBooleanValue dirty() { return Options.unsavedChanges; }

    public void newMap(int width, int height, int elevation) {
        if (closed || !ready().get()) throw new IllegalStateException("Engine not ready");
        if (width < 1 || width > 8 || height < 1 || height > 8)
            throw new IllegalArgumentException("Invalid map dimensions");
        Client.runLater.add(() -> {
            int[][] heights = new int[width * 64 + 1][height * 64 + 1];
            for (int[] row : heights) Arrays.fill(row, elevation);
            client.loadNew(width, height, heights);
            Options.unsavedChanges.set(true);
        });
    }

    public java.util.concurrent.CompletableFuture<Integer> importPack(Path source) {
        var future = new java.util.concurrent.CompletableFuture<Integer>();
        if (closed || !ready().get()) {
            future.completeExceptionally(new IllegalStateException("Engine not ready"));
            return future;
        }
        Client.runLater.add(() -> {
            try {
                if (java.nio.file.Files.size(source) > 64L * 1024 * 1024)
                    throw new IllegalArgumentException("Map pack exceeds 64 MiB");
                var chunks = com.jagex.util.MultiMapEncoder.decode(java.nio.file.Files.readAllBytes(source));
                if (chunks.isEmpty() || chunks.size() > 64)
                    throw new IllegalArgumentException("Invalid pack region count");
                var occupied = new java.util.HashSet<Long>();
                for (var chunk : chunks) {
                    if (chunk.offsetX < 0 || chunk.offsetY < 0 || chunk.offsetX >= 512 || chunk.offsetY >= 512
                        || !occupied.add(((long) chunk.offsetX << 32) | (chunk.offsetY & 0xffffffffL)))
                        throw new IllegalArgumentException("Invalid or duplicate pack position");
                }
                client.loadChunks(chunks);
                future.complete(chunks.size());
            } catch (Exception ex) { future.completeExceptionally(ex); }
        });
        return future;
    }

    public void view(String name, boolean enabled) {
        switch (name) {
            case "objects" -> Options.showObjects.set(enabled);
            case "grid" -> Options.showBorders.set(enabled);
            case "heights" -> Options.showTileHeightNumbers.set(enabled);
            case "underlays" -> Options.showUnderlayNumbers.set(enabled);
            case "overlays" -> Options.showOverlayNumbers.set(enabled);
            case "tileInfo" -> Options.showTileInformation.set(enabled);
            case "allPlanes" -> Options.allHeightsVisible.set(enabled);
            default -> throw new IllegalArgumentException("Unknown view option: " + name);
        }
    }

    public String regionStatus() {
        if (client.chunks.isEmpty()) return "No map loaded";
        return client.chunks.size() + " region(s) loaded";
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
    public void flags(int mask) {
        if (mask < 0 || mask > 31) throw new IllegalArgumentException("Invalid tile flags");
        Options.tileFlags.set(new com.jagex.util.BitFlag((byte) mask));
        tool("SET_FLAGS");
    }

    public java.util.Map<Integer,Integer> materials(boolean overlay) {
        var result = new java.util.LinkedHashMap<Integer,Integer>();
        int highest = overlay ? com.jagex.cache.loader.floor.FloorDefinitionLoader.getHighestOverlayId()
            : com.jagex.cache.loader.floor.FloorDefinitionLoader.getHighestUnderlayId();
        for (int id = 0; id <= Math.min(highest, 65534); id++) {
            var floor = overlay ? com.jagex.cache.loader.floor.FloorDefinitionLoader.getOverlay(id)
                : com.jagex.cache.loader.floor.FloorDefinitionLoader.getUnderlay(id);
            if (floor != null) result.put(id + 1, floor.getRgb() & 0xffffff);
        }
        return result;
    }

    public void paint(int underlay, int overlay, int shape) {
        Options.underlayPaintId.set(underlay);
        Options.overlayPaintId.set(overlay);
        Options.overlayPaintShapeId.set(shape);
    }
    public java.util.List<String> searchObjects(String query, int limit) {
        if (!ready().get()) throw new IllegalStateException("Engine not ready");
        String term = query.trim().toLowerCase(java.util.Locale.ROOT);
        var results = new java.util.ArrayList<String>();
        for (int id = 0; id < ObjectDefinitionLoader.getCount() && results.size() < Math.min(500, limit); id++) {
            var def = ObjectDefinitionLoader.lookup(id);
            if (def == null || def.getName() == null) continue;
            if (Integer.toString(id).equals(term) || def.getName().toLowerCase(java.util.Locale.ROOT).contains(term))
                results.add(id + ": " + def.getName());
        }
        return results;
    }

    public java.util.concurrent.CompletableFuture<Integer> exportPack(Path destination) {
        var future = new java.util.concurrent.CompletableFuture<Integer>();
        Client.runLater.add(() -> {
            try {
                if (closed || client.chunks.isEmpty()) throw new IllegalStateException("No loaded regions");
                var chunks = new java.util.ArrayList<>(client.chunks);
                byte[] bytes = com.jagex.util.MultiMapEncoder.encode(chunks);
                var verified = com.jagex.util.MultiMapEncoder.decode(bytes);
                if (verified.size() != chunks.size()) throw new IllegalStateException("Pack region count mismatch");
                Path target = destination.toAbsolutePath();
                Path temporary = java.nio.file.Files.createTempFile(target.getParent(), "map-", ".tmp");
                try {
                    java.nio.file.Files.write(temporary, bytes);
                    if (!Arrays.equals(bytes, java.nio.file.Files.readAllBytes(temporary)))
                        throw new java.io.IOException("Pack verification failed");
                    java.nio.file.Files.move(temporary, target, java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE);
                } finally { java.nio.file.Files.deleteIfExists(temporary); }
                future.complete(chunks.size());
            } catch (Exception ex) { future.completeExceptionally(ex); }
        });
        return future;
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
            if (event.getEventType() == MouseEvent.MOUSE_DRAGGED) client.metaModifierHeld = 3;
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
