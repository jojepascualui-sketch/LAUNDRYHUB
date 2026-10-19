package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.PricingSettings;
import com.laundryhub.admin.api.dto.PricingUpdate;
import com.laundryhub.admin.api.dto.Soap;
import com.laundryhub.admin.api.dto.SoapRequest;
import com.laundryhub.admin.model.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;

/**
 * A3-admin-pricing: rates, fabric soaps (availability) and payment methods. "Save changes" sends the
 * rates and payment settings and every changed soap; the server validates and stores them.
 */
// OOP: Inheritance — a PageController.
public class PricingController extends PageController {

    /** Column widths measured on A3 (Name, Add-on per kg, Available). */
    private static final double[] COLUMNS = {284, 243, 174};
    private static final List<String> HEADERS = List.of("Name", "Add-on per kg", "Available");
    private static final String SAVED = "Changes saved.";
    private static final String NUMBER_ERROR = "Rates must be numbers greater than zero (the fee may be 0).";
    private static final String ERROR_CLASS = "error";
    private static final String NO_SOAPS = "No soaps yet. Add one with + Add soap.";

    @FXML
    private TextField rate;
    @FXML
    private TextField minKg;
    @FXML
    private TextField fee;
    @FXML
    private TextField radius;
    @FXML
    private GridPane soaps;
    @FXML
    private CheckBox cash;
    @FXML
    private CheckBox gcash;
    @FXML
    private TextField gcashNumber;
    @FXML
    private Button save;

    // OOP: Encapsulation — loaded soaps and their checkboxes stay private.
    private List<Soap> loadedSoaps = List.of();
    private final Map<Long, CheckBox> available = new HashMap<>();

    @Override
    protected void onAttach() {
        Ui.columns(soaps, COLUMNS);
    }

    @Override
    public Page page() {
        return Page.PRICING;
    }

    // OOP: Polymorphism — the pricing page's own refresh.
    @Override
    public void refresh() {
        call(api().pricing(), this::render);
    }

    private void render(PricingSettings settings) {
        rate.setText(Money.number(settings.ratePerKg()));
        minKg.setText(Money.number(settings.minKg()));
        fee.setText(Money.number(settings.deliveryFee()));
        radius.setText(Money.number(settings.deliveryRadiusKm()));
        cash.setSelected(settings.cashEnabled());
        gcash.setSelected(settings.gcashEnabled());
        gcashNumber.setText(settings.shopGcashNumber() == null ? "" : settings.shopGcashNumber());
        loadedSoaps = settings.soaps();
        renderSoaps();
    }

    private void renderSoaps() {
        soaps.getChildren().clear();
        available.clear();
        Ui.headerRow(soaps, true, HEADERS);
        if (loadedSoaps.isEmpty()) {
            Ui.emptyRow(soaps, 1, HEADERS.size(), NO_SOAPS);
        }
        int row = 1;
        for (Soap soap : loadedSoaps) {
            CheckBox box = new CheckBox();
            box.setSelected(soap.available());
            box.setAccessibleText("Available");
            available.put(soap.id(), box);
            List<Node> cells = new ArrayList<>();
            cells.add(Ui.label(soap.name(), "strong"));
            cells.add(Ui.label(Money.pesoCompact(soap.addOnPerKg())));
            cells.add(box);
            Ui.bodyRow(soaps, row++, true, cells);
        }
    }

    /** "+ Add soap" (no design for the form: a dialog in the same style). */
    @FXML
    private void addSoap() {
        SoapDialog.ask(save.getScene().getWindow()).ifPresent(request ->
                call(api().createSoap(request), created -> {
                    navigator().toast(created.name() + " added.");
                    refresh();
                }));
    }

    /** "Save changes". */
    @FXML
    private void saveChanges() {
        BigDecimal rateValue = positive(rate, false);
        BigDecimal minValue = positive(minKg, false);
        BigDecimal feeValue = positive(fee, true);
        BigDecimal radiusValue = positive(radius, false);
        if (rateValue == null || minValue == null || feeValue == null || radiusValue == null) {
            showError(NUMBER_ERROR);
            return;
        }
        String number = gcashNumber.getText() == null ? "" : gcashNumber.getText().replace(" ", "").trim();
        PricingUpdate update = new PricingUpdate(rateValue, minValue, feeValue, radiusValue, cash.isSelected(),
                gcash.isSelected(), number.isEmpty() ? null : number);
        save.setDisable(true);
        CompletableFuture<?> all = api().savePricing(update);
        for (Soap soap : loadedSoaps) {
            CheckBox box = available.get(soap.id());
            if (box != null && box.isSelected() != soap.available()) {
                SoapRequest change = new SoapRequest(soap.name(), soap.description(), soap.addOnPerKg(),
                        box.isSelected(), soap.tintHex(), soap.sortOrder());
                all = all.thenCompose(done -> api().updateSoap(soap.id(), change));
            }
        }
        call(all, done -> {
            save.setDisable(false);
            navigator().toast(SAVED);
            refresh();
        }, message -> {
            save.setDisable(false);
            showError(message);
        });
    }

    /** @return the value, or {@code null} (and a red border) if it is not a valid number */
    private static BigDecimal positive(TextField field, boolean zeroAllowed) {
        field.getStyleClass().remove(ERROR_CLASS);
        try {
            BigDecimal value = new BigDecimal(field.getText().trim());
            if (value.signum() > 0 || (zeroAllowed && value.signum() == 0)) {
                return value;
            }
        } catch (NumberFormatException | NullPointerException e) {
            // marked below
        }
        field.getStyleClass().add(ERROR_CLASS);
        return null;
    }
}
