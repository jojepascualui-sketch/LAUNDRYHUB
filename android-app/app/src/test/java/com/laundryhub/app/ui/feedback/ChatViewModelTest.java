package com.laundryhub.app.ui.feedback;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;

import com.laundryhub.app.testing.FakeFeedbackRepository;

import org.junit.Rule;
import org.junit.Test;

/** Feedback chat: history, sending, polling only newer messages, errors. */
public class ChatViewModelTest {

    @Rule
    public InstantTaskExecutorRule instant = new InstantTaskExecutorRule();

    @Test
    public void loadsHistoryThenPollsOnlyNewerMessages() {
        FakeFeedbackRepository repo = new FakeFeedbackRepository();
        repo.shopReplies("Welcome to Laundry HUB!");
        ChatViewModel vm = new ChatViewModel(repo);

        assertFalse(vm.getLoading().getValue());
        assertNull(repo.lastAfter);
        assertEquals(1, vm.getMessages().getValue().size());

        repo.shopReplies("Your laundry is ready.");
        vm.refresh();

        assertEquals(Long.valueOf(1), repo.lastAfter);
        assertEquals(2, vm.getMessages().getValue().size());
        assertFalse(vm.getMessages().getValue().get(1).mine());
    }

    @Test
    public void sendingTrimsAppendsAndClearsTheInput() {
        FakeFeedbackRepository repo = new FakeFeedbackRepository();
        ChatViewModel vm = new ChatViewModel(repo);

        vm.send("   ");
        assertTrue(vm.getMessages().getValue().isEmpty());

        vm.send("  Thank you!  ");
        assertEquals("Thank you!", vm.getMessages().getValue().get(0).body());
        assertTrue(vm.getMessages().getValue().get(0).mine());
        assertNotNull(vm.getSent().getValue().take());

        // The next poll does not duplicate the sent message.
        vm.refresh();
        assertEquals(1, vm.getMessages().getValue().size());
    }

    @Test
    public void errorsShowTheBannerAndKeepMessages() {
        FakeFeedbackRepository repo = new FakeFeedbackRepository();
        repo.shopReplies("Hi");
        ChatViewModel vm = new ChatViewModel(repo);
        repo.failWith = 500;

        vm.send("Hello");

        assertNotNull(vm.getError().getValue());
        assertEquals(1, vm.getMessages().getValue().size());
        assertFalse(vm.getSending().getValue());
    }
}
