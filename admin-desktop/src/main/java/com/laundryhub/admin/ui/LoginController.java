package com.laundryhub.admin.ui;

import com.laundryhub.admin.model.Session;

import javafx.fxml.FXML;
import javafx.scene.Group;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;

/**
 * Admin sign in in the style of 00c (blue header with rings, logo tile, "Admin sign in", Staff email,
 * Password, Sign in). Only admin and staff accounts can sign in here.
 */
// OOP: Inheritance — a BaseController.
public class LoginController extends BaseController {

    /** Ring sizes from Auth.dc.html: 220 px SVG of a 160 view box, r 70 / 34, stroke 14 / 8. */
    private static final double RING_SCALE = 220.0 / 160.0;
    private static final double OUTER_RADIUS = 70 * RING_SCALE;
    private static final double OUTER_STROKE = 14 * RING_SCALE;
    private static final double INNER_RADIUS = 34 * RING_SCALE;
    private static final double INNER_STROKE = 8 * RING_SCALE;
    private static final double RING_BOX = 220;
    private static final double RING_RIGHT = -60;
    private static final double RING_TOP = -50;
    private static final double LOGO_SIZE = 28;
    private static final double CORNER = 24;
    private static final String REQUIRED = "Enter your staff email and password.";

    @FXML
    private StackPane header;
    @FXML
    private StackPane logoTile;
    @FXML
    private Label errorBanner;
    @FXML
    private TextField email;
    @FXML
    private PasswordField password;
    @FXML
    private Button signIn;

    @Override
    protected void onAttach() {
        logoTile.getChildren().add(Icons.icon(Icons.LOGO, LOGO_SIZE, "auth-logo-icon"));
        addRings();
        email.setOnAction(e -> password.requestFocus());
        password.setOnAction(e -> submit());
        showBanner(null);
    }

    /** The two decorative rings at the top right of the blue header, clipped to it. */
    private void addRings() {
        Circle outer = new Circle(RING_BOX / 2, RING_BOX / 2, OUTER_RADIUS);
        outer.setStrokeWidth(OUTER_STROKE);
        outer.getStyleClass().add("auth-ring");
        Circle inner = new Circle(RING_BOX / 2, RING_BOX / 2, INNER_RADIUS);
        inner.setStrokeWidth(INNER_STROKE);
        inner.getStyleClass().add("auth-ring");
        Group rings = new Group(outer, inner);
        rings.setManaged(false);
        header.getChildren().add(0, rings);
        header.layoutBoundsProperty().addListener((obs, old, bounds) -> {
            rings.setLayoutX(bounds.getWidth() - RING_BOX - RING_RIGHT);
            rings.setLayoutY(RING_TOP);
        });
        // Rounded top corners like the header; the clip runs past the bottom so those corners stay square.
        Rectangle clip = new Rectangle();
        clip.setArcWidth(CORNER * 2);
        clip.setArcHeight(CORNER * 2);
        clip.widthProperty().bind(header.widthProperty());
        clip.heightProperty().bind(header.heightProperty().add(CORNER));
        header.setClip(clip);
    }

    /**
     * Fills the form (screenshot tour).
     *
     * @param staffEmail email
     * @param secret     password
     */
    public void fill(String staffEmail, String secret) {
        email.setText(staffEmail);
        password.setText(secret);
    }

    /** "Sign in". */
    @FXML
    public void submit() {
        String user = email.getText() == null ? "" : email.getText().trim();
        String secret = password.getText() == null ? "" : password.getText();
        if (user.isEmpty() || secret.isEmpty()) {
            showError(REQUIRED);
            return;
        }
        signIn.setDisable(true);
        showBanner(null);
        call(api().signIn(user, secret), response -> {
            signIn.setDisable(false);
            password.clear();
            context().signIn(Session.from(response));
            navigator().showConsole();
        }, message -> {
            signIn.setDisable(false);
            showError(message);
        });
    }

    // OOP: Polymorphism — sign-in errors appear in the banner above the form.
    @Override
    protected void showError(String message) {
        showBanner(message);
    }

    private void showBanner(String message) {
        boolean visible = message != null && !message.isBlank();
        errorBanner.setText(visible ? message : "");
        errorBanner.setVisible(visible);
        errorBanner.setManaged(visible);
    }
}
