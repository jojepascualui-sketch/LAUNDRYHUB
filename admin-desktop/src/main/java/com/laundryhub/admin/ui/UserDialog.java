package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.User;
import com.laundryhub.admin.api.dto.UserCreate;
import com.laundryhub.admin.api.dto.UserUpdate;
import com.laundryhub.admin.model.Role;

import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** Add / Edit account forms for A4. The design has no screen for them, so they reuse the A3 field style. */
public final class UserDialog {

    private UserDialog() {
    }

    /**
     * @param owner console window
     * @param tab   the tab the admin is on (decides the role and extra field)
     * @return the new account, or empty if cancelled
     */
    public static Optional<UserCreate> create(Window owner, UserTab tab) {
        Dialog<UserCreate> dialog = new Dialog<>();
        dialog.initOwner(owner);
        String title = tab.addLabel().replace("+ ", "");
        dialog.setTitle(title);
        dialog.setHeaderText(title);
        TextField name = Forms.field("Juan Dela Cruz");
        TextField email = Forms.field("name@email.com");
        TextField mobile = Forms.field("09XX XXX XXXX");
        PasswordField password = new PasswordField();
        password.setPromptText("At least 8 characters");
        password.getStyleClass().add("text-input-field");
        TextField extra = Forms.field(tab == UserTab.RIDERS ? "ABC 1234" : "Counter staff");
        VBox form = new VBox(16, Forms.labeled("Full name", name), Forms.labeled("Email", email),
                Forms.labeled("Mobile number", mobile), Forms.labeled("Password", password));
        if (tab != UserTab.CUSTOMERS) {
            form.getChildren().add(Forms.labeled(tab.columnTitle(), extra));
        }
        ButtonType save = new ButtonType("Add", ButtonBar.ButtonData.OK_DONE);
        Forms.style(dialog, form, save);
        Role role = tab.newRole();
        dialog.setResultConverter(button -> button != save ? null : new UserCreate(role.name(), name.getText().trim(),
                email.getText().trim(), blankToNull(mobile.getText()), blankToNull(password.getText()),
                tab == UserTab.RIDERS ? blankToNull(extra.getText()) : null,
                tab == UserTab.STAFF ? blankToNull(extra.getText()) : null));
        return dialog.showAndWait();
    }

    /**
     * @param owner console window
     * @param tab   the tab the account is on
     * @param user  the account
     * @return the changes, or empty if cancelled
     */
    public static Optional<UserUpdate> edit(Window owner, UserTab tab, User user) {
        Dialog<UserUpdate> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle("Edit " + user.fullName());
        dialog.setHeaderText("Edit " + user.fullName());
        TextField name = Forms.field("Full name");
        name.setText(user.fullName());
        TextField mobile = Forms.field("09XX XXX XXXX");
        mobile.setText(user.mobileNumber() == null ? "" : user.mobileNumber());
        TextField extra = Forms.field("");
        extra.setText(tab == UserTab.RIDERS ? nullToBlank(user.plateNumber()) : nullToBlank(user.position()));
        PasswordField password = new PasswordField();
        password.setPromptText("Leave blank to keep the password");
        password.getStyleClass().add("text-input-field");
        CheckBox active = new CheckBox("Active");
        active.setSelected(UserTab.ACTIVE.equals(user.status()));
        VBox form = new VBox(16, Forms.labeled("Full name", name), Forms.labeled("Mobile number", mobile));
        if (tab != UserTab.CUSTOMERS) {
            form.getChildren().add(Forms.labeled(tab.columnTitle(), extra));
        }
        form.getChildren().addAll(Forms.labeled("New password", password), active);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        Forms.style(dialog, form, save);
        dialog.setResultConverter(button -> button != save ? null : new UserUpdate(name.getText().trim(),
                blankToNull(mobile.getText()), active.isSelected() ? UserTab.ACTIVE : "INACTIVE",
                tab == UserTab.RIDERS ? blankToNull(extra.getText()) : null,
                tab == UserTab.STAFF ? blankToNull(extra.getText()) : null, blankToNull(password.getText())));
        return dialog.showAndWait();
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.replace(" ", "").trim();
    }

    private static String nullToBlank(String text) {
        return text == null ? "" : text;
    }
}
