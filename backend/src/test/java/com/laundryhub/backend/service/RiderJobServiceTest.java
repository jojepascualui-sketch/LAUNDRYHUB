package com.laundryhub.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.order.OrderStatusHistory;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.domain.rider.DeliveryJob;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.PickupJob;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.rider.RiderJobDecline;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.RiderJobDeclineRepository;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.ShopSettingsRepository;
import com.laundryhub.backend.testsupport.Fixtures;
import com.laundryhub.backend.web.dto.RiderJobResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

/** RiderJobService: duty check, one winner per booking, decline per rider and the R2/R3 step workflow. */
@ExtendWith(MockitoExtension.class)
class RiderJobServiceTest {

    private static final long ONE = 1L;
    private static final long TWO = 2L;
    private static final long JOB_ID = 10L;
    private static final int PICKUP_STEPS = 4;
    private static final int STEPS_BEFORE_CASH = 3;

    @Mock
    private AccountLookup accounts;
    @Mock
    private RiderJobRepository jobs;
    @Mock
    private RiderJobDeclineRepository declines;
    @Mock
    private PricingSettingsRepository pricing;
    @Mock
    private RiderService riders;
    @Mock
    private ShopSettingsRepository shopSettings;

    private final PricingSettings settings = PricingSettings.defaults();
    private final Admin admin = Fixtures.admin();
    private final Rider riderOne = Fixtures.rider("rider1@example.com", true);
    private final Rider riderTwo = Fixtures.rider("rider2@example.com", true);
    private RiderJobService service;

    @BeforeEach
    void setUp() {
        DeliveryZoneService zone = new DeliveryZoneService(new LaundryHubProperties(null, null,
                new LaundryHubProperties.Shop(Fixtures.SHOP_LAT, Fixtures.SHOP_LNG)), shopSettings);
        service = new RiderJobService(accounts, jobs, declines, pricing, riders, zone);
        lenient().when(accounts.rider(ONE)).thenReturn(riderOne);
        lenient().when(accounts.rider(TWO)).thenReturn(riderTwo);
        lenient().when(pricing.getSettings()).thenReturn(settings);
    }

    private Order order() {
        return Fixtures.deliveryOrder("LH-0001", "10", Fixtures.standard(), settings);
    }

    private PickupJob pickupFor(Order order) {
        PickupJob job = new PickupJob(order);
        lenient().when(jobs.findById(JOB_ID)).thenReturn(Optional.of(job));
        return job;
    }

    /** A return delivery that is on offer: the order went through washing to Ready. */
    private DeliveryJob readyDeliveryFor(Order order) {
        order.changeStatus(OrderStatus.WASHING, admin, null);
        order.changeStatus(OrderStatus.READY, admin, null);
        DeliveryJob job = new DeliveryJob(order);
        job.offer();
        lenient().when(jobs.findById(JOB_ID)).thenReturn(Optional.of(job));
        return job;
    }

    private void assignedTo(long riderId, RiderJob job) {
        when(jobs.findByIdAndRiderId(JOB_ID, riderId)).thenReturn(Optional.of(job));
    }

    // ---- accept / decline -------------------------------------------------------------------

