package com.laundryhub.admin.ui;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.animation.SequentialTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.util.Duration;

/**
 * The console frame (A1–A5): dark sidebar with logo, navigation (active item #24333F with an accent dot),
 * profile and Log out; header with kicker, 30 px title, search and bell; the current page below.
 */
// OOP: Inheritance — a BaseController.
public class ShellController extends BaseController {

    private static final double NAV_DOT = 4;
    private static final double LOGO_SIZE = 20;
    private static final double BELL_SIZE = 20;
    private static final Duration TOAST_IN = Duration.millis(150);
    private static final Duration TOAST_STAY = Duration.seconds(3);
    private static final String NO_NOTIFICATIONS = "Notifications are not available yet.";

    @FXML
    private StackPane brandTile;
    @FXML
    private VBox nav;
    @FXML
    private Label avatar;
    @FXML
    private Label profileName;
    @FXML
    private Label profileRole;
    @FXML
    private Hyperlink logout;
    @FXML
    private Label kicker;
    @FXML
    private Label title;
    @FXML
    private TextField search;
    @FXML
    private Button bell;
    @FXML
    private StackPane pageHolder;
    @FXML
    private ScrollPane scroll;
    @FXML
    private Label toast;
    @FXML
    private ProgressIndicator loading;

    // OOP: Encapsulation — loaded pages and nav buttons are private to the shell.
    private final Map<Page, PageController> pages = new EnumMap<>(Page.class);
    private final Map<Page, Parent> views = new EnumMap<>(Page.class);
    private final Map<Page, ToggleButton> navButtons = new EnumMap<>(Page.class);
    private final ToggleGroup navGroup = new ToggleGroup();
    private Page current;

    @Override
    protected void onAttach() {
        brandTile.getChildren().add(Icons.icon(Icons.LOGO, LOGO_SIZE, "logo-icon"));
        bell.setGraphic(Icons.icon(Icons.BELL, BELL_SIZE, "icon-stroke"));
        bell.setOnAction(e -> navigator().toast(NO_NOTIFICATIONS));
        search.setOnAction(e -> navigator().searchOrders(search.getText()));
        logout.setOnAction(e -> {
            context().signOut();
            navigator().showSignIn();
        });
        for (Page page : Page.values()) {
            ToggleButton item = new ToggleButton(page.navLabel());
            Circle dot = new Circle(NAV_DOT);
            dot.getStyleClass().add("nav-dot");
            item.setGraphic(dot);
            item.getStyleClass().setAll("nav-item");
            item.setToggleGroup(navGroup);
            item.setMaxWidth(Double.MAX_VALUE);
            item.setOnAction(e -> show(page));
            navButtons.put(page, item);
            nav.getChildren().add(item);
        }
        if (context().session() != null) {
            avatar.setText(context().session().initials());
            profileName.setText(context().session().fullName());
            profileRole.setText(context().session().position());
        }
        toast.setVisible(false);
        loading.visibleProperty().bind(context().busyProperty());
    }

    /**
     * Shows a page, loading it the first time, and refreshes its data.
     *
     * @param page the page
     */
    public void show(Page page) {
        PageController controller = controller(page);
        if (current != null && current != page) {
            pages.get(current).onHide();
        }
        current = page;
        navButtons.get(page).setSelected(true);
        kicker.setText(page.kicker());
        title.setText(page.title());
        pageHolder.getChildren().setAll(views.get(page));
        scroll.setVvalue(0);
        controller.refresh();
    }

    /**
     * Orders with one order selected.
     *
     * @param orderId the order
     */
    public void showOrder(long orderId) {
        with(Page.ORDERS, OrdersController.class, orders -> orders.select(orderId));
    }

    /**
     * Orders filtered by the search box.
     *
     * @param query order no. or customer
     */
    public void showSearch(String query) {
        search.setText(query);
        with(Page.ORDERS, OrdersController.class, orders -> orders.search(query));
    }

    private <C extends PageController> void with(Page page, Class<C> type, Consumer<C> before) {
        before.accept(type.cast(controller(page)));
        show(page);
    }

    /** @return the page on screen, or {@code null} */
    public Page current() {
        return current;
    }

    /**
     * @param message text for a short toast at the bottom right
     */
    public void toast(String message) {
        toast.setText(message);
        toast.setOpacity(0);
        toast.setVisible(true);
        FadeTransition in = new FadeTransition(TOAST_IN, toast);
        in.setToValue(1);
        FadeTransition out = new FadeTransition(TOAST_IN, toast);
        out.setToValue(0);
        SequentialTransition sequence = new SequentialTransition(in, new PauseTransition(TOAST_STAY), out);
        sequence.setOnFinished(e -> toast.setVisible(false));
        sequence.playFromStart();
    }

    private PageController controller(Page page) {
        PageController controller = pages.get(page);
        if (controller != null) {
            return controller;
        }
        FXMLLoader loader = new FXMLLoader(ShellController.class.getResource(page.fxml()));
        try {
            views.put(page, loader.load());
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot load " + page.fxml(), e);
        }
        controller = loader.getController();
        controller.attach(context());
        pages.put(page, controller);
        return controller;
    }
}
