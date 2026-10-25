package com.jagex.entity.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class MeshTest {
    @Test
    void nonFlatCopyToleratesAnAlreadyShadedModel() {
        Mesh source = new Mesh();
        assertDoesNotThrow(() -> new Mesh(false, true, source));
    }
}
