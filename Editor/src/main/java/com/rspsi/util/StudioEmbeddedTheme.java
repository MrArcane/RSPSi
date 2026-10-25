package com.rspsi.util;

import javafx.scene.Parent;
import javafx.scene.Node;

/** Applies the Chaotix Studio visual language to RSPSi windows opened from embedded mode. */
public final class StudioEmbeddedTheme {
    private StudioEmbeddedTheme() {}

    public static void apply(Parent content) {
        if (!Boolean.getBoolean("rspsi.embedded") || content == null) return;
        if (!content.getStyleClass().contains("studio-embedded")) {
            content.getStyleClass().add("studio-embedded");
        }
        String stylesheet = StudioEmbeddedTheme.class
            .getResource("/css/studio_embedded.css").toExternalForm();
        if (!content.getStylesheets().contains(stylesheet)) {
            content.getStylesheets().add(stylesheet);
        }
        content.setStyle(content.getStyle() + "; -fx-background-color: #1b1b1b;");
        replaceLegacyInlineColors(content);
    }

    private static void replaceLegacyInlineColors(Node node) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            style = style
                .replace("#4a4a4a", "#1f1f1f")
                .replace("#4A4A4A", "#1f1f1f")
                .replace("#3c3c3c", "#242424")
                .replace("#3C3C3C", "#242424")
                .replace("#3d3d3d", "#242424")
                .replace("#3D3D3D", "#242424")
                .replace("#1d1d1d", "#1b1b1b")
                .replace("#1D1D1D", "#1b1b1b")
                .replace("#f0544c", "#dcb567")
                .replace("#F0544C", "#dcb567");
            node.setStyle(style);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(StudioEmbeddedTheme::replaceLegacyInlineColors);
        }
    }
}
