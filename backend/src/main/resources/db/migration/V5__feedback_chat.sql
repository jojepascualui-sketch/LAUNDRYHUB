-- Feedback chat (no design yet, CLAUDE.md §7): one conversation per customer or rider with the shop.
-- Admin and staff answer from the console. Customers can also rate a completed order 1–5 stars.

CREATE TABLE chat_threads (
    id               BIGINT      NOT NULL AUTO_INCREMENT,
    user_id          BIGINT      NOT NULL COMMENT 'The customer or rider this conversation belongs to',
    last_message_at  DATETIME(6) NULL,
    shop_unread      INT         NOT NULL DEFAULT 0 COMMENT 'Messages from the user the shop has not opened',
    user_unread      INT         NOT NULL DEFAULT 0 COMMENT 'Shop replies the user has not opened',
    created_at       DATETIME(6) NOT NULL,
    updated_at       DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_chat_threads_user UNIQUE (user_id),
    CONSTRAINT fk_chat_threads_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_chat_threads_unread CHECK (shop_unread >= 0 AND user_unread >= 0)
);
CREATE INDEX idx_chat_threads_last ON chat_threads (last_message_at);

CREATE TABLE chat_messages (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    thread_id    BIGINT        NOT NULL,
    sender_id    BIGINT        NULL COMMENT 'NULL when the sending staff account was deleted',
    sender_name  VARCHAR(120)  NOT NULL COMMENT 'Name at the time of sending',
    sender_side  VARCHAR(10)   NOT NULL,
    body         VARCHAR(1000) NOT NULL,
    sent_at      DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_chat_messages_thread FOREIGN KEY (thread_id) REFERENCES chat_threads (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE SET NULL,
    CONSTRAINT ck_chat_messages_side CHECK (sender_side IN ('USER', 'SHOP'))
);
CREATE INDEX idx_chat_messages_thread ON chat_messages (thread_id, id);

CREATE TABLE order_ratings (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    order_id     BIGINT       NOT NULL,
    customer_id  BIGINT       NOT NULL,
    stars        INT          NOT NULL,
    comment      VARCHAR(500) NULL,
    created_at   DATETIME(6)  NOT NULL,
    updated_at   DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_order_ratings_order UNIQUE (order_id),
    CONSTRAINT fk_order_ratings_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE,
    CONSTRAINT fk_order_ratings_customer FOREIGN KEY (customer_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_order_ratings_stars CHECK (stars BETWEEN 1 AND 5)
);
CREATE INDEX idx_order_ratings_created ON order_ratings (created_at);
