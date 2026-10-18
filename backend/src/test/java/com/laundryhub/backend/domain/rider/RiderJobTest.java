package com.laundryhub.backend.domain.rider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;
import com.laundryhub.backend.domain.order.TimeSlot;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.domain.pricing.PriceCalculator;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Rider;

import org.junit.jupiter.api.Test;

/** Step names and button labels must match DESIGN_SPEC.md (Rider app, R2/R3). */
class RiderJobTest {

    private final PricingSettings settings = PricingSettings.defaults();
    private final Soap standard = new Soap("Standard", "Everyday clean for regular clothes", BigDecimal.ZERO,
            "#E4EEF8", 1);

    private Order deliveryOrder() {
        ServiceType delivery = ServiceType.of(ServiceCode.PICKUP_DELIVERY, settings);
        LaundryLoad load = new LaundryLoad(new BigDecimal("9"));
        return new Order("LH-0008", new Customer("Carlo Mendoza", "carlo@example.com", "09170000008"),
                delivery, load, standard, new PriceCalculator(settings).calculate(load, standard, delivery),
                new TimeSlot(LocalDate.now(), "4–6 PM", LocalTime.of(16, 0), LocalTime.of(18, 0)),
                new Location(new BigDecimal("14.6"), new BigDecimal("121.0"), "Brgy. Mabini"));
    }

    private static List<String> titles(RiderJob job) {
        return job.getSteps().stream().map(JobStep::title).toList();
    }

    private static List<String> buttons(RiderJob job) {
        return job.getSteps().stream().map(JobStep::actionLabel).toList();
    }

    @Test
    void pickupStepsMatchTheDesign() {
        PickupJob job = new PickupJob(deliveryOrder());

        assertThat(job.getType().getLabel()).isEqualTo("Pickup");
        assertThat(titles(job)).containsExactly("Head to customer", "Arrived at pickup point",
                "Laundry picked up & weighed", "Dropped off at shop");
        assertThat(buttons(job)).containsExactly("Start trip", "I’ve arrived", "Confirm pickup", "Drop off at shop");
    }

    @Test
    void cashReturnDeliveryHasTheCashCollectedStep() {
        Order order = deliveryOrder();
        new CashPayment(order);
        DeliveryJob job = new DeliveryJob(order);

        assertThat(job.getType().getLabel()).isEqualTo("Return delivery");
        assertThat(titles(job)).containsExactly("Collect laundry at shop", "On the way to customer",
                "Arrived at drop-off", "Cash collected", "Delivered");
        assertThat(buttons(job)).containsExactly("Picked up at shop", "Start trip", "I’ve arrived",
                "Cash received", "Mark as delivered");
    }

    @Test
    void gcashReturnDeliverySkipsTheCashStep() {
        Order order = deliveryOrder();
        new GCashPayment(order, "09170001021");

        assertThat(titles(new DeliveryJob(order))).containsExactly("Collect laundry at shop",
                "On the way to customer", "Arrived at drop-off", "Delivered");
    }

    @Test
    void deliveryOrderCreatesAnAvailablePickupAndAWaitingReturn() {
        List<RiderJob> jobs = RiderJob.createFor(deliveryOrder());

        assertThat(jobs).extracting(RiderJob::getType).containsExactly(JobType.PICKUP, JobType.RETURN_DELIVERY);
        assertThat(jobs).extracting(RiderJob::getStatus).containsExactly(JobStatus.AVAILABLE, JobStatus.WAITING);
    }

    @Test
    void offDutyRiderCannotAccept() {
        Rider rider = new Rider("Rider 1", "rider1@example.com", "09170000001", "SMP 0001");

        assertThatThrownBy(() -> new PickupJob(deliveryOrder()).accept(rider))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cashReceivedMarksTheCashAsCollectedAndLastStepCompletesTheJob() {
        Order order = deliveryOrder();
        CashPayment cash = new CashPayment(order);
        DeliveryJob job = new DeliveryJob(order);
        Rider rider = new Rider("Rider 1", "rider1@example.com", "09170000001", "SMP 0001");
        rider.goOnDuty();
        job.offer();
        job.accept(rider);

        for (int step = 0; step < 4; step++) {
            job.completeCurrentStep(rider);
        }
        assertThat(cash.isPaid()).isTrue();
        assertThat(cash.getCollectedBy()).isSameAs(rider);
        assertThat(job.getCurrentStep().actionLabel()).isEqualTo("Mark as delivered");

        job.completeCurrentStep(rider);
        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(job.getCurrentStep()).isNull();
    }
}
