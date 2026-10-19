package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.User;

import java.util.ArrayList;
import java.util.List;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ToggleButton;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;

/** A4-admin-users: Customers / Riders / Staff tabs, the add button and the accounts table with Edit. */
// OOP: Inheritance — a PageController.
public class UsersController extends PageController {

    /** Column widths measured on A4 (Name, Contact, Orders/Vehicle/Role, Status, Edit). */
    private static final double[] COLUMNS = {290, 287, 165, 182, 177};

    @FXML
    private HBox tabs;
    @FXML
    private Button add;
    @FXML
    private GridPane table;

    // OOP: Encapsulation — the selected tab stays private.
    private UserTab tab = UserTab.CUSTOMERS;
    private final ToggleGroup group = new ToggleGroup();

    @Override
    protected void onAttach() {
        Ui.columns(table, COLUMNS);
        for (UserTab each : UserTab.values()) {
            ToggleButton segment = new ToggleButton(each.label());
            segment.getStyleClass().setAll("segment");
            segment.setToggleGroup(group);
            segment.setSelected(each == tab);
            segment.setOnAction(e -> {
                segment.setSelected(true);
                tab = each;
                refresh();
            });
            tabs.getChildren().add(segment);
        }
        add.setOnAction(e -> addUser());
    }

    @Override
    public Page page() {
        return Page.USERS;
    }

    // OOP: Polymorphism — the accounts page's own refresh.
    @Override
    public void refresh() {
        add.setText(tab.addLabel());
        UserTab requested = tab;
        call(api().users(tab.roles()), users -> {
            if (requested == tab) {
                render(users);
            }
        });
    }

    private void render(List<User> users) {
        table.getChildren().clear();
        List<String> headers = List.of("Name", "Contact", tab.columnTitle(), "Status", "");
        Ui.headerRow(table, false, headers);
        if (users.isEmpty()) {
            Ui.emptyRow(table, 1, headers.size(), "No " + tab.label().toLowerCase(java.util.Locale.ROOT) + " yet.");
        }
        int row = 1;
        // Oldest account first, as in the design (Maria, Juan, Ana, ...).
        for (User user : users.stream().sorted(java.util.Comparator.comparing(User::id)).toList()) {
            Button edit = new Button("Edit");
            edit.getStyleClass().setAll("outline-button", "row-button");
            edit.setOnAction(e -> editUser(user));
            List<Node> cells = new ArrayList<>();
            cells.add(Ui.label(user.fullName(), "strong"));
            cells.add(Ui.label(tab.contact(user)));
            cells.add(Ui.label(tab.thirdColumn(user)));
            cells.add(Ui.badge(tab.statusText(user), tab.statusStyle(user)));
            cells.add(edit);
            Ui.bodyRow(table, row++, false, cells);
        }
    }

    private void addUser() {
        UserDialog.create(add.getScene().getWindow(), tab).ifPresent(request ->
                call(api().createUser(request), created -> {
                    navigator().toast(created.fullName() + " added.");
                    refresh();
                }));
    }

    private void editUser(User user) {
        UserDialog.edit(add.getScene().getWindow(), tab, user).ifPresent(request ->
                call(api().updateUser(user.id(), request), saved -> {
                    navigator().toast(saved.fullName() + " saved.");
                    refresh();
                }));
    }
}
