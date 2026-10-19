package com.laundryhub.app.ui.rider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.testing.FakeRiderRepository;

import org.junit.Rule;
import org.junit.Test;

/** R2/R3/R4: one tap per step, the cash step only for cash deliveries, then completed. */
public class ActiveJobViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void cashDeliveryWalksThroughEveryButtonLabel() {
        FakeRiderRepository repo = new FakeRiderRepository();
        repo.acceptAt(FakeRiderRepository.CARLO, 0);
        ActiveJobViewModel vm = new ActiveJobViewModel(repo, FakeRiderRepository.CARLO);

        String[] labels = {"Picked up at shop", "Start trip", "I’ve arrived", "Cash received", "Mark as delivered"};
        for (String label : labels) {
            assertEquals(label, vm.getJob().getValue().currentActionLabel());
            vm.completeStep();
        }
        assertTrue(vm.getJob().getValue().isCompleted());
        assertEquals("advance", repo.lastStepEndpoint);
        assertEquals(1, repo.doneToday);
    }

    @Test
    public void cashStepUsesCashCollected() {
        FakeRiderRepository repo = new FakeRiderRepository();
        repo.acceptAt(FakeRiderRepository.CARLO, FakeRiderRepository.CASH_STEP);
        ActiveJobViewModel vm = new ActiveJobViewModel(repo, FakeRiderRepository.CARLO);

        assertTrue(vm.getJob().getValue().isAwaitingCash());
        vm.completeStep();
        assertEquals("cash-collected", repo.lastStepEndpoint);
    }

    @Test
    public void gcashDeliveryHasNoCashStep() {
        FakeRiderRepository repo = new FakeRiderRepository();
        repo.acceptAt(FakeRiderRepository.MARIA, 0);
        ActiveJobViewModel vm = new ActiveJobViewModel(repo, FakeRiderRepository.MARIA);

        assertEquals(4, vm.getJob().getValue().getSteps().size());
        for (int i = 0; i < 4; i++) {
            vm.completeStep();
        }
        assertTrue(vm.getJob().getValue().isCompleted());
    }

    @Test
    public void failedStepShowsTheError() {
        FakeRiderRepository repo = new FakeRiderRepository();
        repo.acceptAt(FakeRiderRepository.ANA, 1);
        ActiveJobViewModel vm = new ActiveJobViewModel(repo, FakeRiderRepository.ANA);
        repo.failWith = 500;

        vm.completeStep();

        assertNotNull(vm.getError().getValue());
        assertEquals("I’ve arrived", vm.getJob().getValue().currentActionLabel());
    }
}
