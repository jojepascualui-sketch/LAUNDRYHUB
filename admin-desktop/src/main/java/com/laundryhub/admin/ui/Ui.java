package com.laundryhub.admin.ui;

import com.laundryhub.admin.model.OrderStatus;

import java.util.List;
import java.util.Locale;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

/** Small builders for the design's repeated pieces: badges, labels and table rows. */
public final class Ui {

    /** Percent of a full row. */
    private static final double FULL = 100.0;
    private static final int MOBILE_DIGITS = 11;
    private static final int MOBILE_FIRST = 4;
    private static final int MOBILE_SECOND = 7;

    private Ui() {
    }

    /**
     * @param text        label text
     * @param styleClasses CSS classes
     * @return the label
     */
    public static Label label(String text, String... styleClasses) {
        Label label = new Label(text == null ? "" : text);
        label.getStyleClass().addAll(styleClasses);
        return label;
    }

    /**
     * @param status order status
     * @param size   "" (12 px), "small" (A2 list) or "large" (A2 detail)
     * @return a status badge that always shows the status text
     */
    public static Label badge(OrderStatus status, String size) {
        Label badge = label(status.label(), "badge", status.styleClass());
        if (!size.isEmpty()) {
            badge.getStyleClass().add(size);
        }
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    /**
     * @param text       badge text
     * @param styleClass color class, e.g. "status-completed"
     * @return a badge
     */
    public static Label badge(String text, String styleClass) {
        Label badge = label(text, "badge", styleClass);
        badge.setMinWidth(Region.USE_PREF_SIZE);
        return badge;
    }

    /**
     * @return a spacer that takes the free width of an HBox
     */
    public static Region grow() {
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        return spacer;
    }

    /**
     * Sets the table's columns as shares of its width (the design's column positions).
     *
     * @param grid   the table
     * @param widths column widths measured on the design PNG (any unit)
     */
    public static void columns(GridPane grid, double... widths) {
        double total = 0;
        for (double width : widths) {
            total += width;
        }
        grid.getColumnConstraints().clear();
        for (double width : widths) {
            ColumnConstraints column = new ColumnConstraints();
            column.setPercentWidth(width / total * FULL);
            grid.getColumnConstraints().add(column);
        }
    }

    /**
     * Adds a header row: 12 px upper-case muted labels over a divider.
     *
     * @param grid    the table
     * @param compact A3's tighter padding
     * @param titles  column titles
     */
    public static void headerRow(GridPane grid, boolean compact, List<String> titles) {
        for (int column = 0; column < titles.size(); column++) {
            Label head = label(titles.get(column).toUpperCase(Locale.ROOT), "table-head");
            if (compact) {
                head.getStyleClass().add("compact");
            }
            head.setMaxWidth(Double.MAX_VALUE);
            grid.add(head, column, 0);
        }
    }

    /**
     * Adds one body row; every cell gets the row divider and the same height.
     *
     * @param grid    the table
     * @param row     row index (header is 0)
     * @param compact A3's tighter padding
     * @param cells   cell contents
     */
    public static void bodyRow(GridPane grid, int row, boolean compact, List<Node> cells) {
        for (int column = 0; column < cells.size(); column++) {
            HBox cell = new HBox(cells.get(column));
            cell.setAlignment(Pos.CENTER_LEFT);
            cell.getStyleClass().add("table-cell");
            if (compact) {
                cell.getStyleClass().add("compact");
            }
            cell.setMaxWidth(Double.MAX_VALUE);
            cell.setMaxHeight(Double.MAX_VALUE);
            GridPane.setFillHeight(cell, true);
            grid.add(cell, column, row);
        }
    }

    /**
     * Empty state of a table (no design): one muted row across all columns.
     *
     * @param grid    the table
     * @param row     row index
     * @param columns number of columns
     * @param text    e.g. "No orders yet."
     */
    public static void emptyRow(GridPane grid, int row, int columns, String text) {
        Label empty = label(text, "table-cell", "muted");
        empty.setMaxWidth(Double.MAX_VALUE);
        grid.add(empty, 0, row, columns, 1);
    }

    /**
     * @param mobile 11-digit mobile number, may be {@code null}
     * @return "0917 000 1021", or the input unchanged if it is not 11 digits
     */
    public static String formatMobile(String mobile) {
        if (mobile == null || mobile.length() != MOBILE_DIGITS || !mobile.chars().allMatch(Character::isDigit)) {
            return mobile == null ? "" : mobile;
        }
        return mobile.substring(0, MOBILE_FIRST) + " " + mobile.substring(MOBILE_FIRST, MOBILE_SECOND) + " "
                + mobile.substring(MOBILE_SECOND);
    }
}
