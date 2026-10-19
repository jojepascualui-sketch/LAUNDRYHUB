package com.laundryhub.app.ui.track;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.model.tracking.Tracking;
import com.laundryhub.app.testing.FakeFeedbackRepository;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.testing.SampleTracking;

import org.junit.Rule;
import org.junit.Test;

/** 07: loading, timeline states, error banner and recovery on the next poll. */
public class TrackOrderViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void loadsTheTimelineAndStopsLoading() {
        FakeOrderRepository repo = new FakeOrderRepository();
        repo.tracking = SampleTracking.at(3);

        TrackOrderViewModel vm = new TrackOrderViewModel(repo, new FakeFeedbackRepository(), 1);

        assertFalse(vm.getLoading().getValue());
        assertNull(vm.getError().getValue());
        Tracking tracking = vm.getTracking().getValue();
        assertEquals("LH-0001", tracking.getOrderNo());
        assertEquals(Tracking.State.DONE, tracking.getRows().get(2).state());
        assertEquals(Tracking.State.CURRENT, tracking.getRows().get(3).state());
        assertTrue(tracking.isLive());
    }

    @Test
    public void errorShowsTheBannerAndPollingRecovers() {
        FakeOrderRepository repo = new FakeOrderRepository();
        TrackOrderViewModel vm = new TrackOrderViewModel(repo, new FakeFeedbackRepository(), 1);
        assertNotNull(vm.getError().getValue());
        assertFalse(vm.getLoading().getValue());

        repo.tracking = SampleTracking.at(1);
        vm.refresh();

        assertNull(vm.getError().getValue());
        assertEquals(Tracking.State.CURRENT, vm.getTracking().getValue().getRows().get(1).state());
    }

    @Test
    public void completedOrdersCanBeRatedOnce() {
        FakeOrderRepository repo = new FakeOrderRepository();
        FakeFeedbackRepository feedback = new FakeFeedbackRepository();
        repo.tracking = SampleTracking.at(5);
        TrackOrderViewModel vm = new TrackOrderViewModel(repo, feedback, 1);

        assertTrue(vm.getTracking().getValue().isCompleted());
        assertFalse(vm.getRating().getValue().isPresent());

        vm.rate(4, "  Fresh  ");

        assertEquals(4, vm.getRating().getValue().get().stars());
        assertEquals("Fresh", feedback.ratings.get(1L).comment());
    }

    @Test
    public void openOrdersAreNotRated() {
        FakeOrderRepository repo = new FakeOrderRepository();
        FakeFeedbackRepository feedback = new FakeFeedbackRepository();
        repo.tracking = SampleTracking.at(3);
        TrackOrderViewModel vm = new TrackOrderViewModel(repo, feedback, 1);

        vm.rate(5, null);

        assertNull(vm.getRating().getValue());
        assertTrue(feedback.ratings.isEmpty());
    }
}
