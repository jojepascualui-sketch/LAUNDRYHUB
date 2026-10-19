package com.laundryhub.backend.domain.feedback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.testsupport.Fixtures;

import java.lang.reflect.Field;

import org.junit.jupiter.api.Test;

/** Conversation rules (who may write, unread counters) and rating rules, without Spring. */
class FeedbackDomainTest {

    private static <T extends User> T withId(T user, long id) throws Exception {
        Field field = com.laundryhub.backend.domain.common.AuditedEntity.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(user, id);
        return user;
    }

    @Test
    void sideComesFromTheAccountType() {
        assertThat(Fixtures.customer().chatSide()).isEqualTo(MessageSide.USER);
        assertThat(Fixtures.rider("r@example.com", true).chatSide()).isEqualTo(MessageSide.USER);
        assertThat(Fixtures.admin().chatSide()).isEqualTo(MessageSide.SHOP);
        assertThat(Fixtures.staff().chatSide()).isEqualTo(MessageSide.SHOP);
    }

    @Test
    void ownerAndShopWriteAndCountersFollow() throws Exception {
        Customer owner = withId(Fixtures.customer(), 1);
        ChatThread thread = new ChatThread(owner);

        ChatMessage question = thread.post(owner, " Is the shop open on Sunday? ");
        assertThat(question.getSide()).isEqualTo(MessageSide.USER);
        assertThat(question.getBody()).isEqualTo("Is the shop open on Sunday?");
        assertThat(thread.getShopUnread()).isEqualTo(1);

        ChatMessage answer = thread.post(withId(Fixtures.staff(), 2), "Yes, 8 AM to 5 PM.");
        assertThat(answer.getSide()).isEqualTo(MessageSide.SHOP);
        assertThat(answer.getSenderName()).isEqualTo("Counter Staff");
        assertThat(thread.getUserUnread()).isEqualTo(1);
        assertThat(thread.getLastMessageAt()).isEqualTo(answer.getSentAt());

        thread.markReadBy(MessageSide.SHOP);
        thread.markReadBy(MessageSide.USER);
        assertThat(thread.getShopUnread()).isZero();
        assertThat(thread.getUserUnread()).isZero();
    }

    @Test
    void strangersAndConsoleOwnersAreRefused() throws Exception {
        ChatThread thread = new ChatThread(withId(Fixtures.customer(), 1));
        Customer other = withId(new Customer("Bea Cruz", "bea@example.com", null), 3);

        assertThatThrownBy(() -> thread.post(other, "Hi")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ChatThread(Fixtures.admin())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> thread.post(withId(Fixtures.admin(), 4), " "))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> thread.post(withId(Fixtures.admin(), 4), "x".repeat(ChatMessage.MAX_BODY + 1)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void ratingNeedsACompletedOwnOrderAndOneToFiveStars() throws Exception {
        PricingSettings settings = PricingSettings.defaults();
        Order order = Fixtures.walkInOrder("LH-0002", "8", Fixtures.standard(), settings);
        Customer owner = order.getCustomer();
        withId(owner, 1);

        assertThatThrownBy(() -> new OrderRating(order, owner, 5, null)).isInstanceOf(IllegalStateException.class);

        Field status = Order.class.getDeclaredField("status");
        status.setAccessible(true);
        status.set(order, com.laundryhub.backend.domain.order.OrderStatus.COMPLETED);

        OrderRating rating = new OrderRating(order, owner, 4, "  Fresh and folded  ");
        assertThat(rating.getStars()).isEqualTo(4);
        assertThat(rating.getComment()).isEqualTo("Fresh and folded");
        assertThatThrownBy(() -> rating.change(0, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> rating.change(6, null)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OrderRating(order, withId(new Customer("Bea Cruz", "bea@example.com", null), 9), 5,
                null)).isInstanceOf(IllegalArgumentException.class);
    }
}
