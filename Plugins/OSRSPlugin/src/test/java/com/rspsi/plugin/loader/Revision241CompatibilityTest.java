package com.rspsi.plugin.loader;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.displee.cache.CacheLibrary;
import com.jagex.cache.loader.floor.FloorType;
import com.jagex.entity.model.MeshLoader;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class Revision241CompatibilityTest {
    private static final Path CACHE_PATH = Path.of("..", "..", "..", "..", "cache-241")
        .toAbsolutePath().normalize();

    @Test
    void loadsRevision241DefinitionsMapsTexturesAndModels() {
        CacheLibrary cache = new CacheLibrary(CACHE_PATH.toString(), false, null);
        try {
            var configs = cache.index(2);

            var floors = new FloorDefinitionLoaderOSRS();
            assertDoesNotThrow(() -> floors.initUnderlays(configs.archive(1)));
            assertDoesNotThrow(() -> floors.initOverlays(configs.archive(4)));
            assertTrue(floors.getSize(FloorType.UNDERLAY) > 100);
            assertTrue(floors.getSize(FloorType.OVERLAY) > 100);

            var animations = new AnimationDefinitionLoaderOSRS();
            assertDoesNotThrow(() -> animations.init(configs.archive(12)));
            assertTrue(animations.count() > 1_000, "Unexpected animation count: " + animations.count());

            var graphics = new GraphicLoaderOSRS();
            assertDoesNotThrow(() -> graphics.init(configs.archive(13)));
            assertTrue(graphics.count() > 500, "Unexpected graphic count: " + graphics.count());

            var varbits = new VarbitLoaderOSRS();
            assertDoesNotThrow(() -> varbits.init(configs.archive(14)));
            assertTrue(varbits.count() > 1_000, "Unexpected varbit count: " + varbits.count());

            var areas = new RSAreaLoaderOSRS();
            assertDoesNotThrow(() -> areas.init(configs.archive(35)));
            assertTrue(areas.count() > 100, "Unexpected area count: " + areas.count());

            var textures = new TextureLoaderOSRS();
            assertDoesNotThrow(() -> textures.init(cache.index(9).archive(0), cache.index(8)));
            assertTrue(textures.count() > 10, "Unexpected texture count: " + textures.count());

            verifyKnownMapGroup();
            verifyRepresentativeModels();
        } finally {
            cache.close();
        }
    }

    private static void verifyKnownMapGroup() {
        var openRune = dev.openrune.filesystem.Cache.Companion.load(CACHE_PATH);
        try {
            byte[] terrain = openRune.data(5, 12338, 0, null);
            byte[] objects = openRune.data(5, 12338, 1, null);
            assertNotNull(terrain, "Revision 241 terrain file is missing");
            assertNotNull(objects, "Revision 241 object file is missing");
            assertTrue(terrain.length > 0, "Revision 241 terrain file is empty");
            assertTrue(objects.length > 0, "Revision 241 object file is empty");
        } finally {
            openRune.close();
        }
    }

    private static void verifyRepresentativeModels() {
        int decoded = 0;
        RuntimeException firstFailure = null;
        int firstFailureId = -1;
        var openRune = dev.openrune.filesystem.Cache.Companion.load(CACHE_PATH);
        try {
            for (int modelId = 0; modelId < 10_000 && decoded < 100; modelId++) {
                byte[] data = openRune.data(7, modelId, 0, null);
                if (data == null || data.length == 0) continue;
                try {
                    assertNotNull(MeshLoader.load(data));
                    decoded++;
                } catch (RuntimeException exception) {
                    if (firstFailure == null) {
                        firstFailure = exception;
                        firstFailureId = modelId;
                    }
                }
            }
        } finally {
            openRune.close();
        }
        String firstFailureSummary = firstFailure == null ? "none"
            : firstFailureId + ": " + firstFailure.getClass().getSimpleName() + ": " + firstFailure.getMessage();
        assertTrue(decoded >= 100, "Expected at least 100 revision 241 models to decode, got " + decoded
            + "; first failure=" + firstFailureSummary);
    }
}
