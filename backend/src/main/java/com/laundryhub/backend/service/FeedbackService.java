package com.laundryhub.backend.service;

import com.laundryhub.backend.domain.feedback.ChatMessage;
import com.laundryhub.backend.domain.feedback.ChatThread;
import com.laundryhub.backend.domain.feedback.MessageSide;
import com.laundryhub.backend.domain.feedback.OrderRating;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.User;
import com.laundryhub.backend.exception.ForbiddenException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.ChatMessageRepository;
import com.laundryhub.backend.repository.ChatThreadRepository;
import com.laundryhub.backend.repository.OrderRatingRepository;
import com.laundryhub.backend.web.dto.ChatMessageResponse;
import com.laundryhub.backend.web.dto.ChatResponse;
import com.laundryhub.backend.web.dto.ChatThreadResponse;
import com.laundryhub.backend.web.dto.RatingResponse;
import com.laundryhub.backend.web.dto.RatingSummaryResponse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Feedback chat (customers and riders ↔ shop) and order ratings. No design yet (CLAUDE.md §7); the apps
 * poll {@link #messagesFor} with the last message id they have.
 */
@Service
public class FeedbackService {

    /** Messages returned on the first load of a conversation. */
    public static final int HISTORY_SIZE = 200;
    /** Ratings in the console's "latest" list. */
    public static final int LATEST_RATINGS = 10;
    private static final int PREVIEW_LENGTH = 80;
    private static final int AVERAGE_DECIMALS = 1;
    private static final String ELLIPSIS = "\u2026";
    private static final String NO_CHAT = "Admin and staff answer feedback from the console.";

    // OOP: Encapsulation — repositories and lookups are private collaborators.
    private final AccountLookup accounts;
    private final OrderService orders;
    private final ChatThreadRepository threads;
    private final ChatMessageRepository messages;
    private final OrderRatingRepository ratings;

    /**
     * @param accounts signed-in user lookup
     * @param orders   order ownership checks
     * @param threads  conversations
     * @param messages chat messages
     * @param ratings  order ratings
     */
    public FeedbackService(AccountLookup accounts, OrderService orders, ChatThreadRepository threads,
                           ChatMessageRepository messages, OrderRatingRepository ratings) {
        this.accounts = accounts;
        this.orders = orders;
        this.threads = threads;
        this.messages = messages;
        this.ratings = ratings;
    }

    // ------------------------------------------------------------------ customer / rider

    /**
     * @param userId  signed-in customer or rider
     * @param afterId only newer messages (polling), or {@code null} for the latest history
     * @return the conversation; opening it marks the shop's replies as read
     */
    @Transactional
    public ChatResponse messagesFor(Long userId, Long afterId) {
        User user = chatUser(userId);
        Optional<ChatThread> thread = threads.findByUser(user);
        if (thread.isEmpty()) {
            return new ChatResponse(null, List.of());
        }
        thread.get().markReadBy(MessageSide.USER);
        return new ChatResponse(thread.get().getId(), load(thread.get(), afterId, MessageSide.USER));
    }

    /**
     * @param userId signed-in customer or rider
     * @param body   the message
     * @return the saved message
     */
    @Transactional
    public ChatMessageResponse send(Long userId, String body) {
        User user = chatUser(userId);
        ChatThread thread = threads.findByUser(user).orElseGet(() -> threads.save(new ChatThread(user)));
        ChatMessage message = messages.save(thread.post(user, body));
        return ChatMessageResponse.from(message, MessageSide.USER);
    }

    /**
     * @param userId signed-in customer or rider
     * @return shop replies not opened yet (for a badge)
     */
    @Transactional(readOnly = true)
    public int unreadFor(Long userId) {
        return threads.findByUser(chatUser(userId)).map(ChatThread::getUserUnread).orElse(0);
    }

    private User chatUser(Long userId) {
        User user = accounts.anyRole(userId);
        // OOP: Polymorphism — console users answer SHOP and cannot own a conversation.
        if (user.chatSide() != MessageSide.USER) {
            throw new ForbiddenException(NO_CHAT);
        }
        return user;
    }

    // ------------------------------------------------------------------ console

    /**
     * @param editorId signed-in admin or staff member
     * @return conversations with messages, newest activity first
     */
    @Transactional(readOnly = true)
    public List<ChatThreadResponse> threads(Long editorId) {
        accounts.consoleUser(editorId);
        List<ChatThreadResponse> list = new ArrayList<>();
        for (ChatThread thread : threads.findActiveNewestFirst()) {
            User user = thread.getUser();
            String last = messages.findFirstByThreadOrderByIdDesc(thread).map(ChatMessage::getBody).orElse("");
            list.add(new ChatThreadResponse(thread.getId(), user.getId(), user.getFullName(), user.getInitials(),
                    user.getRole(), preview(last), thread.getLastMessageAt(), thread.getShopUnread()));
        }
        return list;
    }

    /**
     * @param editorId signed-in admin or staff member
     * @param threadId conversation
     * @param afterId  only newer messages, or {@code null}
     * @return the messages; opening marks the user's messages as read by the shop
     */
    @Transactional
    public ChatResponse thread(Long editorId, Long threadId, Long afterId) {
        accounts.consoleUser(editorId);
        ChatThread thread = findThread(threadId);
        thread.markReadBy(MessageSide.SHOP);
        return new ChatResponse(thread.getId(), load(thread, afterId, MessageSide.SHOP));
    }

    /**
     * @param editorId signed-in admin or staff member
     * @param threadId conversation
     * @param body     the reply
     * @return the saved reply
     */
    @Transactional
    public ChatMessageResponse reply(Long editorId, Long threadId, String body) {
        ConsoleUser editor = accounts.consoleUser(editorId);
        ChatThread thread = findThread(threadId);
        ChatMessage message = messages.save(thread.post(editor, body));
        thread.markReadBy(MessageSide.SHOP);
        return ChatMessageResponse.from(message, MessageSide.SHOP);
    }

    /**
     * @param editorId signed-in admin or staff member
     * @return average, count and the latest ratings
     */
    @Transactional(readOnly = true)
    public RatingSummaryResponse ratingSummary(Long editorId) {
        accounts.consoleUser(editorId);
        Double average = ratings.averageStars();
        List<RatingResponse> latest = ratings.findLatest(PageRequest.of(0, LATEST_RATINGS)).stream()
                .map(RatingResponse::from).toList();
        return new RatingSummaryResponse(average == null ? null
                : BigDecimal.valueOf(average).setScale(AVERAGE_DECIMALS, RoundingMode.HALF_UP), ratings.count(), latest);
    }

    // ------------------------------------------------------------------ ratings (customer)

    /**
     * @param customerId signed-in customer
     * @param orderId    their order
     * @return the rating, or empty if not rated yet
     */
    @Transactional(readOnly = true)
    public Optional<RatingResponse> rating(Long customerId, Long orderId) {
        accounts.customer(customerId);
        Order order = orders.findOwned(customerId, orderId);
        return ratings.findByOrder(order).map(RatingResponse::from);
    }

    /**
     * Rates a completed order, or changes the earlier rating.
     *
     * @param customerId signed-in customer
     * @param orderId    their completed order
     * @param stars      1–5
     * @param comment    optional comment
     * @return the saved rating
     */
    @Transactional
    public RatingResponse rate(Long customerId, Long orderId, int stars, String comment) {
        Customer customer = accounts.customer(customerId);
        Order order = orders.findOwned(customerId, orderId);
        OrderRating rating = ratings.findByOrder(order).orElse(null);
        if (rating == null) {
            rating = ratings.save(new OrderRating(order, customer, stars, comment));
        } else {
            rating.change(stars, comment);
        }
        ratings.flush();
        return RatingResponse.from(rating);
    }

    // ------------------------------------------------------------------ helpers

    private ChatThread findThread(Long threadId) {
        return threads.findById(threadId).orElseThrow(() -> new NotFoundException("Conversation not found"));
    }

    private List<ChatMessageResponse> load(ChatThread thread, Long afterId, MessageSide viewer) {
        List<ChatMessage> list;
        if (afterId == null) {
            list = new ArrayList<>(messages.findLatest(thread, PageRequest.of(0, HISTORY_SIZE)));
            Collections.reverse(list);
        } else {
            list = messages.findByThreadAndIdGreaterThanOrderByIdAsc(thread, afterId);
        }
        return list.stream().map(m -> ChatMessageResponse.from(m, viewer)).toList();
    }

    private static String preview(String text) {
        return text.length() <= PREVIEW_LENGTH ? text : text.substring(0, PREVIEW_LENGTH) + ELLIPSIS;
    }
}
