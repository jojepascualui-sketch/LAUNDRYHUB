package com.laundryhub.backend.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.domain.payment.CashPayment;
import com.laundryhub.backend.domain.payment.GCashPayment;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.JobType;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.user.AccountStatus;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.RiderRepository;
import com.laundryhub.backend.web.dto.AdminJobResponse;
import com.laundryhub.backend.web.dto.AdminOrderDetailResponse;
import com.laundryhub.backend.web.dto.AdminOrderListResponse;
import com.laundryhub.backend.web.dto.AdminOrderRow;
import com.laundryhub.backend.web.dto.AdminOrderUpdateRequest;
import com.laundryhub.backend.web.dto.LocationResponse;
import com.laundryhub.backend.web.dto.PaymentResponse;
import com.laundryhub.backend.web.dto.PriceBreakdownResponse;
import com.laundryhub.backend.web.dto.RiderOptionResponse;
import com.laundryhub.backend.web.dto.ScheduleResponse;
import com.laundryhub.backend.web.dto.StatusCount;
import com.laundryhub.backend.web.dto.StatusHistoryResponse;

import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A2 Orders &amp; status update: list with filter chips, detail panel, "Update status",
 * "Assign rider" and payment confirmation. Status changes keep the rider jobs in step:
 * Ready offers the return delivery to riders, Cancelled cancels both jobs.
 */
@Service
public class AdminOrderService {

    private final OrderRepository orders;
    private final RiderJobRepository jobs;
    private final RiderRepository riders;
    private final AccountLookup accounts;

    /**
     * @param orders   orders
     * @param jobs     rider jobs
     * @param riders   riders
     * @param accounts signed-in account lookup
     */
    public AdminOrderService(OrderRepository orders, RiderJobRepository jobs, RiderRepository riders,
                             AccountLookup accounts) {
        this.orders = orders;
        this.jobs = jobs;
        this.riders = riders;
        this.accounts = accounts;
    }

    /**
     * A2 list and chips.
     *
     * @param status chip filter, or {@code null} for "All"
     * @param search text in order number or customer name (the header "Search orders"), or {@code null}
     * @return counts and matching orders, newest first
     */
    @Transactional(readOnly = true)
    public AdminOrderListResponse list(OrderStatus status, String search) {
        List<StatusCount> counts = countPerStatus();
        long total = counts.stream().mapToLong(StatusCount::count).sum();
        String pattern = search == null || search.isBlank() ? null : "%" + search.trim().toLowerCase() + "%";
        List<AdminOrderRow> rows = orders.search(status, pattern).stream().map(AdminOrderRow::from).toList();
        return new AdminOrderListResponse(total, counts, rows);
    }

    /** @return one entry per status, in workflow order, including zero counts */
    @Transactional(readOnly = true)
    public List<StatusCount> countPerStatus() {
        Map<OrderStatus, Long> counted = new EnumMap<>(OrderStatus.class);
        for (Object[] row : orders.countPerStatus()) {
            counted.put((OrderStatus) row[0], (Long) row[1]);
        }
        return Arrays.stream(OrderStatus.values())
                .map(s -> new StatusCount(s, s.getLabel(), counted.getOrDefault(s, 0L)))
                .toList();
    }

    /**
     * A2 detail panel.
     *
     * @param orderId order id
     * @return the detail
     */
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse detail(Long orderId) {
        return toDetail(find(orderId));
    }

    /**
     * A2 "Update status" and/or "Assign rider".
     *
     * @param editorId the signed-in admin or staff member
     * @param orderId  order id
     * @param request  new status and/or rider
     * @return the updated detail
     */
    @Transactional
    public AdminOrderDetailResponse update(Long editorId, Long orderId, AdminOrderUpdateRequest request) {
        ConsoleUser editor = accounts.consoleUser(editorId);
        Order order = find(orderId);
        List<RiderJob> orderJobs = jobs.findByOrderId(orderId);
        if (request.status() != null) {
            changeStatus(order, orderJobs, request, editor);
        }
        if (request.riderId() != null) {
            Rider rider = riders.findById(request.riderId())
                    .orElseThrow(() -> new NotFoundException("Rider not found"));
            jobToAssign(orderJobs, request.jobType()).assignTo(rider);
        }
        try {
            jobs.saveAllAndFlush(orderJobs);
        } catch (ObjectOptimisticLockingFailureException e) {
            throw new ConflictException("A rider changed this job at the same time. Please refresh.");
        }
        return toDetail(order);
    }

