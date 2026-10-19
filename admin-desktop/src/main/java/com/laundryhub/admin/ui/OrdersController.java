package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.Job;
import com.laundryhub.admin.api.dto.OrderDetail;
import com.laundryhub.admin.api.dto.OrderList;
import com.laundryhub.admin.api.dto.OrderRow;
import com.laundryhub.admin.api.dto.OrderUpdate;
import com.laundryhub.admin.api.dto.PinnedLocation;
import com.laundryhub.admin.api.dto.PriceBreakdown;
import com.laundryhub.admin.api.dto.RiderOption;
import com.laundryhub.admin.model.Money;
import com.laundryhub.admin.model.OrderStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * A2-admin-orders: status chips with counts, the order list and the detail panel (info tiles, pinned
 * location map, Update status, Assign rider, price breakdown). Status and rider changes are saved
 * right away; the server recalculates nothing the admin can type.
 */
// OOP: Inheritance — a PageController.
public class OrdersController extends PageController {

    private static final String ALL = "All";
    private static final String WALK_IN_NOTE = "Walk-in order \u2014 customer drops off and picks up at the shop.";
    private static final String NONE = "No orders with this status.";
    private static final String DELIVERY = "PICKUP_DELIVERY";
    private static final String PAID = "paid";
    private static final String UNPAID = "unpaid";
    private static final String SEPARATOR = " \u00B7 ";
    private static final String FINISHED_JOB = "COMPLETED";
    private static final String CANCELLED_JOB = "CANCELLED";
    private static final String PICK_RIDER = "Choose a rider";

    @FXML
    private FlowPane filters;
    @FXML
    private VBox list;
    @FXML
    private VBox detail;
    @FXML
    private Label detailId;
    @FXML
    private Label detailCustomer;
    @FXML
    private StackPane detailBadge;
    @FXML
    private GridPane tiles;
    @FXML
    private Label serviceValue;
    @FXML
    private Label weightValue;
    @FXML
    private Label soapValue;
    @FXML
    private Label scheduleValue;
    @FXML
    private VBox locationBox;
    @FXML
    private StackPane mapFrame;
    @FXML
    private Label addressLine;
    @FXML
    private Hyperlink openMaps;
    @FXML
    private Label walkInNote;
    @FXML
    private FlowPane statusButtons;
    @FXML
    private VBox riderBox;
    @FXML
    private ComboBox<RiderOption> rider;
    @FXML
    private Label laundryLabel;
    @FXML
    private Label laundryValue;
    @FXML
    private Label soapLabel;
    @FXML
    private Label soapCost;
    @FXML
    private Label feeValue;
    @FXML
    private Label totalLabel;
    @FXML
    private Label totalValue;

    // OOP: Encapsulation — page state stays private.
    private List<OrderRow> orders = List.of();
    private OrderStatus filter;
    private String query = "";
    private Long selectedId;
    private OrderDetail shown;
    private boolean updatingRider;
    private OrderMap map;

    @Override
    protected void onAttach() {
        Ui.columns(tiles, 1, 1, 1, 1);
        map = new OrderMap(mapFrame, context().config());
        rider.setCellFactory(view -> new RiderCell());
        rider.setButtonCell(new RiderCell());
        rider.setPromptText(PICK_RIDER);
        rider.valueProperty().addListener((obs, old, value) -> {
            if (!updatingRider && value != null && shown != null) {
                saveRider(value);
            }
        });
        openMaps.setOnAction(e -> {
            if (shown != null && shown.location() != null) {
                context().openInBrowser(shown.location().mapsUrl());
            }
        });
        detail.setVisible(false);
    }

    @Override
    public Page page() {
        return Page.ORDERS;
    }

    /**
     * Selects an order on the next refresh (A1 "Open").
     *
     * @param orderId the order
     */
    public void select(long orderId) {
        selectedId = orderId;
        filter = null;
        query = "";
    }

    /**
     * Filters by the header search on the next refresh.
     *
     * @param text order no. or customer
     */
    public void search(String text) {
        query = text == null ? "" : text.trim();
        filter = null;
    }

    // OOP: Polymorphism — the orders page's own refresh.
    @Override
    public void refresh() {
        call(api().orders(null, query), this::showList);
        call(api().riders(), riders -> {
            updatingRider = true;
            rider.getItems().setAll(riders);
            updatingRider = false;
            if (shown != null) {
                selectRider(shown);
            }
        });
    }

    private void showList(OrderList result) {
        orders = result.orders().stream().sorted(Comparator.comparing(OrderRow::orderNo)).toList();
        renderFilters();
        if (selectedId == null || orders.stream().noneMatch(o -> Objects.equals(o.id(), selectedId))) {
            selectedId = orders.isEmpty() ? null : orders.get(0).id();
        }
        renderList();
        if (selectedId != null) {
            loadDetail(selectedId);
        } else {
            detail.setVisible(false);
        }
    }

    private void renderFilters() {
        filters.getChildren().clear();
        filters.getChildren().add(chip(ALL, orders.size(), filter == null, null));
        for (OrderStatus status : OrderStatus.DESIGN_ORDER) {
            long count = orders.stream().filter(o -> status.name().equals(o.status())).count();
            filters.getChildren().add(chip(status.label(), count, filter == status, status));
        }
    }

    private ToggleButton chip(String label, long count, boolean selected, OrderStatus status) {
        ToggleButton chip = new ToggleButton(label + " (" + count + ")");
        chip.getStyleClass().setAll("outline-button", "chip-button");
        chip.setSelected(selected);
        chip.setOnAction(e -> {
            filter = status;
            renderFilters();
            renderList();
        });
        return chip;
    }

