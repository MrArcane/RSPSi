package com.jagex.entity.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jagex.draw.raster.GameRasterizer;
import org.junit.jupiter.api.Test;

class MeshTest {
    @Test
    void nonFlatCopyToleratesAnAlreadyShadedModel() {
        Mesh source = new Mesh();
        assertDoesNotThrow(() -> new Mesh(false, true, source));
    }

    @Test
    void rasterizerGrowsForRevision241ModelsAndDenseDepthBuckets() {
        GameRasterizer rasterizer = new GameRasterizer();
        rasterizer.ensureModelCapacity(12_000, 15_000, 9_000);

        assertTrue(rasterizer.vertexScreenX.length >= 12_000);
        assertTrue(rasterizer.cullFaces.length >= 15_000);
        assertTrue(rasterizer.depthListIndices.length >= 9_000);
        assertTrue(rasterizer.anIntArrayArray1674[0].length >= 15_000);

        for (int face = 0; face < 700; face++) rasterizer.addDepthFace(8_500, face);
        assertEquals(700, rasterizer.depthListIndices[8_500]);
        assertEquals(699, rasterizer.faceList[8_500][699]);
    }
}