    @Test
    void offDutyRiderCannotAccept() {
        PickupJob job = pickupFor(order());
        riderOne.goOffDuty();

        assertThatThrownBy(() -> service.accept(ONE, JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Go on duty to accept bookings.");
        assertThat(job.getStatus()).isEqualTo(JobStatus.AVAILABLE);
        verify(jobs, never()).saveAndFlush(any());
    }

    @Test
    void acceptAssignsTheJobAndShowsTheCustomerNumber() {
        PickupJob job = pickupFor(order());

        RiderJobResponse response = service.accept(ONE, JOB_ID);

        assertThat(job.getStatus()).isEqualTo(JobStatus.ACCEPTED);
        assertThat(job.getRider()).isSameAs(riderOne);
        assertThat(response.customerMobile()).isEqualTo(Fixtures.CUSTOMER_MOBILE);
        assertThat(response.currentActionLabel()).isEqualTo("Start trip");
        verify(jobs).saveAndFlush(job);
    }

    @Test
    void secondRiderToAcceptGetsAConflict() {
        PickupJob job = pickupFor(order());
        service.accept(ONE, JOB_ID);

        assertThatThrownBy(() -> service.accept(TWO, JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage(RiderJobService.ALREADY_TAKEN);
        assertThat(job.getRider()).isSameAs(riderOne);
    }

    @Test
    void losingTheDatabaseRaceIsAlsoAConflict() {
        PickupJob job = pickupFor(order());
        when(jobs.saveAndFlush(job)).thenThrow(new ObjectOptimisticLockingFailureException(RiderJob.class, JOB_ID));

        assertThatThrownBy(() -> service.accept(ONE, JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage(RiderJobService.ALREADY_TAKEN);
    }

    @Test
    void unknownJobIsNotFound() {
        when(jobs.findById(JOB_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.accept(ONE, JOB_ID)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void declineIsRecordedForThatRiderOnly() {
        PickupJob job = pickupFor(order());
        ArgumentCaptor<RiderJobDecline> decline = ArgumentCaptor.forClass(RiderJobDecline.class);

        service.decline(TWO, JOB_ID);

        verify(declines).save(decline.capture());
        assertThat(decline.getValue().getRider()).isSameAs(riderTwo);
        assertThat(decline.getValue().getJob()).isSameAs(job);
        assertThat(job.getStatus()).isEqualTo(JobStatus.AVAILABLE);
        // The other rider can still take it.
        service.accept(ONE, JOB_ID);
        assertThat(job.getRider()).isSameAs(riderOne);
    }

    @Test
    void aTakenJobCannotBeDeclined() {
        pickupFor(order());
        service.accept(ONE, JOB_ID);

        assertThatThrownBy(() -> service.decline(TWO, JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This booking is no longer available.");
        verify(declines, never()).save(any());
    }

    @Test
    void waitingReturnDeliveryCannotBeAcceptedYet() {
        DeliveryJob waiting = new DeliveryJob(order());
        when(jobs.findById(JOB_ID)).thenReturn(Optional.of(waiting));

        assertThatThrownBy(() -> service.accept(ONE, JOB_ID)).isInstanceOf(ConflictException.class);
    }

    // ---- steps --------------------------------------------------------------------------------

    @Test
    void pickupRunsThroughEveryStepAndStartsTheWash() {
        Order order = order();
        PickupJob job = pickupFor(order);
        service.accept(ONE, JOB_ID);
        assignedTo(ONE, job);

        for (int step = 1; step < PICKUP_STEPS; step++) {
            service.advance(ONE, JOB_ID, null);
            assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        }
        RiderJobResponse done = service.advance(ONE, JOB_ID, null);

        assertThat(done.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(done.currentActionLabel()).isNull();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.WASHING);
        assertThat(order.getStatusHistory()).last().extracting(OrderStatusHistory::getNote)
                .isEqualTo("Pickup: Dropped off at shop");
    }

    @Test
    void weightIsAcceptedOnlyWithConfirmPickupAndBillsTheMinimum() {
        Order order = order();
        PickupJob job = pickupFor(order);
        service.accept(ONE, JOB_ID);
        assignedTo(ONE, job);

        assertThatThrownBy(() -> service.advance(ONE, JOB_ID, new BigDecimal("9")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The weight can only be sent with \"Confirm pickup\".");
        service.advance(ONE, JOB_ID, null);
        service.advance(ONE, JOB_ID, null);
        RiderJobResponse weighed = service.advance(ONE, JOB_ID, new BigDecimal("5"));

        assertThat(order.getWeightKg()).isEqualByComparingTo("5");
        assertThat(weighed.billableKg()).isEqualByComparingTo("8");
        assertThat(weighed.total()).isEqualTo(new BigDecimal("300.00"));
        assertThat(weighed.currentActionLabel()).isEqualTo("Drop off at shop");
    }

    @Test
    void cashReturnDeliveryWaitsForTheCash() {
        Order order = order();
        CashPayment cash = new CashPayment(order);
        DeliveryJob job = readyDeliveryFor(order);
        service.accept(ONE, JOB_ID);
        assignedTo(ONE, job);

        service.advance(ONE, JOB_ID, null);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.OUT_FOR_DELIVERY);
        RiderJobResponse atCash = null;
        for (int step = 1; step < STEPS_BEFORE_CASH; step++) {
            atCash = service.advance(ONE, JOB_ID, null);
        }
        assertThat(atCash.awaitingCash()).isTrue();
        assertThat(atCash.currentActionLabel()).isEqualTo("Cash received");
        assertThat(atCash.payment().collectCash()).isTrue();

        assertThatThrownBy(() -> service.advance(ONE, JOB_ID, null))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Confirm the cash first with \"Cash received\".");
        RiderJobResponse paid = service.cashCollected(ONE, JOB_ID);
        assertThat(cash.isPaid()).isTrue();
        assertThat(cash.getCollectedBy()).isSameAs(riderOne);
        assertThat(paid.currentActionLabel()).isEqualTo("Mark as delivered");

        RiderJobResponse done = service.advance(ONE, JOB_ID, null);
        assertThat(done.status()).isEqualTo(JobStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void gcashReturnDeliveryHasNoCashStep() {
        Order order = order();
        new GCashPayment(order, Fixtures.GCASH_NUMBER);
        DeliveryJob job = readyDeliveryFor(order);
        service.accept(ONE, JOB_ID);
        assignedTo(ONE, job);

        assertThat(job.getSteps()).hasSize(DeliveryJob.PREPAID_STEPS.size());
        assertThatThrownBy(() -> service.cashCollected(ONE, JOB_ID))
                .isInstanceOf(ConflictException.class)
                .hasMessage("This job is not waiting for cash.");
        for (int step = 0; step < DeliveryJob.PREPAID_STEPS.size(); step++) {
            assertThat(job.isAwaitingCash()).isFalse();
            service.advance(ONE, JOB_ID, null);
        }

        assertThat(job.getStatus()).isEqualTo(JobStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void cashCannotBeConfirmedOnAPickup() {
        PickupJob job = pickupFor(order());
        service.accept(ONE, JOB_ID);
        assignedTo(ONE, job);

        assertThatThrownBy(() -> service.cashCollected(ONE, JOB_ID)).isInstanceOf(ConflictException.class);
    }

    @Test
    void onlyTheAssignedRiderCanAdvance() {
        when(jobs.findByIdAndRiderId(JOB_ID, TWO)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.advance(TWO, JOB_ID, null))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Job not found");
    }

    @Test
    void offDutyRidersSeeNoBookings() {
        riderOne.goOffDuty();

        assertThat(service.available(ONE)).isEmpty();
        verify(jobs, never()).findOpenFor(any(), any());
    }
}
