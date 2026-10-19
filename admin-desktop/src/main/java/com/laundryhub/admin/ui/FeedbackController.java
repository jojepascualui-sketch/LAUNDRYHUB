package com.laundryhub.admin.ui;

import com.laundryhub.admin.api.dto.Chat;
import com.laundryhub.admin.api.dto.ChatMessage;
import com.laundryhub.admin.api.dto.ChatThread;
import com.laundryhub.admin.api.dto.RatingItem;
import com.laundryhub.admin.api.dto.RatingSummary;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.css.PseudoClass;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

/**
 * Feedback page (no design, CLAUDE.md §7): order ratings, the conversations with customers and riders, and
 * replies as the shop. Built from the A2 list/detail layout and existing tokens; refreshes every 5 s while
 * shown.
 */
// OOP: Inheritance — a PageController.
public class FeedbackController extends PageController {

    /** Refresh interval while the page is shown. */
    public static final Duration POLL = Duration.seconds(5);

    private static final double[] RATING_COLUMNS = {110, 200, 110, 400};
    private static final List<String> RATING_HEADERS = List.of("Order", "Customer", "Stars", "Comment");
    private static final String STAR = "\u2605";
    private static final String NO_STAR = "\u2606";
    private static final int MAX_STARS = 5;
    private static final String NO_RATINGS = "No ratings yet.";
    private static final String NO_CHATS = "No messages yet. Customers and riders write from the app.";
    private static final String RIDER = "RIDER";
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US);
    private static final PseudoClass SELECTED = PseudoClass.getPseudoClass("selected");

    @FXML
    private Label average;
    @FXML
    private Label ratingCount;
    @FXML
    private GridPane ratingTable;
    @FXML
    private VBox threadList;
    @FXML
    private VBox detail;
    @FXML
    private Label threadName;
    @FXML
    private VBox messages;
    @FXML
    private ScrollPane messageScroll;
    @FXML
    private TextField replyField;
    @FXML
    private Button send;

    // OOP: Encapsulation — page state and the poll timer stay private.
    private Long selectedId;
    private Long lastMessageId;
    private List<ChatThread> threads = List.of();
    private final Timeline poll = new Timeline(new KeyFrame(POLL, e -> pollNow()));

    @Override
    protected void onAttach() {
        Ui.columns(ratingTable, RATING_COLUMNS);
        poll.setCycleCount(Timeline.INDEFINITE);
        send.setOnAction(e -> sendReply());
        replyField.setOnAction(e -> sendReply());
        detail.setVisible(false);
    }

    @Override
    public Page page() {
        return Page.FEEDBACK;
    }

    // OOP: Polymorphism — the Feedback page's own refresh; it also starts the 5 s poll.
    @Override
    public void refresh() {
        call(api().ratings(), this::renderRatings);
        loadThreads();
        if (selectedId != null) {
            openThread(selectedId);
        }
        poll.playFromStart();
    }

    @Override
    public void onHide() {
        poll.stop();
    }

    private void pollNow() {
        loadThreads();
        if (selectedId != null) {
            long id = selectedId;
            call(api().feedbackMessages(id, lastMessageId), chat -> {
                if (Objects.equals(selectedId, id)) {
                    appendMessages(chat.messages());
                }
            }, message -> { });
        }
    }

    private void loadThreads() {
        call(api().feedbackThreads(), list -> {
            threads = list;
            renderThreads();
            // Like A2: the newest conversation opens when none is selected.
            if (selectedId == null && !threads.isEmpty()) {
                openThread(threads.get(0).id());
            }
        });
    }

    private void renderRatings(RatingSummary summary) {
        average.setText(summary.average() == null ? "\u2013" : summary.average().toPlainString() + " " + STAR);
        ratingCount.setText(summary.count() == 1 ? "from 1 rated order" : "from " + summary.count() + " rated orders");
        ratingTable.getChildren().clear();
        Ui.headerRow(ratingTable, true, RATING_HEADERS);
        if (summary.latest().isEmpty()) {
            Ui.emptyRow(ratingTable, 1, RATING_HEADERS.size(), NO_RATINGS);
        }
        int row = 1;
        for (RatingItem rating : summary.latest()) {
            List<Node> cells = new ArrayList<>();
            cells.add(Ui.label(rating.orderNo(), "strong"));
            cells.add(Ui.label(rating.customerName()));
            Label stars = Ui.label(STAR.repeat(rating.stars()) + NO_STAR.repeat(MAX_STARS - rating.stars()), "stars");
            stars.setAccessibleText(rating.stars() + " of " + MAX_STARS + " stars");
            cells.add(stars);
            cells.add(Ui.label(rating.comment() == null ? "" : rating.comment(), "muted"));
            Ui.bodyRow(ratingTable, row++, true, cells);
        }
    }

    private void renderThreads() {
        threadList.getChildren().clear();
        if (threads.isEmpty()) {
            Label empty = Ui.label(NO_CHATS, "empty-box");
            empty.setWrapText(true);
            empty.setMaxWidth(Double.MAX_VALUE);
            threadList.getChildren().add(empty);
            return;
        }
        for (ChatThread thread : threads) {
            threadList.getChildren().add(card(thread));
        }
    }

    private VBox card(ChatThread thread) {
        boolean rider = RIDER.equals(thread.role());
        HBox top = new HBox(8, Ui.label(thread.userName(), "order-card-id"),
                Ui.badge(rider ? "Rider" : "Customer", rider ? "status-out-for-delivery" : "status-washing"),
                Ui.grow());
        top.setAlignment(Pos.CENTER_LEFT);
        if (thread.unread() > 0) {
            Label unread = Ui.label(String.valueOf(thread.unread()), "unread-badge");
            unread.setAccessibleText(thread.unread() + " unread");
            top.getChildren().add(unread);
        }
        Label last = Ui.label(thread.lastMessage(), "small");
        last.setMaxWidth(Double.MAX_VALUE);
        VBox card = new VBox(top, last, Ui.label(format(thread.lastMessageAt()), "small"));
        card.getStyleClass().add("order-card");
        card.pseudoClassStateChanged(SELECTED, Objects.equals(thread.id(), selectedId));
        card.setOnMouseClicked(e -> openThread(thread.id()));
        card.setAccessibleText(thread.userName() + ", " + thread.lastMessage());
        return card;
    }

    private void openThread(long id) {
        boolean switching = !Objects.equals(selectedId, id);
        selectedId = id;
        if (switching) {
            lastMessageId = null;
            messages.getChildren().clear();
        }
        renderThreads();
        threads.stream().filter(t -> t.id() == id).findFirst()
                .ifPresent(t -> threadName.setText(t.userName() + (RIDER.equals(t.role()) ? " \u00B7 Rider" : " \u00B7 Customer")));
        detail.setVisible(true);
        call(api().feedbackMessages(id, lastMessageId), (Chat chat) -> {
            if (Objects.equals(selectedId, id)) {
                appendMessages(chat.messages());
                loadThreads();
            }
        });
    }

    private void appendMessages(List<ChatMessage> newer) {
        boolean added = false;
        for (ChatMessage message : newer) {
            if (lastMessageId == null || message.id() > lastMessageId) {
                messages.getChildren().add(bubble(message));
                lastMessageId = message.id();
                added = true;
            }
        }
        if (added) {
            Platform.runLater(() -> messageScroll.setVvalue(1.0));
        }
    }

    private static VBox bubble(ChatMessage message) {
        Label body = Ui.label(message.body(), message.mine() ? "bubble-mine" : "bubble-theirs");
        body.setWrapText(true);
        Label meta = Ui.label((message.mine() ? "" : message.senderName() + " \u00B7 ") + format(message.sentAt()),
                "small");
        VBox box = new VBox(4, body, meta);
        box.setAlignment(message.mine() ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        box.setFillWidth(false);
        return box;
    }

    private void sendReply() {
        String text = replyField.getText() == null ? "" : replyField.getText().trim();
        if (text.isEmpty() || selectedId == null) {
            return;
        }
        long id = selectedId;
        send.setDisable(true);
        call(api().reply(id, text), message -> {
            send.setDisable(false);
            replyField.clear();
            if (Objects.equals(selectedId, id)) {
                appendMessages(List.of(message));
            }
            loadThreads();
        }, error -> {
            send.setDisable(false);
            showError(error);
        });
    }

    private static String format(String isoTime) {
        if (isoTime == null) {
            return "";
        }
        try {
            return TIME.format(LocalDateTime.parse(isoTime));
        } catch (DateTimeParseException e) {
            return isoTime;
        }
    }
}
