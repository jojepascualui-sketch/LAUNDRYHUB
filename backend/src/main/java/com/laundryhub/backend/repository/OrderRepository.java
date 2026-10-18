package com.laundryhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Laundry orders. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * @param orderNo e.g. "LH-0001"
     * @return the order with that number
     */
    Optional<Order> findByOrderNo(String orderNo);

    /**
     * @param status status filter (A2 chips)
     * @return orders with that status, newest first
     */
    List<Order> findByStatusOrderByCreatedAtDesc(OrderStatus status);

    /**
     * @param status a status
     * @return how many orders have it (A1 pipeline tiles)
     */
    long countByStatus(OrderStatus status);

    /** @return all orders, newest first (A1 Recent orders) */
    List<Order> findAllByOrderByCreatedAtDesc();

    /**
     * @param page how many to return
     * @return the newest orders (A1 Recent orders)
     */
    List<Order> findAllByOrderByCreatedAtDesc(Pageable page);

    /**
     * @param customerId the customer
     * @return the customer's orders, newest first (Orders tab)
     */
    List<Order> findByCustomerIdOrderByCreatedAtDesc(Long customerId);

    /**
     * @param id         order id
     * @param customerId the customer who must own it
     * @return the order, if it belongs to that customer
     */
    Optional<Order> findByIdAndCustomerId(Long id, Long customerId);

    /**
     * A2 list: optional status filter and a search on order number or customer name.
     *
     * @param status  status, or {@code null} for all
     * @param pattern lower-case LIKE pattern such as {@code %ana%}, or {@code null} for no search
     * @return matching orders, newest first
     */
    @Query("""
            select o from LaundryOrder o join fetch o.customer c
            where (:status is null or o.status = :status)
              and (:pattern is null or lower(o.orderNo) like :pattern or lower(c.fullName) like :pattern)
            order by o.createdAt desc
            """)
    List<Order> search(@Param("status") OrderStatus status, @Param("pattern") String pattern);

    /** @return rows of [status, count] for every status in use (A1 pipeline, A2 chips) */
    @Query("select o.status, count(o) from LaundryOrder o group by o.status")
    List<Object[]> countPerStatus();

    /**
     * Orders placed in a period, with customer and payment loaded (A1 today, A5 reports).
     *
     * @param from first moment, inclusive
     * @param to   end, exclusive
     * @return the orders, oldest first
     */
    @Query("""
            select o from LaundryOrder o join fetch o.customer left join fetch o.payment
            where o.createdAt >= :from and o.createdAt < :to
            order by o.createdAt asc
            """)
    List<Order> findPlacedBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /**
     * Order numbers, highest first ("LH-10000" sorts after "LH-9999" because longer comes first).
     *
     * @param page use {@code PageRequest.of(0, 1)} for the highest only
     * @return order numbers
     */
    @Query("select o.orderNo from LaundryOrder o order by length(o.orderNo) desc, o.orderNo desc")
    List<String> findOrderNosHighestFirst(Pageable page);

    /** @return rows of [customer id, order count] (A4 Customers "Orders" column) */
    @Query("select o.customer.id, count(o) from LaundryOrder o group by o.customer.id")
    List<Object[]> countPerCustomer();

    /**
     * @param soapId a soap
     * @return whether any order used it (then it cannot be deleted, only made unavailable)
     */
    boolean existsBySoapId(Long soapId);
}
