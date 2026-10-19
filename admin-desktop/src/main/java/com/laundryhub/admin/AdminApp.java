package com.laundryhub.admin;

import com.laundryhub.admin.api.ApiClient;
import com.laundryhub.admin.api.HttpAdminApi;
import com.laundryhub.admin.ui.AppContext;
import com.laundryhub.admin.ui.LoginController;
import com.laundryhub.admin.ui.Navigator;
import com.laundryhub.admin.ui.Page;
import com.laundryhub.admin.ui.ShellController;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/** Laundry HUB admin console: sign in, then the A1–A5 pages, all data from the REST API. */
// OOP: Inheritance — a JavaFX Application; it also implements the Navigator contract.
public class AdminApp extends Application implements Navigator {

    /** Design fonts (docs/design/DESIGN_SPEC.md §1), bundled under src/main/resources/fonts. */
    static final String[] FONT_FILES = {
            "dm_sans_regular.ttf", "dm_sans_medium.ttf", "dm_sans_bold.ttf",
            "bricolage_grotesque_medium.ttf", "bricolage_grotesque_bold.ttf",
            "bricolage_grotesque_extrabold.ttf"
    };

    /** Window size of the A1–A5 designs. */
    public static final double WIDTH = 1440;
    /** Design height of A3–A5. */
    public static final double HEIGHT = 900;
    static final String STYLESHEET = "/css/laundryhub.css";
    private static final String TITLE = "Laundry HUB Admin";
    private static final double MIN_WIDTH = 1100;
    private static final double MIN_HEIGHT = 700;

    private Stage stage;
    private Scene scene;
    private AppContext context;
    private LoginController login;
    private ShellController shell;

    @Override
    public void start(Stage primaryStage) throws IOException {
        loadFonts();
        stage = primaryStage;
        AppConfig config = AppConfig.fromSystem();
        ApiClient client = new ApiClient(config.apiBaseUrl(), () -> context == null ? null : context.token());
        context = new AppContext(config, new HttpAdminApi(client), this, getHostServices());
        scene = new Scene(new javafx.scene.layout.StackPane(), WIDTH, HEIGHT);
        scene.getStylesheets().add(AdminApp.class.getResource(STYLESHEET).toExternalForm());
        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        stage.setScene(scene);
        showSignIn();
        stage.show();
        ScreenshotTour.startIfRequested(this, stage, context);
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

    // OOP: Polymorphism — the Application answers the Navigator calls of every controller.
    @Override
    public void showSignIn() {
        shell = null;
        FXMLLoader loader = loader("login.fxml");
        scene.setRoot(load(loader));
        login = loader.getController();
        login.attach(context);
    }

    @Override
    public void showConsole() {
        FXMLLoader loader = loader("shell.fxml");
        scene.setRoot(load(loader));
        shell = loader.getController();
        shell.attach(context);
        shell.show(Page.DASHBOARD);
    }

    @Override
    public void open(Page page) {
        if (shell != null) {
            shell.show(page);
        }
    }

    @Override
    public void openOrder(long orderId) {
        if (shell != null) {
            shell.showOrder(orderId);
        }
    }

    @Override
    public void searchOrders(String query) {
        if (shell != null) {
            shell.showSearch(query);
        }
    }

    @Override
    public void toast(String message) {
        if (shell != null) {
            shell.toast(message);
        }
    }

    /** @return the sign-in controller while it is shown */
    LoginController login() {
        return login;
    }

    private static FXMLLoader loader(String fxml) {
        return new FXMLLoader(ShellController.class.getResource(fxml));
    }

    private static Parent load(FXMLLoader loader) {
        try {
            return loader.load();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * @param args JavaFX arguments
     */
    public static void main(String[] args) {
        launch(args);
    }
}