    private static void changeStatus(Order order, List<RiderJob> orderJobs, AdminOrderUpdateRequest request,
                                     ConsoleUser editor) {
        if (request.status() == OrderStatus.CANCELLED) {
            order.cancel(request.cancelReason(), editor);
            orderJobs.forEach(RiderJob::cancel);
            return;
        }
        order.changeStatus(request.status(), editor, null);
        if (request.status() == OrderStatus.READY) {
            // Clean laundry is ready: offer the return delivery to on-duty riders.
            orderJobs.stream()
                    .filter(job -> job.getType() == JobType.RETURN_DELIVERY && job.getStatus() == JobStatus.WAITING)
                    .forEach(RiderJob::offer);
        }
    }

    /** The job the dropdown applies to: the given type, else the pickup until it is done, then the return. */
    private static RiderJob jobToAssign(List<RiderJob> orderJobs, JobType type) {
        if (orderJobs.isEmpty()) {
            throw new ConflictException("Walk-in orders have no rider jobs.");
        }
        return orderJobs.stream()
                .filter(job -> type == null ? !isFinished(job) : job.getType() == type)
                .min((a, b) -> a.getType().compareTo(b.getType()))
                .orElseThrow(() -> new ConflictException("This order has no open rider job."));
    }

    private static boolean isFinished(RiderJob job) {
        return job.getStatus() == JobStatus.COMPLETED || job.getStatus() == JobStatus.CANCELLED;
    }

    /**
     * Marks the payment as received from the console: cash at the counter, or a GCash transfer
     * (A2 "(paid)/(unpaid)").
     *
     * @param editorId    the signed-in admin or staff member
     * @param orderId     order id
     * @param referenceNo receipt number (optional for cash) or GCash reference (required)
     * @return the updated detail
     */
    @Transactional
    public AdminOrderDetailResponse confirmPayment(Long editorId, Long orderId, String referenceNo) {
        accounts.consoleUser(editorId);
        Order order = find(orderId);
        PaymentMethod payment = order.getPayment();
        if (payment == null) {
            throw new ConflictException("This order has no payment.");
        }
        // OOP: Polymorphism — each payment subclass records "paid" its own way.
        if (payment instanceof GCashPayment gcash) {
            gcash.confirm(referenceNo);
        } else if (payment instanceof CashPayment cash) {
            cash.recordPaidAtCounter(referenceNo);
        }
        return toDetail(order);
    }

    /**
     * A2 "Assign rider" dropdown.
     *
     * @return every active rider with duty and workload
     */
    @Transactional(readOnly = true)
    public List<RiderOptionResponse> riderOptions() {
        return riders.findAllByOrderByFullNameAsc().stream()
                .filter(r -> r.getStatus() == AccountStatus.ACTIVE)
                .map(r -> RiderOptionResponse.from(r, jobs.countByRiderAndStatusIn(r, RiderService.ACTIVE_STATUSES)))
                .toList();
    }

    private Order find(Long orderId) {
        return orders.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
    }

    private AdminOrderDetailResponse toDetail(Order order) {
        List<OrderStatus> options = Arrays.stream(OrderStatus.values())
                .filter(s -> s != OrderStatus.CANCELLED && s.appliesTo(order.getServiceCode()))
                .toList();
        List<AdminJobResponse> orderJobs = jobs.findByOrderId(order.getId()).stream()
                .sorted((a, b) -> a.getType().compareTo(b.getType()))
                .map(AdminJobResponse::from)
                .toList();
        return new AdminOrderDetailResponse(AdminOrderRow.from(order), order.getSoapName(),
                ScheduleResponse.from(order.getSchedule(), LocalDate.now()),
                LocationResponse.from(order.getPickupLocation(), order.getPickupGeocodedAddress()),
                PriceBreakdownResponse.from(order.getPriceBreakdown()), PaymentResponse.from(order.getPayment()),
                options, orderJobs, order.getStatusHistory().stream().map(StatusHistoryResponse::from).toList());
    }
}
