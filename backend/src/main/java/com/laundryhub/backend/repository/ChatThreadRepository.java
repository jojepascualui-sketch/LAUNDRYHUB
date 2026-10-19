package com.laundryhub.backend.repository;

import com.laundryhub.backend.domain.feedback.ChatThread;
import com.laundryhub.backend.domain.user.User;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Feedback conversations. */
// OOP: Abstraction — Spring Data generates the queries behind this interface.
public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {

    /**
     * @param user a customer or rider
     * @return their conversation, if they wrote before
     */
    Optional<ChatThread> findByUser(User user);

    /** @return conversations with at least one message, newest activity first */
    @Query("select t from ChatThread t join fetch t.user where t.lastMessageAt is not null order by t.lastMessageAt desc")
    List<ChatThread> findActiveNewestFirst();
}
