package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.Dashboard;
import com.laundryhub.admin.api.dto.OrderRow;
import com.laundryhub.admin.api.dto.StatusCount;
import com.laundryhub.admin.model.Money;
import com.laundryhub.admin.model.OrderStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;

/** A1-admin-dashboard: four numbers, the order pipeline and the recent orders table. */
// OOP: Inheritance — a PageController.
public class DashboardController extends PageController {

    /** Column widths measured on A1 (Order, Customer, Service, Kg, Payment, Total, Status, Open). */
    private static final double[] COLUMNS = {124, 178, 198, 52, 116, 117, 196, 124};
    private static final List<String> HEADERS = List.of("Order", "Customer", "Service", "Kg", "Payment", "Total",
            "Status", "");
    private static final String KG_SUFFIX = " kg";
    private static final String NO_ORDERS = "No orders yet.";

    @FXML
    private Label ordersToday;
    @FXML
    private Label waiting;
    @FXML
    private Label kilos;
    @FXML
    private Label sales;
    @FXML
    private GridPane pipeline;
    @FXML
    private GridPane recent;

    @Override
    protected void onAttach() {
        Ui.columns(pipeline, 1, 1, 1, 1, 1);
        Ui.columns(recent, COLUMNS);
    }

    @Override
    public Page page() {
        return Page.DASHBOARD;
    }

    // OOP: Polymorphism — the dashboard's own refresh.
    @Override
    public void refresh() {
        call(api().dashboard(), this::render);
    }

    /** "View all". */
    @FXML
    private void viewAll() {
        navigator().open(Page.ORDERS);
    }

    private void render(Dashboard data) {
        ordersToday.setText(String.valueOf(data.ordersToday()));
        waiting.setText(String.valueOf(data.waitingToProcess()));
        kilos.setText(Money.number(data.kilosToday()) + KG_SUFFIX);
        sales.setText(Money.peso(data.salesToday()));

        pipeline.getChildren().clear();
        int column = 0;
        for (OrderStatus status : OrderStatus.DESIGN_ORDER) {
            VBox tile = new VBox(Ui.label(status.label(), "pipeline-label"),
                    Ui.label(String.valueOf(count(data.pipeline(), status)), "figure-28"));
            tile.getStyleClass().addAll("pipeline-tile", status.styleClass());
            tile.setMaxWidth(Double.MAX_VALUE);
            pipeline.add(tile, column++, 0);
        }

        recent.getChildren().clear();
        Ui.headerRow(recent, false, HEADERS);
        if (data.recentOrders().isEmpty()) {
            Ui.emptyRow(recent, 1, HEADERS.size(), NO_ORDERS);
        }
        int row = 1;
        // Oldest first, as in the design (LH-0001 at the top).
        for (OrderRow order : data.recentOrders().stream().sorted(Comparator.comparing(OrderRow::orderNo)).toList()) {
            Button open = new Button("Open");
            open.getStyleClass().setAll("outline-button", "row-button");
            open.setOnAction(e -> navigator().openOrder(order.id()));
            List<Node> cells = new ArrayList<>();
            cells.add(Ui.label(order.orderNo(), "strong"));
            cells.add(Ui.label(order.customerName()));
            cells.add(Ui.label(order.serviceName()));
            cells.add(Ui.label(Money.number(order.weightKg())));
            cells.add(Ui.label(order.paymentName()));
            cells.add(Ui.label(Money.peso(order.total()), "strong"));
            cells.add(Ui.badge(OrderStatus.fromApi(order.status()), ""));
            cells.add(open);
            Ui.bodyRow(recent, row++, false, cells);
        }
    }

    private static long count(List<StatusCount> counts, OrderStatus status) {
        return counts.stream().filter(c -> status.name().equals(c.status())).mapToLong(StatusCount::count).sum();
    }
}
