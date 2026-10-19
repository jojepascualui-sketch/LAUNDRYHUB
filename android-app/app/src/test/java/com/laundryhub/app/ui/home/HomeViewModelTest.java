package com.laundryhub.app.ui.home;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.R;
import com.laundryhub.app.model.tracking.OrderSummary;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.ui.common.UiText;

import org.junit.Rule;
import org.junit.Test;

/** Home: prices load, and the Orders tab opens Track order for the newest order. */
public class HomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void ordersTabOpensTheNewestOrder() {
        FakeOrderRepository repo = new FakeOrderRepository();
        repo.myOrders.add(new OrderSummary(6, "LH-0006", "Pending"));
        repo.myOrders.add(new OrderSummary(1, "LH-0001", "Out for delivery"));
        HomeViewModel vm = new HomeViewModel(repo);

        vm.openLatestOrder();

        assertEquals(Long.valueOf(6), vm.getOpenTracking().getValue().take());
        assertNull(vm.getNotice().getValue());
    }

    @Test
    public void noOrdersSaysSo() {
        HomeViewModel vm = new HomeViewModel(new FakeOrderRepository());

        vm.openLatestOrder();

        assertEquals(UiText.of(R.string.no_orders_yet), vm.getNotice().getValue().take());
        assertNull(vm.getOpenTracking().getValue());
    }

    @Test
    public void refreshLoadsPricing() {
        FakeOrderRepository repo = new FakeOrderRepository();
        HomeViewModel vm = new HomeViewModel(repo);

        vm.refresh();

        assertEquals(repo.pricing, vm.getPricing().getValue());
    }
}
