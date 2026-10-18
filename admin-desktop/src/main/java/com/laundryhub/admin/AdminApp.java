package com.laundryhub.admin;

import java.io.IOException;
import java.io.InputStream;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class AdminApp extends Application {

    /** Design fonts (docs/design/DESIGN_SPEC.md §1), bundled under src/main/resources/fonts. */
    static final String[] FONT_FILES = {
            "dm_sans_regular.ttf", "dm_sans_medium.ttf", "dm_sans_bold.ttf",
            "bricolage_grotesque_medium.ttf", "bricolage_grotesque_bold.ttf",
            "bricolage_grotesque_extrabold.ttf"
    };

    @Override
    public void start(Stage stage) throws Exception {
        loadFonts();
        FXMLLoader loader = new FXMLLoader(AdminApp.class.getResource("main.fxml"));
        stage.setTitle("Laundry HUB Admin");
        stage.setScene(new Scene(loader.load(), 640, 400));
        stage.show();
    }

    /** Registers the bundled fonts so CSS can use "DM Sans" and "Bricolage Grotesque". */
    static void loadFonts() throws IOException {
        for (String file : FONT_FILES) {
            try (InputStream in = AdminApp.class.getResourceAsStream("/fonts/" + file)) {
                if (in == null || Font.loadFont(in, 0) == null) {
                    throw new IOException("Could not load font " + file);
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
