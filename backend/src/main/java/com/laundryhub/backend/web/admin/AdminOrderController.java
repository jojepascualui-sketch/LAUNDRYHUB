package com.laundryhub.backend.web.admin;

import java.util.List;

import com.laundryhub.backend.domain.order.OrderStatus;
import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.AdminOrderService;
import com.laundryhub.backend.service.DashboardService;
import com.laundryhub.backend.web.dto.AdminOrderDetailResponse;
import com.laundryhub.backend.web.dto.AdminOrderListResponse;
import com.laundryhub.backend.web.dto.AdminOrderUpdateRequest;
import com.laundryhub.backend.web.dto.ConfirmPaymentRequest;
import com.laundryhub.backend.web.dto.DashboardResponse;
import com.laundryhub.backend.web.dto.RiderOptionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** A1 Dashboard and A2 Orders (roles ADMIN and STAFF). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin orders", description = "A1 Dashboard · A2 Orders & status update")
public class AdminOrderController {

    private final DashboardService dashboard;
    private final AdminOrderService orders;

    /**
     * @param dashboard dashboard logic
     * @param orders    order management
     */
    public AdminOrderController(DashboardService dashboard, AdminOrderService orders) {
        this.dashboard = dashboard;
        this.orders = orders;
    }

    /** @return summary cards, pipeline and recent orders */
    @GetMapping("/dashboard")
    @Operation(summary = "A1 Dashboard")
    public DashboardResponse dashboard() {
        return dashboard.dashboard();
    }

    /**
     * @param status filter chip, or none for "All"
     * @param q      "Search orders" text (order no. or customer name)
     * @return chip counts and order cards
     */
    @GetMapping("/orders")
    @Operation(summary = "A2 Order list with status filter and search")
    public AdminOrderListResponse list(@RequestParam(required = false) OrderStatus status,
                                       @RequestParam(required = false) String q) {
        return orders.list(status, q);
    }

    /**
     * @param id order id
     * @return the detail panel
     */
    @GetMapping("/orders/{id}")
    @Operation(summary = "A2 Order detail panel")
    public AdminOrderDetailResponse detail(@PathVariable Long id) {
        return orders.detail(id);
    }

    /**
     * @param editorId the signed-in admin or staff
     * @param id       order id
     * @param request  new status and/or rider
     * @return the updated detail
     */
    @PatchMapping("/orders/{id}")
    @Operation(summary = "A2 Update status and/or assign / reassign rider")
    public AdminOrderDetailResponse update(@CurrentUserId Long editorId, @PathVariable Long id,
                                           @Valid @RequestBody AdminOrderUpdateRequest request) {
        return orders.update(editorId, id, request);
    }

    /**
     * @param editorId the signed-in admin or staff
     * @param id       order id
     * @param request  receipt or GCash reference
     * @return the updated detail
     */
    @PostMapping("/orders/{id}/payment")
    @Operation(summary = "A2 Mark payment received (cash at counter / GCash confirmed)")
    public AdminOrderDetailResponse confirmPayment(@CurrentUserId Long editorId, @PathVariable Long id,
                                                   @Valid @RequestBody(required = false) ConfirmPaymentRequest request) {
        return orders.confirmPayment(editorId, id, request == null ? null : request.referenceNo());
    }

    /** @return riders for the "Assign rider" dropdown */
    @GetMapping("/riders")
    @Operation(summary = "A2 Assign rider dropdown")
    public List<RiderOptionResponse> riders() {
        return orders.riderOptions();
    }
}
