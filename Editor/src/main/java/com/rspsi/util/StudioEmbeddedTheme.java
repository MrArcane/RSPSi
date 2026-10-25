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
        content.setStyle(content.getStyle() + "; -fx-background-color: #101319;");
        replaceLegacyInlineColors(content);
    }

    private static void replaceLegacyInlineColors(Node node) {
        String style = node.getStyle();
        if (style != null && !style.isBlank()) {
            style = style
                .replace("#4a4a4a", "#13171e")
                .replace("#4A4A4A", "#13171e")
                .replace("#3c3c3c", "#171c25")
                .replace("#3C3C3C", "#171c25")
                .replace("#3d3d3d", "#171c25")
                .replace("#3D3D3D", "#171c25")
                .replace("#2b2b2b", "#13171e")
                .replace("#2B2B2B", "#13171e")
                .replace("#1d1d1d", "#101319")
                .replace("#1D1D1D", "#101319")
                .replace("#f0544c", "#4f8cff")
                .replace("#F0544C", "#4f8cff");
            node.setStyle(style);
        }
        if (node instanceof Parent parent) {
            parent.getChildrenUnmodifiable().forEach(StudioEmbeddedTheme::replaceLegacyInlineColors);
        }
    }
}
