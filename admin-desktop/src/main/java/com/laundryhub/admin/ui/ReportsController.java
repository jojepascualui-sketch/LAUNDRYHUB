package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.Report;
import com.laundryhub.admin.api.dto.Soap;
import com.laundryhub.admin.api.dto.SoapSales;
import com.laundryhub.admin.model.Money;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

/** A5-admin-reports: cash / GCash sales, delivery vs walk-in, sales by fabric soap bars and Export CSV. */
// OOP: Inheritance — a PageController.
public class ReportsController extends PageController {

    private static final double NAME_WIDTH = 140;
    private static final double VALUE_WIDTH = 90;
    private static final double ROW_GAP = 12;
    private static final String CSV_NAME = "laundryhub-sales.csv";
    private static final String CSV_FILTER = "CSV files";
    private static final String CSV_PATTERN = "*.csv";
    private static final String NO_SALES = "No sales yet.";

    @FXML
    private Label cashSales;
    @FXML
    private Label gcashSales;
    @FXML
    private Label split;
    @FXML
    private VBox bars;
    @FXML
    private Button export;

    // OOP: Encapsulation — the A3 soap order used to sort the bars.
    private List<String> soapOrder = List.of();

    @Override
    public Page page() {
        return Page.REPORTS;
    }

    // OOP: Polymorphism — the reports page's own refresh.
    @Override
    public void refresh() {
        // Bars follow the soap order of A3 (Standard, Hypoallergenic, ...), like the design.
        call(api().pricing(), settings -> {
            soapOrder = settings.soaps().stream().map(Soap::name).toList();
            call(api().report(), this::render);
        });
    }

    private void render(Report report) {
        cashSales.setText(Money.peso(report.cashSales()));
        gcashSales.setText(Money.peso(report.gcashSales()));
        split.setText(report.deliveryCount() + " / " + report.walkInCount());
        bars.getChildren().removeIf(node -> node instanceof HBox || node.getStyleClass().contains("muted"));
        if (report.soapSales().isEmpty()) {
            bars.getChildren().add(Ui.label(NO_SALES, "muted"));
        }
        BigDecimal max = report.soapSales().stream().map(SoapSales::sales)
                .reduce(BigDecimal.ONE, BigDecimal::max);
        List<SoapSales> sorted = report.soapSales().stream()
                .sorted(Comparator.comparingInt(s -> position(s.soapName()))).toList();
        for (SoapSales soap : sorted) {
            bars.getChildren().add(bar(soap, soap.sales().doubleValue() / max.doubleValue()));
        }
    }

    private int position(String soapName) {
        int index = soapOrder.indexOf(soapName);
        return index < 0 ? Integer.MAX_VALUE : index;
    }

    private static HBox bar(SoapSales soap, double share) {
        Label name = new Label(soap.soapName());
        name.setMinWidth(NAME_WIDTH);
        name.setPrefWidth(NAME_WIDTH);
        name.setMaxWidth(NAME_WIDTH);
        Region fill = new Region();
        fill.getStyleClass().add("bar-fill");
        StackPane track = new StackPane(fill);
        track.getStyleClass().add("bar-track");
        track.setAlignment(Pos.CENTER_LEFT);
        fill.maxWidthProperty().bind(track.widthProperty().multiply(share));
        fill.prefWidthProperty().bind(track.widthProperty().multiply(share));
        HBox.setHgrow(track, Priority.ALWAYS);
        Label value = Ui.label(Money.peso(soap.sales()), "strong");
        value.setMinWidth(VALUE_WIDTH);
        value.setPrefWidth(VALUE_WIDTH);
        value.setMaxWidth(VALUE_WIDTH);
        value.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(ROW_GAP, name, track, value);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setAccessibleText(soap.soapName() + " " + Money.peso(soap.sales()));
        return row;
    }

    /** "Export CSV": asks where to save, then downloads the server's CSV. */
    @FXML
    private void exportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setInitialFileName(CSV_NAME);
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter(CSV_FILTER, CSV_PATTERN));
        File target = chooser.showSaveDialog(export.getScene().getWindow());
        if (target == null) {
            return;
        }
        export.setDisable(true);
        call(api().exportCsv(), csv -> {
            export.setDisable(false);
            try {
                Files.writeString(target.toPath(), csv, StandardCharsets.UTF_8);
                navigator().toast("Saved " + target.getName());
            } catch (IOException e) {
                showError("Could not save " + target.getName());
            }
        }, message -> {
            export.setDisable(false);
            showError(message);
        });
    }
}
