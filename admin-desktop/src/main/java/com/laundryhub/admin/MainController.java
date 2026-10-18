package com.laundryhub.admin;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class MainController {

    static final String API_BASE_URL = "http://localhost:8080/api/";

    private final HttpClient http = HttpClient.newHttpClient();

    @FXML
    private Label statusLabel;

    @FXML
    private void onPing() {
        statusLabel.setText("Pinging " + API_BASE_URL + "ping ...");
        HttpRequest request = HttpRequest.newBuilder(URI.create(API_BASE_URL + "ping")).GET().build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenAccept(res -> Platform.runLater(() -> statusLabel.setText(res.statusCode() + " " + res.body())))
                .exceptionally(err -> {
                    Platform.runLater(() -> statusLabel.setText("Backend unreachable: " + err.getMessage()));
                    return null;
                });
    }
}
