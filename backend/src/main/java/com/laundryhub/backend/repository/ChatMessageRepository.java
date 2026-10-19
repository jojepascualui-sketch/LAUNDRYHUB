package com.laundryhub.backend.repository;

import com.laundryhub.backend.domain.feedback.ChatMessage;
import com.laundryhub.backend.domain.feedback.ChatThread;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Feedback messages. */
// OOP: Abstraction — Spring Data generates the queries behind this interface.
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * @param thread  the conversation
     * @param afterId only messages with a larger id (polling)
     * @return new messages, oldest first
     */
    List<ChatMessage> findByThreadAndIdGreaterThanOrderByIdAsc(ChatThread thread, Long afterId);

    /**
     * @param thread the conversation
     * @param page   how many (newest first)
     * @return the latest messages, newest first
     */
    @Query("select m from ChatMessage m where m.thread = ?1 order by m.id desc")
    List<ChatMessage> findLatest(ChatThread thread, Pageable page);

    /**
     * @param thread the conversation
     * @return its newest message, if any
     */
    Optional<ChatMessage> findFirstByThreadOrderByIdDesc(ChatThread thread);
}
