package com.laundryhub.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.ShopSettings;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.exception.ServiceUnavailableException;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.ShopSettingsRepository;
import com.laundryhub.backend.testsupport.Fixtures;
import com.laundryhub.backend.web.dto.ReportResponse;
import com.laundryhub.backend.web.dto.ReportResponse.SoapSales;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** A5 sales totals (ReportService) and the delivery radius (DeliveryZoneService). */
@ExtendWith(MockitoExtension.class)
class ReportAndZoneServiceTest {

    private static final LocalDate DAY = LocalDate.of(2026, 10, 19);

    @Mock
    private OrderRepository orders;
    @Mock
    private ShopSettingsRepository shopSettings;

    private final PricingSettings settings = PricingSettings.defaults();

    // ---- ReportService ----------------------------------------------------------------------

    private List<Order> todaysOrders() {
        Order delivery = Fixtures.deliveryOrder("LH-0001", "10", Fixtures.hypoallergenic(), settings); // 410 GCash
        new GCashPayment(delivery, Fixtures.GCASH_NUMBER);
        Order walkIn = Fixtures.walkInOrder("LH-0002", "8", Fixtures.standard(), settings);           // 240 cash
        new CashPayment(walkIn);
        Order walkInHypo = Fixtures.walkInOrder("LH-0003", "9", Fixtures.hypoallergenic(), settings); // 315 cash
        new CashPayment(walkInHypo);
        Order cancelled = Fixtures.walkInOrder("LH-0004", "20", Fixtures.standard(), settings);       // not a sale
        new CashPayment(cancelled);
        cancelled.cancel("Duplicate", Fixtures.admin());
        return List.of(delivery, walkIn, walkInHypo, cancelled);
    }

    @Test
    void reportSplitsSalesAndSkipsCancelledOrders() {
        when(orders.findPlacedBetween(DAY.atStartOfDay(), DAY.plusDays(1).atStartOfDay())).thenReturn(todaysOrders());

        ReportResponse report = new ReportService(orders).report(DAY, DAY);

        assertThat(report.orderCount()).isEqualTo(3);
        assertThat(report.totalSales()).isEqualTo(new BigDecimal("965.00"));
        assertThat(report.cashSales()).isEqualTo(new BigDecimal("555.00"));
        assertThat(report.gcashSales()).isEqualTo(new BigDecimal("410.00"));
        assertThat(report.deliveryCount()).isEqualTo(1);
        assertThat(report.walkInCount()).isEqualTo(2);
        assertThat(report.deliverySales()).isEqualTo(new BigDecimal("410.00"));
        assertThat(report.walkInSales()).isEqualTo(new BigDecimal("555.00"));
        assertThat(report.soapSales()).extracting(SoapSales::soapName).containsExactly("Hypoallergenic", "Standard");
        assertThat(report.soapSales().get(0).orders()).isEqualTo(2);
        assertThat(report.soapSales().get(0).kilos()).isEqualByComparingTo("19");
        assertThat(report.soapSales().get(0).sales()).isEqualTo(new BigDecimal("725.00"));
    }

    @Test
    void emptyRangeHasZeroTotals() {
        ReportResponse report = new ReportService(orders).report(DAY, DAY);

        assertThat(report.orderCount()).isZero();
        assertThat(report.totalSales()).isEqualTo(new BigDecimal("0.00"));
        assertThat(report.soapSales()).isEmpty();
    }

