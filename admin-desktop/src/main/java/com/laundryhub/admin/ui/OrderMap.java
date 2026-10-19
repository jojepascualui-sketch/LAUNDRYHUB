package com.laundryhub.admin.ui;

import com.laundryhub.admin.AppConfig;
import com.laundryhub.admin.api.dto.PinnedLocation;

import javafx.scene.layout.StackPane;
import javafx.scene.shape.Rectangle;
import javafx.scene.web.WebView;

/**
 * The A2 "Pinned location" map: a WebView showing the backend's map page ({@code /api/maps/order-pin}),
 * which draws the Google map with the design's orange pin. The Maps key stays on the server
 * ({@code MAPS_JS_KEY}); without it the page shows the plain map frame with a note.
 */
public final class OrderMap {

    private static final double RADIUS = 16;

    // OOP: Encapsulation — the WebView is created on the first pin.
    private final StackPane frame;
    private final AppConfig config;
    private WebView web;

    /**
     * @param frame  the rounded map frame
     * @param config where the backend is
     */
    public OrderMap(StackPane frame, AppConfig config) {
        this.frame = frame;
        this.config = config;
        Rectangle clip = new Rectangle();
        clip.setArcWidth(RADIUS * 2);
        clip.setArcHeight(RADIUS * 2);
        clip.widthProperty().bind(frame.widthProperty());
        clip.heightProperty().bind(frame.heightProperty());
        frame.setClip(clip);
    }

    /**
     * @param pin the customer's pin, or {@code null}
     */
    public void show(PinnedLocation pin) {
        if (pin == null) {
            return;
        }
        if (web == null) {
            web = new WebView();
            web.setContextMenuEnabled(false);
            frame.getChildren().setAll(web);
        }
        web.getEngine().load(config.mapPageUrl(pin.latitude().toPlainString(), pin.longitude().toPlainString()));
    }
}
