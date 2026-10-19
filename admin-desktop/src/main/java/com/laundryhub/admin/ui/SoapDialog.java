package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.SoapRequest;

import java.math.BigDecimal;
import java.util.Optional;

import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/** "+ Add soap" form. The design has no screen for it, so it reuses the A3 field style. */
public final class SoapDialog {

    private static final String TITLE = "Add soap";

    private SoapDialog() {
    }

    /**
     * @param owner the console window
     * @return the new soap, or empty if cancelled or incomplete
     */
    public static Optional<SoapRequest> ask(Window owner) {
        Dialog<SoapRequest> dialog = new Dialog<>();
        dialog.initOwner(owner);
        dialog.setTitle(TITLE);
        dialog.setHeaderText(TITLE);
        TextField name = Forms.field("e.g. Lavender");
        TextField description = Forms.field("Short note for customers");
        TextField addOn = Forms.field("0");
        VBox content = new VBox(16, Forms.labeled("Name", name), Forms.labeled("Description", description),
                Forms.labeled("Add-on per kg (\u20B1)", addOn));
        ButtonType add = new ButtonType("Add soap", ButtonBar.ButtonData.OK_DONE);
        Forms.style(dialog, content, add);
        dialog.setResultConverter(button -> {
            if (button != add) {
                return null;
            }
            try {
                BigDecimal price = new BigDecimal(addOn.getText().trim());
                if (name.getText().isBlank() || description.getText().isBlank() || price.signum() < 0) {
                    return null;
                }
                return new SoapRequest(name.getText().trim(), description.getText().trim(), price, true, null, null);
            } catch (NumberFormatException e) {
                return null;
            }
        });
        return dialog.showAndWait();
    }
}
