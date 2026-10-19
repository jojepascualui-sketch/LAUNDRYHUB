package com.laundryhub.backend.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.domain.pricing.PriceBreakdown;
import com.laundryhub.backend.domain.pricing.PriceCalculator;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.testsupport.Fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Order: constructor checks, status transitions with history, cancel and re-weighing. */
class OrderTest {

    private final PricingSettings settings = PricingSettings.defaults();
    private final Soap standard = Fixtures.standard();
    private final Admin admin = Fixtures.admin();

    private Order delivery() {
        return Fixtures.deliveryOrder("LH-0001", "10", standard, settings);
    }

    private Order walkIn() {
        return Fixtures.walkInOrder("LH-0002", "8", standard, settings);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"LH-1", "XX-0001", "LH-00A1", "lh-0001"})
    void orderNumberMustLookLikeLh0001(String orderNo) {
        ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, settings);
        LaundryLoad load = new LaundryLoad(new BigDecimal("8"));
        PriceBreakdown price = new PriceCalculator(settings).calculate(load, standard, walkIn);

        assertThatThrownBy(() -> new Order(orderNo, Fixtures.customer(), walkIn, load, standard, price,
                Fixtures.todaySlot(), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Order number must look like LH-0001");
    }

    @Test
    void deliveryNeedsAPinAndWalkInDropsIt() {
        ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, settings);
        ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, settings);
        LaundryLoad load = new LaundryLoad(new BigDecimal("8"));
        PriceCalculator calculator = new PriceCalculator(settings);

        assertThatThrownBy(() -> new Order("LH-0001", Fixtures.customer(), delivery, load, standard,
                calculator.calculate(load, standard, delivery), Fixtures.todaySlot(), null))
                .hasMessage("Pin your location for Pickup & Delivery");
        Order order = new Order("LH-0002", Fixtures.customer(), walkIn, load, standard,
                calculator.calculate(load, standard, walkIn), Fixtures.todaySlot(), Fixtures.nearPin());
        assertThat(order.getPickupLocation()).isNull();
    }

    @Test
    void priceMustBelongToTheSameOrder() {
        ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, settings);
        ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, settings);
        LaundryLoad ten = new LaundryLoad(new BigDecimal("10"));
        PriceCalculator calculator = new PriceCalculator(settings);

        // Price for 10 kg sent with a 12 kg load, and a delivery price on a walk-in order.
        assertThatThrownBy(() -> new Order("LH-0001", Fixtures.customer(), walkIn,
                new LaundryLoad(new BigDecimal("12")), standard, calculator.calculate(ten, standard, walkIn),
                Fixtures.todaySlot(), null))
                .hasMessage("Price breakdown does not match this order");
        assertThatThrownBy(() -> new Order("LH-0001", Fixtures.customer(), walkIn, ten, standard,
                calculator.calculate(ten, standard, delivery), Fixtures.todaySlot(), null))
                .hasMessage("Price breakdown does not match this order");
    }

    @Test
    void newOrderIsPendingWithOneHistoryRow() {
        Order order = delivery();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.getStatusHistory()).extracting(OrderStatusHistory::getStatus)
                .containsExactly(OrderStatus.PENDING);
        assertThat(order.getTotal()).isEqualTo(new BigDecimal("360.00"));
        assertThat(order.getServiceType()).isInstanceOf(PickupDeliveryService.class);
    }

    @Test
    void walkInSkipsOutForDelivery() {
        Order order = walkIn();
        order.changeStatus(OrderStatus.WASHING, admin, null);
        order.changeStatus(OrderStatus.READY, admin, null);

        assertThatThrownBy(() -> order.changeStatus(OrderStatus.OUT_FOR_DELIVERY, admin, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Out for delivery does not apply to Walk-in");
        order.changeStatus(OrderStatus.COMPLETED, admin, "Picked up at the counter");

        assertThat(order.getStatusHistory()).extracting(OrderStatusHistory::getStatus)
                .containsExactly(OrderStatus.PENDING, OrderStatus.WASHING, OrderStatus.READY, OrderStatus.COMPLETED);
    }

    @Test
    void deliveryGoesThroughOutForDelivery() {
        Order order = delivery();
        for (OrderStatus next : new OrderStatus[] {OrderStatus.WASHING, OrderStatus.READY,
                OrderStatus.OUT_FOR_DELIVERY, OrderStatus.COMPLETED}) {
            order.changeStatus(next, admin, null);
        }

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getStatusHistory()).hasSize(5);
    }

    @Test
    void finalOrdersCannotChangeAndRepeatsAreIgnored() {
        Order order = walkIn();
        order.changeStatus(OrderStatus.PENDING, admin, null);
        assertThat(order.getStatusHistory()).hasSize(1);

        order.changeStatus(OrderStatus.COMPLETED, admin, null);
        assertThatThrownBy(() -> order.changeStatus(OrderStatus.WASHING, admin, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order LH-0002 is already Completed");
        assertThatThrownBy(() -> walkIn().changeStatus(null, admin, null)).hasMessage("Status is required");
    }

    @Test
    void cancelNeedsAReason() {
        Order order = walkIn();

        assertThatThrownBy(() -> order.cancel(" ", admin)).hasMessage("Cancel reason must not be blank");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        order.cancel(" Customer request ", admin);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledReason()).isEqualTo("Customer request");
    }

    @Test
    void weighingRepricesTheOrderAndItsPayment() {
        Order order = delivery();
        GCashPayment gcash = new GCashPayment(order, Fixtures.GCASH_NUMBER);

        order.recordWeighedLoad(LaundryLoad.measured(new BigDecimal("12"), settings.getMinKg()));

        assertThat(order.getBillableKg()).isEqualByComparingTo("12");
        assertThat(order.getTotal()).isEqualTo(new BigDecimal("420.00"));
        assertThat(gcash.getAmount()).isEqualTo(new BigDecimal("420.00"));
    }

    @Test
    void aLighterWeighedLoadIsStillBilledAtTheMinimum() {
        Order order = delivery();

        order.recordWeighedLoad(LaundryLoad.measured(new BigDecimal("5"), settings.getMinKg()));

        assertThat(order.getWeightKg()).isEqualByComparingTo("5");
        assertThat(order.getBillableKg()).isEqualByComparingTo("8");
        assertThat(order.getTotal()).isEqualTo(new BigDecimal("300.00"));
    }

    @Test
    void weightIsFrozenOnceWashingOrPaid() {
        LaundryLoad twelve = LaundryLoad.measured(new BigDecimal("12"), settings.getMinKg());
        Order washing = delivery();
        washing.changeStatus(OrderStatus.WASHING, admin, null);
        Order paid = delivery();
        new GCashPayment(paid, Fixtures.GCASH_NUMBER).confirm("GC-1");

        assertThatThrownBy(() -> washing.recordWeighedLoad(twelve))
                .hasMessage("Weight can only change while the order is pending");
        assertThatThrownBy(() -> paid.recordWeighedLoad(twelve)).hasMessage("Weight cannot change after payment");
    }

    @Test
    void walkInHasNoGeocodedAddressAndOnlyOnePayment() {
        Order order = walkIn();
        new CashPayment(order);

        assertThatThrownBy(() -> order.recordGeocodedAddress("Somewhere"))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new CashPayment(order))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Order LH-0002 already has a payment");
    }
}
