package com.laundryhub.admin.ui;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import javafx.scene.shape.SVGPath;

/** Outline icons copied from the design's SVGs (24 × 24 view box, stroke 2). */
public final class Icons {

    /** Washing machine logo (rect 4,2 16×20 r3, drum r5, buttons). */
    public static final String LOGO = "M7 2h10a3 3 0 0 1 3 3v14a3 3 0 0 1 -3 3H7a3 3 0 0 1 -3 -3V5a3 3 0 0 1 3 -3z"
            + "M7 13a5 5 0 1 0 10 0a5 5 0 1 0 -10 0M8 6h0.01M11 6h2";
    /** Notifications bell. */
    public static final String BELL = "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3 -2 3 -9M10.3 21a1.94 1.94 0 0 0 3.4 0";

    private static final double VIEW_BOX = 24;

    private Icons() {
    }

    /**
     * @param path       SVG path in a 24 × 24 box
     * @param size       icon size in px
     * @param styleClass stroke style, e.g. "icon-stroke"
     * @return the icon, exactly {@code size} × {@code size}
     */
    public static Node icon(String path, double size, String styleClass) {
        SVGPath svg = new SVGPath();
        svg.setContent(path);
        svg.getStyleClass().add(styleClass);
        Pane box = new Pane(svg);
        box.setMinSize(VIEW_BOX, VIEW_BOX);
        box.setPrefSize(VIEW_BOX, VIEW_BOX);
        box.setMaxSize(VIEW_BOX, VIEW_BOX);
        double scale = size / VIEW_BOX;
        box.setScaleX(scale);
        box.setScaleY(scale);
        box.setTranslateX(-(VIEW_BOX - size) / 2);
        box.setTranslateY(-(VIEW_BOX - size) / 2);
        return new Group(box);
    }
}
