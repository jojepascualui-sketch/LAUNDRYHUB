package com.laundryhub.app.ui.rider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.Observer;

import com.laundryhub.app.R;
import com.laundryhub.app.testing.FakeRiderRepository;
import com.laundryhub.app.ui.common.UiText;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;

/** R1/R5 behavior from source/Rider.dc.html: tabs, off duty, accept (incl. 409), decline. */
public class RiderHomeViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    private FakeRiderRepository repo;
    private RiderHomeViewModel vm;
    private final Observer<List<RiderListItem>> keepAlive = items -> { };

    @Before
    public void setUp() {
        repo = new FakeRiderRepository();
        vm = new RiderHomeViewModel(repo);
        vm.getItems().observeForever(keepAlive);
        vm.refresh(false);
    }

    private List<RiderListItem> items() {
        return vm.getItems().getValue();
    }

    @Test
    public void onDutyListsTheFourBookingsNearestFirst() {
        assertEquals(4, items().size());
        RiderListItem.Booking first = (RiderListItem.Booking) items().get(0);
        assertEquals("LH-0003", first.getJob().getOrderNo());
        assertFalse(first.isAccepted());
        assertEquals(4, vm.getSummary().getValue().availableCount());
    }

    @Test
    public void offDutyShowsOnlyTheOffDutyCardButKeepsTheCounters() {
        vm.setDuty(false);

        assertEquals(1, items().size());
        assertTrue(((RiderListItem.State) items().get(0)).isOffDuty());
        assertEquals(4, vm.getSummary().getValue().availableCount());
        vm.selectTab(RiderTab.MY_JOBS);
        assertTrue(((RiderListItem.State) items().get(0)).isOffDuty());
    }

    @Test
    public void myJobsStartsEmpty() {
        vm.selectTab(RiderTab.MY_JOBS);

        RiderListItem.State empty = (RiderListItem.State) items().get(0);
        assertFalse(empty.isOffDuty());
        assertEquals(R.string.empty_my_jobs, empty.getMessage());
    }

    @Test
    public void acceptOpensTheJobAndMovesItToMyJobs() {
        vm.accept(((RiderListItem.Booking) items().get(0)).getJob());

        assertEquals(Long.valueOf(FakeRiderRepository.ANA), vm.getOpenJob().getValue().take());
        assertEquals(3, items().size());
        assertEquals(1, vm.getSummary().getValue().acceptedCount());
        vm.selectTab(RiderTab.MY_JOBS);
        RiderListItem.Booking mine = (RiderListItem.Booking) items().get(0);
        assertTrue(mine.isAccepted());
        assertFalse(mine.getJob().isStarted());
    }

    @Test
    public void bookingTakenByAnotherRiderShowsAFriendlyMessage() {
        repo.takenByOthers.add(FakeRiderRepository.CARLO);

        vm.accept(((RiderListItem.Booking) items().get(1)).getJob());

        assertEquals(UiText.of(R.string.booking_taken), vm.getNotice().getValue().take());
        assertNull(vm.getOpenJob().getValue());
        assertEquals(3, items().size());
    }

    @Test
    public void declineHidesTheBooking() {
        vm.decline(((RiderListItem.Booking) items().get(0)).getJob());

        assertEquals(3, items().size());
        assertEquals("LH-0008", ((RiderListItem.Booking) items().get(0)).getJob().getOrderNo());
    }

    @Test
    public void emptyAvailableListSaysSo() {
        repo.available.clear();
        vm.refresh(false);

        assertEquals(R.string.empty_available, ((RiderListItem.State) items().get(0)).getMessage());
    }

    @Test
    public void loadErrorShowsBannerUntilTheNextGoodRefresh() {
        repo.failWith = 500;
        vm.refresh(true);
        assertEquals(UiText.of("Server error 500"), vm.getLoadError().getValue());
        assertFalse(vm.getRefreshing().getValue());

        repo.failWith = null;
        vm.refresh(false);
        assertNull(vm.getLoadError().getValue());
    }

    @Test
    public void expiredSessionSignsOut() {
        repo.failWith = RiderHomeViewModel.UNAUTHORIZED;
        vm.refresh(false);

        assertTrue(vm.getSessionExpired().getValue().take());
        assertNull(vm.getLoadError().getValue());
    }
}
