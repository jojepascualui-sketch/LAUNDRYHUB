package com.laundryhub.backend.repository;

import com.laundryhub.backend.domain.feedback.OrderRating;
import com.laundryhub.backend.domain.order.Order;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Order ratings. */
// OOP: Abstraction — Spring Data generates the queries behind this interface.
public interface OrderRatingRepository extends JpaRepository<OrderRating, Long> {

    /**
     * @param order an order
     * @return its rating, if the customer rated it
     */
    Optional<OrderRating> findByOrder(Order order);

    /** @return the average stars, or {@code null} when there are no ratings */
    @Query("select avg(r.stars) from OrderRating r")
    Double averageStars();

    /**
     * @param page how many
     * @return the latest ratings with order and customer
     */
    @Query("select r from OrderRating r join fetch r.order join fetch r.customer order by r.updatedAt desc")
    List<OrderRating> findLatest(Pageable page);
}
