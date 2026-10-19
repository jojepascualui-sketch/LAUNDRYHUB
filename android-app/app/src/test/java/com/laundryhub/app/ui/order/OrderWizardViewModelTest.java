package com.laundryhub.app.ui.order;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.laundryhub.app.R;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.testing.ImmediateDebouncer;
import com.laundryhub.app.ui.common.UiText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.math.BigDecimal;

public class OrderWizardViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private FakeOrderRepository repo;
    private OrderWizardViewModel vm;
    private final Observer<Boolean> keepAlive = b -> { };

    @Before
    public void setUp() {
        repo = new FakeOrderRepository();
        vm = new OrderWizardViewModel(repo, new ImmediateDebouncer());
        vm.getCanContinue().observeForever(keepAlive);
        vm.start();
    }

    private OrderDraft draft() {
        return vm.getDraft().getValue();
    }

    private boolean canContinue() {
        return Boolean.TRUE.equals(vm.getCanContinue().getValue());
    }

    @Test
    public void startsWithMinimumKilosFirstSoapAndAServerTotal() {
        assertEquals(8, draft().getKg());
        assertEquals(Long.valueOf(FakeOrderRepository.STANDARD), draft().getSoapId());
        assertEquals(new BigDecimal("240.00"), vm.getPrice().getValue().total());
        assertTrue(vm.getPrice().getValue().fromServer());
        assertFalse("Continue disabled until a service is chosen", canContinue());
    }

    @Test
    public void minusStopsAtTheMinimum() {
        assertTrue(vm.isAtMinimum());
        vm.decreaseKg();
        assertEquals(8, draft().getKg());

        vm.increaseKg();
        vm.increaseKg();
        assertEquals(10, draft().getKg());
        assertFalse(vm.isAtMinimum());
    }

    @Test
    public void totalFollowsServiceKilosAndSoap() {
        vm.selectService(ServiceType.PICKUP_DELIVERY);
        assertEquals(new BigDecimal("300.00"), vm.getPrice().getValue().total());

        vm.increaseKg();
        vm.increaseKg();
        vm.selectSoap(FakeOrderRepository.HYPOALLERGENIC);
        assertEquals(new BigDecimal("410.00"), vm.getPrice().getValue().total());
    }

    @Test
    public void staleQuotesAreIgnored() {
        repo.holdQuotes = true;
        vm.increaseKg();      // quote A (9 kg)
        vm.increaseKg();      // quote B (10 kg)
        repo.heldQuotes.get(1).onSuccess(repo.serverLines(ServiceType.WALK_IN, 10, FakeOrderRepository.STANDARD));
        repo.heldQuotes.get(0).onSuccess(repo.serverLines(ServiceType.WALK_IN, 9, FakeOrderRepository.STANDARD));

        PriceLines shown = vm.getPrice().getValue();
        assertEquals(new BigDecimal("300.00"), shown.total());
    }

    @Test
    public void walkInFlowPlacesTheOrder() {
        vm.selectService(ServiceType.WALK_IN);
        assertTrue(canContinue());
        vm.next();
        assertEquals(WizardStep.LOAD, vm.getStep().getValue());
        vm.next();
        assertEquals(WizardStep.SCHEDULE, vm.getStep().getValue());
        assertFalse("needs a time chip", canContinue());
        SlotDto chip = vm.getSlots().getValue().get(2);
        vm.selectSlot(chip);
        assertTrue(canContinue());
        vm.next();
        assertEquals(WizardStep.REVIEW, vm.getStep().getValue());
        vm.next();

        assertNotNull(vm.getPlacedOrder().getValue());
        assertEquals("LH-0001", vm.getPlacedOrder().getValue().orderNo());
        assertEquals("Tomorrow, morning", repo.placedDraft.getSlot().text());
    }

    @Test
    public void changingTheServiceClearsTheChosenChip() {
        vm.selectService(ServiceType.WALK_IN);
        vm.selectSlot(vm.getSlots().getValue().get(0));
        vm.selectService(ServiceType.PICKUP_DELIVERY);

        assertNull(draft().getSlot());
        assertEquals("Today, 2–4 PM", vm.getSlots().getValue().get(0).getText());
    }

    @Test
    public void deliveryStaysOnStepThreeWithoutAPin() {
        vm.selectService(ServiceType.PICKUP_DELIVERY);
        vm.next();
        vm.next();
        vm.selectSlot(vm.getSlots().getValue().get(0));
        assertFalse(canContinue());

        vm.setPin(Location.of(14.61, 121.01));
        assertTrue(canContinue());
    }

    private void toDeliverySchedule() {
        vm.selectService(ServiceType.PICKUP_DELIVERY);
        vm.next();
        vm.next();
        vm.selectSlot(vm.getSlots().getValue().get(0));
    }

    @Test
    public void pinInsideTheRadiusFillsTheAddress() {
        toDeliverySchedule();
        vm.setPin(Location.of(14.61, 121.01));

        assertEquals(Boolean.TRUE, draft().getPinInside());
        assertEquals("Pinned near 14.610000,121.010000", draft().getAddressDetails());
        assertNull(vm.getPinError().getValue());
        assertFalse(vm.getCheckingPin().getValue());
        assertTrue(canContinue());

        // Moving the pin replaces an address that was filled in automatically.
        vm.setPin(Location.of(14.62, 121.02));
        assertEquals("Pinned near 14.620000,121.020000", draft().getAddressDetails());
    }

    @Test
    public void typedAddressDetailsAreKept() {
        toDeliverySchedule();
        vm.setAddressDetails("Blk 1 Lot 2, Rizal St.");
        vm.setPin(Location.of(14.61, 121.01));

        assertEquals("Blk 1 Lot 2, Rizal St.", draft().getAddressDetails());
    }

    @Test
    public void pinOutsideTheRadiusShowsAnErrorAndBlocksContinue() {
        toDeliverySchedule();
        vm.setPin(Location.of(14.70, 121.10));

        assertEquals(Boolean.FALSE, draft().getPinInside());
        assertEquals(UiText.format(R.string.pin_outside_radius, "15.5", "5"), vm.getPinError().getValue());
        assertFalse(canContinue());

        vm.setPin(Location.of(14.61, 121.01));
        assertNull(vm.getPinError().getValue());
        assertTrue(canContinue());
    }

    @Test
    public void onlyTheLatestPinCheckCounts() {
        toDeliverySchedule();
        repo.holdPinChecks = true;
        vm.setPin(Location.of(14.70, 121.10));   // far
        vm.setPin(Location.of(14.61, 121.01));   // near (latest)
        assertTrue(vm.getCheckingPin().getValue());
        repo.heldPinChecks.get(1).onSuccess(repo.pinCheck(Location.of(14.61, 121.01)));
        repo.heldPinChecks.get(0).onSuccess(repo.pinCheck(Location.of(14.70, 121.10)));

        assertEquals(Boolean.TRUE, draft().getPinInside());
        assertNull(vm.getPinError().getValue());
    }

    @Test
    public void failedPinCheckStillAllowsContinue() {
        toDeliverySchedule();
        repo.failPinCheck = ApiException.network(new IOException("offline"));
        vm.setPin(Location.of(14.61, 121.01));

        assertNull(draft().getPinInside());
        assertFalse(vm.getCheckingPin().getValue());
        assertTrue("the server checks the pin again when the order is placed", canContinue());
    }

    @Test
    public void deliveryOrderSendsThePin() {
        toDeliverySchedule();
        vm.setPin(Location.of(14.61, 121.01));
        vm.next();
        vm.next();

        assertEquals(Location.of(14.61, 121.01), repo.placedDraft.getPin());
    }

    @Test
    public void gcashNeedsANumberAndServerErrorsAreShown() {
        vm.selectService(ServiceType.WALK_IN);
        vm.next();
        vm.next();
        vm.selectSlot(vm.getSlots().getValue().get(0));
        vm.next();
        vm.selectPayment(PaymentOption.GCASH);
        assertFalse(canContinue());
        vm.setGcashNumber("0917 123 4567");
        assertTrue(canContinue());

        repo.failPlace = new ApiException(400, "GCash is not available right now", null);
        vm.next();
        assertEquals(UiText.of("GCash is not available right now"), vm.getPlaceError().getValue());
        assertNull(vm.getPlacedOrder().getValue());
        assertTrue("can try again", canContinue());
    }

    @Test
    public void backLeavesFromStepOne() {
        vm.selectService(ServiceType.WALK_IN);
        vm.next();
        vm.back();
        assertEquals(WizardStep.SERVICE, vm.getStep().getValue());
        assertNull(vm.getExit().getValue());
        vm.back();
        assertNotNull(vm.getExit().getValue().take());
    }

    @Test
    public void pricingFailureShowsAMessage() {
        FakeOrderRepository failing = new FakeOrderRepository();
        failing.failPricing = ApiException.network(new IOException("offline"));
        OrderWizardViewModel offline = new OrderWizardViewModel(failing, new ImmediateDebouncer());
        offline.start();

        assertEquals(UiText.of(R.string.error_network), offline.getLoadError().getValue());
        failing.failPricing = null;
        offline.retry();
        assertNull(offline.getLoadError().getValue());
        assertNotNull(offline.getPricing().getValue());
    }
}
