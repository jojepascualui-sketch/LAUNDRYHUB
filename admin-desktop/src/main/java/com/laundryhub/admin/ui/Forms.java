package com.laundryhub.admin.ui;

import javafx.scene.Node;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/** Field and dialog helpers in the A3 input style (labels 13 px bold, 6 px gap, 46 px inputs). */
public final class Forms {

    private static final double LABEL_GAP = 6;
    private static final double DIALOG_WIDTH = 420;
    private static final String STYLESHEET = "/css/laundryhub.css";

    private Forms() {
    }

    /**
     * @param prompt placeholder
     * @return a text field in the design's style
     */
    public static TextField field(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.getStyleClass().add("text-input-field");
        return field;
    }

    /**
     * @param text  label above the input
     * @param input the input
     * @return label and input stacked 6 px apart
     */
    public static VBox labeled(String text, Node input) {
        Label label = Ui.label(text, "field-label");
        label.setLabelFor(input);
        return new VBox(LABEL_GAP, label, input);
    }

    /**
     * @param dialog  the dialog
     * @param content form
     * @param ok      the confirm button
     */
    public static void style(Dialog<?> dialog, Node content, ButtonType ok) {
        DialogPane pane = dialog.getDialogPane();
        pane.getStylesheets().add(Forms.class.getResource(STYLESHEET).toExternalForm());
        pane.setContent(content);
        pane.setPrefWidth(DIALOG_WIDTH);
        pane.getButtonTypes().setAll(ButtonType.CANCEL, ok);
        dialog.setGraphic(null);
    }
}
