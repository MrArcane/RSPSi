package com.rspsi.plugin.loader;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.displee.cache.CacheLibrary;
import com.jagex.cache.def.ObjectDefinition;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class ObjectDefinitionLoaderOSRSTest {
    @Test
    void decodesTheCompleteRevision241ObjectArchive() {
        Path cachePath = Path.of("..", "..", "..", "..", "cache-241")
            .toAbsolutePath().normalize();
        CacheLibrary cache = new CacheLibrary(cachePath.toString(), false, null);
        try {
            var archive = cache.index(2).archive(6);
            assertNotNull(archive);
            var loader = new ObjectDefinitionLoaderOSRS();
            loader.init(archive);
            assertTrue(loader.count() > 50_000, "Expected the complete OSRS object archive");

            boolean foundMultipleActions = false;
            boolean foundModels = false;
            for (int id = 0; id < loader.count(); id++) {
                ObjectDefinition definition = loader.forId(id);
                if (definition == null) continue;
                if (definition.getModelIds() != null && definition.getModelIds().length > 0)
                    foundModels = true;
                if (definition.getInteractions() != null
                        && Arrays.stream(definition.getInteractions()).filter(action -> action != null).count() > 1)
                    foundMultipleActions = true;
            }
            assertTrue(foundModels, "Expected decoded object models");
            assertTrue(foundMultipleActions, "Object actions must not overwrite one another");
        } finally {
            cache.close();
        }
    }
}
