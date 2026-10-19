package com.laundryhub.backend.domain.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.testsupport.Fixtures;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Cash and GCash (05 Review &amp; pay, R3 Collect cash): validation and polymorphic behaviour. */
class PaymentMethodTest {

    private final PricingSettings settings = PricingSettings.defaults();

    private Order delivery() {
        return Fixtures.deliveryOrder("LH-0001", "10", Fixtures.hypoallergenic(), settings);
    }

    private Order walkIn() {
        return Fixtures.walkInOrder("LH-0002", "8", Fixtures.standard(), settings);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"12345", "9170001021", "0917000102x"})
    void gcashNeedsAValidMobileNumber(String number) {
        Order order = delivery();

        assertThatThrownBy(() -> new GCashPayment(order, number))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("GCash mobile number must look like 09XXXXXXXXX");
        // Regression: a rejected GCash payment must not stay attached to the order.
        assertThat(order.getPayment()).isNull();
    }

    @Test
    void paymentTakesTheServerTotal() {
        GCashPayment gcash = new GCashPayment(delivery(), Fixtures.GCASH_NUMBER);

        assertThat(gcash.getAmount()).isEqualTo(new BigDecimal("410.00"));
        assertThat(gcash.getAmountDue()).isEqualTo(new BigDecimal("410.00"));
        assertThat(gcash.getStatus()).isEqualTo(PaymentStatus.UNPAID);
        assertThat(gcash.getGcashNumber()).isEqualTo(Fixtures.GCASH_NUMBER);
    }

    @Test
    void eachMethodDescribesItself() {
        List<Payable> payments = List.of(new CashPayment(delivery()), new CashPayment(walkIn()),
                new GCashPayment(delivery(), Fixtures.GCASH_NUMBER));

        assertThat(payments).extracting(Payable::getDisplayName).containsExactly("Cash", "Cash", "GCash");
        assertThat(payments).extracting(Payable::getInstructions).containsExactly("Pay the rider upon delivery",
                "Pay at the counter", "Pay instantly from your e-wallet");
        assertThat(payments).extracting(Payable::collectsCashOnDelivery).containsExactly(true, false, false);
    }

    @Test
    void riderCollectsCashOnlyForDeliveries() {
        Rider rider = Fixtures.rider("rider1@example.com", true);
        CashPayment onDelivery = new CashPayment(delivery());
        CashPayment atCounter = new CashPayment(walkIn());

        onDelivery.recordCollectedBy(rider);
        assertThat(onDelivery.isPaid()).isTrue();
        assertThat(onDelivery.getCollectedBy()).isSameAs(rider);
        assertThat(onDelivery.getAmountDue()).isEqualTo(new BigDecimal("0.00"));
        assertThatThrownBy(() -> atCounter.recordCollectedBy(rider))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Walk-in orders are paid at the counter");
    }

    @Test
    void gcashConfirmationNeedsAReference() {
        GCashPayment gcash = new GCashPayment(delivery(), Fixtures.GCASH_NUMBER);

        assertThatThrownBy(() -> gcash.confirm(" ")).hasMessage("GCash reference number must not be blank");
        gcash.confirm("GC-123456");
        assertThat(gcash.getReferenceNo()).isEqualTo("GC-123456");
        assertThat(gcash.getPaidAt()).isNotNull();
    }

    @Test
    void aPaymentIsPaidOnceAndRefundedOnlyAfterPaying() {
        CashPayment cash = new CashPayment(walkIn());

        assertThatThrownBy(cash::markRefunded).hasMessage("Only a paid payment can be refunded");
        cash.recordPaidAtCounter(null);
        assertThatThrownBy(() -> cash.recordPaidAtCounter(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Payment for LH-0002 is already PAID");
        assertThatThrownBy(() -> cash.changeAmount(BigDecimal.TEN)).hasMessage("Amount cannot change after payment");
        cash.markRefunded();
        assertThat(cash.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void paymentChoiceBuildsTheMatchingSubclass() {
        assertThat(PaymentChoice.CASH.attachTo(walkIn(), null)).isInstanceOf(CashPayment.class)
                .extracting(PaymentMethod::getMethod).isEqualTo(PaymentChoice.CASH);
        assertThat(PaymentChoice.GCASH.attachTo(delivery(), Fixtures.GCASH_NUMBER)).isInstanceOf(GCashPayment.class)
                .extracting(PaymentMethod::getMethod).isEqualTo(PaymentChoice.GCASH);
        assertThatThrownBy(() -> PaymentChoice.GCASH.attachTo(delivery(), null))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(PaymentChoice.GCASH.getDisplayName()).isEqualTo("GCash");
    }

    @Test
    void paymentChoiceFollowsTheAdminSwitches() {
        PricingSettings cashOnly = PricingSettings.defaults();
        Admin admin = Fixtures.admin();
        cashOnly.update(new BigDecimal("30"), new BigDecimal("8"), new BigDecimal("60"), new BigDecimal("5"), true,
                false, null, admin);

        assertThat(PaymentChoice.CASH.isEnabledIn(cashOnly)).isTrue();
        assertThat(PaymentChoice.GCASH.isEnabledIn(cashOnly)).isFalse();
    }

    @Test
    void paymentNeedsAnOrder() {
        assertThatThrownBy(() -> new CashPayment(null)).hasMessage("Order is required");
    }
}
