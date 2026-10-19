package com.laundryhub.backend.domain.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Time slots (04a / 04b chips), service types and the order status rules. */
class ScheduleAndStatusTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);
    private static final LocalTime TWO_PM = LocalTime.of(14, 0);
    private static final LocalTime FOUR_PM = LocalTime.of(16, 0);
    private static final BigDecimal FEE = new BigDecimal("60");

    // ---- TimeSlot ---------------------------------------------------------------------------

    @Test
    void timeSlotNeedsADateAndALabel() {
        assertThatThrownBy(() -> new TimeSlot(null, "2–4 PM", TWO_PM, FOUR_PM)).hasMessage("Schedule date is required");
        assertThatThrownBy(() -> new TimeSlot(TODAY, " ", TWO_PM, FOUR_PM))
                .hasMessage("Schedule label must not be blank");
        assertThatThrownBy(() -> new TimeSlot(TODAY, "x".repeat(TimeSlot.MAX_LABEL_LENGTH + 1), null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void timeSlotNeedsBothTimesInOrder() {
        assertThatThrownBy(() -> new TimeSlot(TODAY, "2–4 PM", TWO_PM, null))
                .hasMessage("Schedule needs both a start and an end time, or neither");
        assertThatThrownBy(() -> new TimeSlot(TODAY, "2–4 PM", null, FOUR_PM))
                .hasMessage("Schedule needs both a start and an end time, or neither");
        assertThatThrownBy(() -> new TimeSlot(TODAY, "2–4 PM", FOUR_PM, TWO_PM))
                .hasMessage("Schedule must start before it ends");
        assertThatThrownBy(() -> new TimeSlot(TODAY, "2–4 PM", TWO_PM, TWO_PM))
                .hasMessage("Schedule must start before it ends");
    }

    @Test
    void displayTextSaysTodayTomorrowOrTheDate() {
        assertThat(new TimeSlot(TODAY, "2–4 PM", TWO_PM, FOUR_PM).displayText(TODAY)).isEqualTo("Today, 2–4 PM");
        assertThat(TimeSlot.partOfDay(TODAY.plusDays(1), "morning").displayText(TODAY)).isEqualTo("Tomorrow, morning");
        assertThat(TimeSlot.partOfDay(TODAY.plusDays(3), "afternoon").displayText(TODAY))
                .isEqualTo("2026-10-22, afternoon");
    }

    @Test
    void equalSlotsAreEqual() {
        assertThat(new TimeSlot(TODAY, "2–4 PM", TWO_PM, FOUR_PM))
                .isEqualTo(new TimeSlot(TODAY, "2–4 PM", TWO_PM, FOUR_PM))
                .hasSameHashCodeAs(new TimeSlot(TODAY, "2–4 PM", TWO_PM, FOUR_PM))
                .isNotEqualTo(TimeSlot.partOfDay(TODAY, "2–4 PM"));
    }

    @Test
    void slotOptionMovesByItsDayOffset() {
        SlotOption tomorrowMorning = SlotOption.tomorrow("morning");

        assertThat(tomorrowMorning.on(TODAY).getDate()).isEqualTo(TODAY.plusDays(1));
        assertThatThrownBy(() -> new SlotOption(-1, "morning", null, null))
                .hasMessage("Day offset must not be negative");
    }

    // ---- ServiceType (polymorphism) ---------------------------------------------------------

    @Test
    void eachServiceOffersItsOwnChips() {
        ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, FEE);
        ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, FEE);

        assertThat(delivery.getSlotOptions()).extracting(SlotOption::label)
                .containsExactly("2–4 PM", "4–6 PM", "9–11 AM", "1–3 PM");
        assertThat(walkIn.getSlotOptions()).extracting(SlotOption::label)
                .containsExactly("morning", "afternoon", "morning", "afternoon");
        assertThat(delivery.findSlot("4–6 PM").start()).isEqualTo(LocalTime.of(16, 0));
        assertThatThrownBy(() -> walkIn.findSlot("2–4 PM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("\"2–4 PM\" is not a time slot for Walk-in");
    }

    @Test
    void onlyDeliveryNeedsAPinAndRiders() {
        ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, FEE);
        ServiceType walkIn = ServiceType.of(ServiceCode.WALK_IN, FEE);

        assertThat(delivery.requiresPinnedLocation()).isTrue();
        assertThat(delivery.createsRiderJobs()).isTrue();
        assertThat(delivery.getServiceFee()).isEqualTo(new BigDecimal("60.00"));
        assertThat(walkIn.requiresPinnedLocation()).isFalse();
        assertThat(walkIn.createsRiderJobs()).isFalse();
        assertThat(walkIn.getServiceFee()).isEqualTo(new BigDecimal("0.00"));
        assertThat(delivery).hasToString("Pickup & Delivery");
        assertThatThrownBy(() -> ServiceType.of(null, FEE)).hasMessage("Service is required");
        assertThatThrownBy(() -> new PickupDeliveryService(new BigDecimal("-1")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---- OrderStatus ------------------------------------------------------------------------

    @Test
    void outForDeliveryAppliesOnlyToPickupAndDelivery() {
        assertThat(OrderStatus.OUT_FOR_DELIVERY.appliesTo(ServiceCode.PICKUP_DELIVERY)).isTrue();
        assertThat(OrderStatus.OUT_FOR_DELIVERY.appliesTo(ServiceCode.WALK_IN)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = OrderStatus.class, names = "OUT_FOR_DELIVERY", mode = EnumSource.Mode.EXCLUDE)
    void everyOtherStatusAppliesToBothServices(OrderStatus status) {
        assertThat(status.appliesTo(ServiceCode.WALK_IN)).isTrue();
        assertThat(status.appliesTo(ServiceCode.PICKUP_DELIVERY)).isTrue();
    }

    @Test
    void onlyCompletedAndCancelledAreFinal() {
        assertThat(OrderStatus.values()).filteredOn(OrderStatus::isFinal)
                .containsExactly(OrderStatus.COMPLETED, OrderStatus.CANCELLED);
    }

    @Test
    void labelsMatchTheStatusBadges() {
        assertThat(OrderStatus.values()).extracting(OrderStatus::getLabel)
                .containsExactly("Pending", "Washing", "Ready", "Out for delivery", "Completed", "Cancelled");
    }
}