    private void renderList() {
        list.getChildren().clear();
        List<OrderRow> visible = orders.stream()
                .filter(o -> filter == null || filter.name().equals(o.status()))
                .toList();
        if (visible.isEmpty()) {
            Label empty = Ui.label(NONE, "empty-box");
            empty.setMaxWidth(Double.MAX_VALUE);
            list.getChildren().add(empty);
            return;
        }
        for (OrderRow order : visible) {
            list.getChildren().add(card(order));
        }
    }

    private VBox card(OrderRow order) {
        HBox top = new HBox(8, Ui.label(order.orderNo(), "order-card-id"), Ui.grow(),
                Ui.badge(OrderStatus.fromApi(order.status()), "small"));
        top.setAlignment(Pos.CENTER_LEFT);
        Label sub = Ui.label(order.serviceName() + SEPARATOR + Money.number(order.weightKg()) + " kg" + SEPARATOR
                + Money.peso(order.total()), "small");
        VBox card = new VBox(top, Ui.label(order.customerName()), sub);
        card.getStyleClass().add("order-card");
        card.setAccessibleText(order.orderNo() + ", " + order.customerName());
        card.pseudoClassStateChanged(javafx.css.PseudoClass.getPseudoClass("selected"),
                Objects.equals(order.id(), selectedId));
        card.setOnMouseClicked(e -> {
            selectedId = order.id();
            renderList();
            loadDetail(order.id());
        });
        return card;
    }

    private void loadDetail(long id) {
        call(api().order(id), this::showDetail);
    }

    private void showDetail(OrderDetail order) {
        shown = order;
        detail.setVisible(true);
        OrderRow row = order.order();
        OrderStatus status = OrderStatus.fromApi(row.status());
        detailId.setText(row.orderNo());
        detailCustomer.setText(row.customerName() + SEPARATOR + Ui.formatMobile(row.customerMobile()));
        detailBadge.getChildren().setAll(Ui.badge(status, "large"));

        serviceValue.setText(row.serviceName());
        weightValue.setText(Money.number(order.price().billableKg()) + " kg");
        soapValue.setText(order.soapName());
        scheduleValue.setText(order.schedule() == null ? "" : order.schedule().text());

        boolean delivery = DELIVERY.equals(row.serviceType());
        show(locationBox, delivery);
        show(walkInNote, !delivery);
        show(riderBox, delivery);
        walkInNote.setText(WALK_IN_NOTE);
        if (delivery) {
            PinnedLocation pin = order.location();
            map.show(pin);
            addressLine.setText(pin == null ? "" : address(pin) + SEPARATOR + pin.latitude().toPlainString() + ", "
                    + pin.longitude().toPlainString());
            selectRider(order);
        }

        statusButtons.getChildren().clear();
        for (String option : order.statusOptions()) {
            OrderStatus choice = OrderStatus.fromApi(option);
            ToggleButton button = new ToggleButton(choice.label());
            button.getStyleClass().setAll("outline-button", "status-button");
            button.setSelected(choice == status);
            button.setOnAction(e -> {
                button.setSelected(choice == status);
                if (choice != status) {
                    saveStatus(choice);
                }
            });
            statusButtons.getChildren().add(button);
        }

        PriceBreakdown price = order.price();
        laundryLabel.setText("Laundry (" + Money.number(price.billableKg()) + " kg \u00D7 "
                + Money.pesoCompact(price.ratePerKg()) + ")");
        laundryValue.setText(Money.peso(price.laundryCost()));
        soapLabel.setText(order.soapName() + " add-on");
        soapCost.setText(Money.peso(price.soapCost()));
        feeValue.setText(Money.peso(price.deliveryFee()));
        totalLabel.setText("Total" + SEPARATOR + row.paymentName() + " (" + (row.paid() ? PAID : UNPAID) + ")");
        totalValue.setText(Money.peso(price.total()));
    }

    private static String address(PinnedLocation pin) {
        if (pin.addressDetails() != null && !pin.addressDetails().isBlank()) {
            return pin.addressDetails();
        }
        return pin.geocodedAddress() == null ? "" : pin.geocodedAddress();
    }

    /** The rider of the open job (pickup until done, then the return delivery). */
    private void selectRider(OrderDetail order) {
        Long riderId = null;
        List<Job> jobs = order.jobs() == null ? List.of() : order.jobs();
        for (Job job : jobs) {
            if (!FINISHED_JOB.equals(job.status()) && !CANCELLED_JOB.equals(job.status())) {
                riderId = job.riderId();
                break;
            }
        }
        if (riderId == null && !jobs.isEmpty()) {
            riderId = jobs.get(jobs.size() - 1).riderId();
        }
        Long id = riderId;
        updatingRider = true;
        rider.setValue(rider.getItems().stream().filter(r -> Objects.equals(r.id(), id)).findFirst().orElse(null));
        updatingRider = false;
    }

    private void saveStatus(OrderStatus status) {
        long id = shown.order().id();
        call(api().updateOrder(id, new OrderUpdate(status.name(), null)), updated -> {
            replaceRow(updated.order());
            showDetail(updated);
        });
    }

    private void saveRider(RiderOption option) {
        long id = shown.order().id();
        call(api().updateOrder(id, new OrderUpdate(null, option.id())), this::showDetail,
                message -> {
                    showError(message);
                    selectRider(shown);
                });
    }

    private void replaceRow(OrderRow updated) {
        List<OrderRow> copy = new ArrayList<>(orders);
        copy.replaceAll(o -> Objects.equals(o.id(), updated.id()) ? updated : o);
        orders = List.copyOf(copy);
        renderFilters();
        renderList();
    }

    private static void show(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    /** "[RIDER 1]" in the Assign rider dropdown. */
    private static final class RiderCell extends ListCell<RiderOption> {
        @Override
        protected void updateItem(RiderOption item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : item.fullName());
        }
    }
}