    @Test
    void rangeMustBeInOrderAndAtMostAYear() {
        ReportService reports = new ReportService(orders);

        assertThatThrownBy(() -> reports.report(DAY, DAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("'from' must be on or before 'to'");
        assertThatThrownBy(() -> reports.report(DAY, DAY.plusDays(ReportService.MAX_RANGE_DAYS + 1)))
                .hasMessage("Choose a range of at most 366 days");
        reports.report(DAY, DAY.plusDays(ReportService.MAX_RANGE_DAYS));
    }

    @Test
    void defaultRangeIsThisMonthSoFar() {
        LocalDate today = LocalDate.now();

        ReportResponse report = new ReportService(orders).report(null, null);

        assertThat(report.from()).isEqualTo(today.withDayOfMonth(1));
        assertThat(report.to()).isEqualTo(today);
        assertThat(new ReportService(orders).csvFileName(DAY, DAY))
                .isEqualTo("laundryhub-sales-2026-10-19-to-2026-10-19.csv");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', quoteCharacter = '`', value = {
        "Ana Reyes|Ana Reyes",
        "=SUM(A1:A9)|'=SUM(A1:A9)",
        "+63917|'+63917",
        "@cmd|'@cmd",
        "-12.50|-12.50",
        "Reyes, Ana|`\"Reyes, Ana\"`",
        "say \"hi\"|`\"say \"\"hi\"\"\"`"})
    void csvCellsAreQuotedAndFormulaSafe(String cell, String expected) {
        assertThat(ReportService.escape(cell)).isEqualTo(expected);
    }

    @Test
    void missingCsvCellIsEmpty() {
        assertThat(ReportService.escape(null)).isEmpty();
    }

    // ---- DeliveryZoneService ----------------------------------------------------------------

    private DeliveryZoneService configuredZone() {
        return new DeliveryZoneService(new LaundryHubProperties(null, null,
                new LaundryHubProperties.Shop(Fixtures.SHOP_LAT, Fixtures.SHOP_LNG)), shopSettings);
    }

    private DeliveryZoneService unconfiguredZone() {
        return new DeliveryZoneService(new LaundryHubProperties(null, null, null), shopSettings);
    }

    private PricingSettings radius(String km) {
        PricingSettings custom = PricingSettings.defaults();
        custom.update(new BigDecimal("30"), new BigDecimal("8"), new BigDecimal("60"), new BigDecimal(km), true, false,
                null, Fixtures.admin());
        return custom;
    }

    @Test
    void configuredShopPositionWinsOverTheDatabase() {
        assertThat(configuredZone().findShopLocation()).isEqualTo(Fixtures.shop());
        verify(shopSettings, never()).getSettings();
    }

    @Test
    void fallsBackToTheShopSettingsPin() {
        ShopSettings shop = mock(ShopSettings.class);
        when(shopSettings.getSettings()).thenReturn(shop);
        when(shop.getLocation()).thenReturn(Fixtures.shop());

        assertThat(unconfiguredZone().findShopLocation()).isEqualTo(Fixtures.shop());
    }

    @Test
    void withoutAShopPositionPinsCannotBeChecked() {
        ShopSettings shop = mock(ShopSettings.class);
        when(shopSettings.getSettings()).thenReturn(shop);

        DeliveryZoneService zone = unconfiguredZone();
        assertThat(zone.findShopLocation()).isNull();
        assertThatThrownBy(() -> zone.requireInsideRadius(Fixtures.nearPin(), settings))
                .isInstanceOf(ServiceUnavailableException.class);
    }

    @Test
    void pinsInsideOrOnTheRadiusAreAccepted() {
        DeliveryZoneService zone = configuredZone();

        assertThat(zone.requireInsideRadius(Fixtures.nearPin(), settings)).isEqualByComparingTo("1.5");
        assertThat(zone.requireInsideRadius(Fixtures.nearPin(), radius("1.5"))).isEqualByComparingTo("1.5");
    }

    @Test
    void pinsOutsideTheRadiusAreRejected() {
        DeliveryZoneService zone = configuredZone();

        assertThatThrownBy(() -> zone.requireInsideRadius(Fixtures.nearPin(), radius("1.4")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Your pin is 1.5 km from the shop. We deliver within 1.4 km.");
        assertThatThrownBy(() -> zone.requireInsideRadius(Fixtures.farPin(), settings))
                .hasMessageEndingWith("We deliver within 5 km.");
        assertThatThrownBy(() -> zone.distanceFromShopKm(null)).hasMessage("Pin is required");
    }

    @ParameterizedTest
    @CsvSource({"1.25, 1.3", "1.24, 1.2", "0.05, 0.1", "0, 0.0"})
    void distancesAreRoundedHalfUpToOneDecimal(double km, String expected) {
        assertThat(DeliveryZoneService.roundKm(km)).isEqualTo(new BigDecimal(expected));
    }

    @Test
    void csvExportChecksTheRangeBeforeQuerying() {
        assertThatThrownBy(() -> new ReportService(orders).exportCsv(DAY, DAY.minusDays(1)))
                .isInstanceOf(IllegalArgumentException.class);
        verify(orders, never()).findPlacedBetween(any(), any());
    }
}
