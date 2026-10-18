package com.laundryhub.backend.web;

import java.net.URI;
import java.util.List;

import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.CatalogService;
import com.laundryhub.backend.service.OrderService;
import com.laundryhub.backend.service.TrackingService;
import com.laundryhub.backend.web.dto.CreateOrderRequest;
import com.laundryhub.backend.web.dto.OrderResponse;
import com.laundryhub.backend.web.dto.OrderSummaryResponse;
import com.laundryhub.backend.web.dto.PriceBreakdownResponse;
import com.laundryhub.backend.web.dto.QuoteRequest;
import com.laundryhub.backend.web.dto.ScheduleResponse;
import com.laundryhub.backend.web.dto.TrackingResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Customer order flow 02–07 and the Orders tab (role CUSTOMER). */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Customer orders", description = "04a/04b time chips · 05 Review & pay · 06 Order placed · 07 Track order")
public class CustomerOrderController {

    private final OrderService orders;
    private final TrackingService tracking;
    private final CatalogService catalog;

    /**
     * @param orders   order logic
     * @param tracking tracking logic
     * @param catalog  time chips
     */
    public CustomerOrderController(OrderService orders, TrackingService tracking, CatalogService catalog) {
        this.orders = orders;
        this.tracking = tracking;
        this.catalog = catalog;
    }

    /**
     * @param service WALK_IN or PICKUP_DELIVERY
     * @return the four time chips with dates
     */
    @GetMapping("/slots")
    @Operation(summary = "04a Pickup time / 04b Drop-off time chips")
    public List<ScheduleResponse> slots(@RequestParam ServiceCode service) {
        return catalog.slots(service);
    }

    /**
     * @param request service, kilos, soap
     * @return the server price breakdown
     */
    @PostMapping("/quote")
    @Operation(summary = "Estimated total / 05 Order summary (server price)")
    public PriceBreakdownResponse quote(@Valid @RequestBody QuoteRequest request) {
        return orders.quote(request);
    }

    /**
     * @param customerId the signed-in customer
     * @param request    the order form
     * @return 201 with the placed order (06)
     */
    @PostMapping
    @Operation(summary = "05 Place order")
    public ResponseEntity<OrderResponse> place(@CurrentUserId Long customerId,
                                               @Valid @RequestBody CreateOrderRequest request) {
        OrderResponse order = orders.placeOrder(customerId, request);
        return ResponseEntity.created(URI.create("/api/orders/" + order.id())).body(order);
    }

    /**
     * @param customerId the signed-in customer
     * @return their orders, newest first
     */
    @GetMapping("/mine")
    @Operation(summary = "Orders tab")
    public List<OrderSummaryResponse> mine(@CurrentUserId Long customerId) {
        return orders.mine(customerId);
    }

    /**
     * @param customerId the signed-in customer
     * @param id         order id
     * @return the order
     */
    @GetMapping("/{id}")
    @Operation(summary = "06 Order placed / order detail")
    public OrderResponse get(@CurrentUserId Long customerId, @PathVariable Long id) {
        return orders.get(customerId, id);
    }

    /**
     * @param customerId the signed-in customer
     * @param id         order id
     * @return timeline, status history, rider card and latest rider location
     */
    @GetMapping("/{id}/tracking")
    @Operation(summary = "07 Track order (poll every few seconds for live GPS)")
    public TrackingResponse tracking(@CurrentUserId Long customerId, @PathVariable Long id) {
        return tracking.track(customerId, id);
    }
}
