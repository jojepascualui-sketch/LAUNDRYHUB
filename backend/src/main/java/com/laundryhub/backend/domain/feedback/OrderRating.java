package com.laundryhub.backend.domain.feedback;

import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.user.Customer;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** A customer's 1–5 star rating of a completed order, with an optional comment. One per order; can be changed. */
@Entity
@Table(name = "order_ratings")
// OOP: Inheritance — an AuditedEntity (id and timestamps).
public class OrderRating extends AuditedEntity {

    /** Lowest rating. */
    public static final int MIN_STARS = 1;
    /** Highest rating. */
    public static final int MAX_STARS = 5;
    /** Longest comment, in characters. */
    public static final int MAX_COMMENT = 500;

    // OOP: Encapsulation — order and customer are fixed; stars and comment change only through change().
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, updatable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, updatable = false)
    private Customer customer;

    @Column(name = "stars", nullable = false)
    private int stars;

    @Column(name = "comment", length = MAX_COMMENT)
    private String comment;

    /** For JPA only. */
    protected OrderRating() {
    }

    /**
     * @param order    the order being rated
     * @param customer who rates; must own the order
     * @param stars    1–5
     * @param comment  optional text
     * @throws IllegalArgumentException if the customer does not own the order or a value is invalid
     * @throws IllegalStateException    if the order is not completed
     */
    public OrderRating(Order order, Customer customer, int stars, String comment) {
        this.order = Check.notNull(order, "Order");
        this.customer = Check.notNull(customer, "Customer");
        if (!order.getCustomer().getId().equals(customer.getId())) {
            throw new IllegalArgumentException("Customers can only rate their own orders");
        }
        if (order.getStatus() != OrderStatus.COMPLETED) {
            throw new IllegalStateException("You can rate an order once it is completed.");
        }
        change(stars, comment);
    }

    /**
     * @param newStars   1–5
     * @param newComment optional text, up to {@value #MAX_COMMENT} characters
     * @throws IllegalArgumentException if the stars are out of range or the comment is too long
     */
    public void change(int newStars, String newComment) {
        if (newStars < MIN_STARS || newStars > MAX_STARS) {
            throw new IllegalArgumentException("Stars must be between " + MIN_STARS + " and " + MAX_STARS);
        }
        this.stars = newStars;
        this.comment = Check.optionalText(newComment, "Comment", MAX_COMMENT);
    }

    /** @return the rated order */
    public Order getOrder() {
        return order;
    }

    /** @return who rated */
    public Customer getCustomer() {
        return customer;
    }

    /** @return 1–5 */
    public int getStars() {
        return stars;
    }

    /** @return the comment, or {@code null} */
    public String getComment() {
        return comment;
    }
}
