package com.jagex.entity.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.jagex.entity.model.VertexNormal;
import org.junit.jupiter.api.Test;

class MeshTest {
    @Test
    void nonFlatCopyToleratesAnAlreadyShadedModel() {
        Mesh source = new Mesh();
        assertDoesNotThrow(() -> new Mesh(false, true, source));
    }

    @Test
    void nonFlatCopyToleratesTruncatedNormalArrays() {
        Mesh source = new Mesh();
        source.vertexCount = 2;
        source.setNormals(new VertexNormal[] {new VertexNormal()});
        source.normals = new VertexNormal[] {new VertexNormal()};

        assertDoesNotThrow(() -> new Mesh(false, true, source));
    }
}
