package com.jagex.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jagex.chunk.Chunk;
import java.nio.ByteBuffer;
import java.util.List;
import org.junit.jupiter.api.Test;

class MultiMapEncoderTest {

    @Test
    void shallowPackRoundTripsEveryRegion() {
        Chunk first = chunk(100, 0, 0, new byte[] {1, 2}, new byte[] {3, 4, 5});
        Chunk second = chunk(200, 64, 128, new byte[] {6}, new byte[] {7, 8});

        List<Chunk> decoded = MultiMapEncoder.decode(MultiMapEncoder.encodeShallow(List.of(first, second)));

        assertEquals(2, decoded.size());
        assertEquals(64, decoded.get(1).offsetX);
        assertEquals(128, decoded.get(1).offsetY);
        assertArrayEquals(second.objectMapData, decoded.get(1).objectMapData);
        assertArrayEquals(second.tileMapData, decoded.get(1).tileMapData);
    }

    @Test
    void rejectsPackWhoseHeaderClaimsMissingRegions() {
        byte[] truncated = ByteBuffer.allocate(4).putInt(1).array();
        assertThrows(IllegalArgumentException.class, () -> MultiMapEncoder.decode(truncated));
    }

    private static Chunk chunk(int id, int offsetX, int offsetY, byte[] objects, byte[] terrain) {
        Chunk chunk = new Chunk(id);
        chunk.objectMapId = id;
        chunk.tileMapId = id;
        chunk.offsetX = offsetX;
        chunk.offsetY = offsetY;
        chunk.objectMapData = objects;
        chunk.tileMapData = terrain;
        return chunk;
    }
}
