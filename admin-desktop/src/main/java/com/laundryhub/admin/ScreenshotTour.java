package com.laundryhub.admin;

import com.laundryhub.admin.ui.AppContext;
import com.laundryhub.admin.ui.Page;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;

import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.scene.image.PixelReader;
import javafx.scene.image.WritableImage;
import javafx.stage.Stage;
import javafx.util.Duration;

import javax.imageio.ImageIO;

/**
 * Design check (CLAUDE.md §1): with {@code LAUNDRYHUB_TOUR_DIR=<folder>} the app signs in with
 * {@code LAUNDRYHUB_TOUR_EMAIL} / {@code LAUNDRYHUB_TOUR_PASSWORD}, opens every page at the size of its
 * design PNG, saves a screenshot of each and exits. Normal runs never use this class.
 */
public final class ScreenshotTour {

    static final String FOLDER_VARIABLE = "LAUNDRYHUB_TOUR_DIR";
    private static final String EMAIL_VARIABLE = "LAUNDRYHUB_TOUR_EMAIL";
    private static final String PASSWORD_VARIABLE = "LAUNDRYHUB_TOUR_PASSWORD";
    /** Time for layout, fonts and CSS after the data arrived. */
    private static final Duration SETTLE = Duration.millis(800);
    /** Heights of the design PNGs (A1 and A2 are taller than the 900 px window). */
    private static final Map<Page, Double> HEIGHTS = Map.of(Page.DASHBOARD, 976.0, Page.ORDERS, 997.0,
            Page.PRICING, 900.0, Page.USERS, 900.0, Page.REPORTS, 900.0, Page.FEEDBACK, 900.0);
    /** File names; the Feedback page has no design PNG (X = no design). */
    private static final Map<Page, String> FILES = Map.of(Page.DASHBOARD, "A1-admin-dashboard",
            Page.ORDERS, "A2-admin-orders", Page.PRICING, "A3-admin-pricing", Page.USERS, "A4-admin-users",
            Page.REPORTS, "A5-admin-reports", Page.FEEDBACK, "X4-admin-feedback");

    private final AdminApp app;
    private final Stage stage;
    private final AppContext context;
    private final File folder;
    private final Deque<Page> remaining = new ArrayDeque<>();

    private ScreenshotTour(AdminApp app, Stage stage, AppContext context, File folder) {
        this.app = app;
        this.stage = stage;
        this.context = context;
        this.folder = folder;
    }

    /**
     * Starts the tour when the system property is set.
     *
     * @param app     the running app
     * @param stage   its window
     * @param context shared objects
     */
    static void startIfRequested(AdminApp app, Stage stage, AppContext context) {
        String path = System.getenv(FOLDER_VARIABLE);
        if (path == null || path.isBlank()) {
            return;
        }
        File folder = new File(path);
        if (!folder.isDirectory() && !folder.mkdirs()) {
            throw new IllegalStateException("Cannot create " + folder);
        }
        new ScreenshotTour(app, stage, context, folder).run();
    }

    private void run() {
        stage.setWidth(AdminApp.WIDTH);
        stage.setHeight(AdminApp.HEIGHT);
        later(() -> {
            save("00-admin-sign-in");
            app.login().fill(System.getenv().getOrDefault(EMAIL_VARIABLE, ""),
                    System.getenv().getOrDefault(PASSWORD_VARIABLE, ""));
            app.login().submit();
            context.whenIdle(() -> later(this::pages));
        });
    }

    private void pages() {
        if (context.session() == null) {
            System.err.println("Tour: sign-in failed");
            Platform.exit();
            return;
        }
        remaining.addAll(java.util.List.of(Page.values()));
        next();
    }

    private void next() {
        Page page = remaining.poll();
        if (page == null) {
            Platform.exit();
            return;
        }
        resize(HEIGHTS.get(page));
        app.open(page);
        context.whenIdle(() -> later(() -> context.whenIdle(() -> later(() -> {
            save(FILES.get(page));
            next();
        }))));
    }

    /** Sizes the scene (not the window frame) to the design PNG. */
    private void resize(double height) {
        double frameWidth = stage.getWidth() - stage.getScene().getWidth();
        double frameHeight = stage.getHeight() - stage.getScene().getHeight();
        stage.setWidth(AdminApp.WIDTH + frameWidth);
        stage.setHeight(height + frameHeight);
    }

    private static void later(Runnable action) {
        PauseTransition pause = new PauseTransition(SETTLE);
        pause.setOnFinished(e -> action.run());
        pause.play();
    }

    private void save(String name) {
        WritableImage image = stage.getScene().snapshot(null);
        int width = (int) image.getWidth();
        int height = (int) image.getHeight();
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        PixelReader reader = image.getPixelReader();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                out.setRGB(x, y, reader.getArgb(x, y));
            }
        }
        try {
            ImageIO.write(out, "png", new File(folder, name + ".png"));
            System.out.println("Tour: saved " + name + " (" + width + "x" + height + ")");
        } catch (IOException e) {
            throw new IllegalStateException("Cannot save " + name, e);
        }
    }
}
