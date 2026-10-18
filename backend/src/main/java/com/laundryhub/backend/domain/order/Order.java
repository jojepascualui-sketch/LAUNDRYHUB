package com.laundryhub.backend.domain.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.common.AuditedEntity;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.payment.PaymentMethod;
import com.laundryhub.backend.domain.pricing.PriceBreakdown;
import com.laundryhub.backend.domain.pricing.PriceCalculator;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.User;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A laundry order (screens 02–07, A1/A2). Prices are a snapshot of the server calculation
 * at the time the order was placed, so later price changes never alter old orders.
 */
@Entity(name = "LaundryOrder") // "Order" clashes with ORDER BY in queries
@Table(name = "orders")
public class Order extends AuditedEntity {

    /** Order numbers look like "LH-0001". */
    public static final Pattern ORDER_NO = Pattern.compile("^LH-\\d{4,}$");

    private static final int MAX_REASON_LENGTH = 255;

    // OOP: Encapsulation — every field is private; status and price change only through methods
    // that enforce the rules, and every status change is recorded in the history.
    @Column(name = "order_no", nullable = false, unique = true, length = 20)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "service_type", nullable = false, length = 20)
    private ServiceCode serviceCode;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "status", nullable = false, length = 20)
    private OrderStatus status;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = LaundryLoad.KG_SCALE)
    private BigDecimal weightKg;

    @Column(name = "billable_kg", nullable = false, precision = 6, scale = LaundryLoad.KG_SCALE)
    private BigDecimal billableKg;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soap_id", nullable = false)
    private Soap soap;

    @Column(name = "soap_name", nullable = false, length = Soap.MAX_NAME_LENGTH)
    private String soapName;

    @Column(name = "rate_per_kg", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal ratePerKg;

    @Column(name = "soap_add_on_per_kg", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal soapAddOnPerKg;

    @Column(name = "laundry_cost", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal laundryCost;

    @Column(name = "soap_cost", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal soapCost;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal deliveryFee;

    @Column(name = "total", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal total;

    @Embedded
    private TimeSlot schedule;

    @Embedded
    @AttributeOverrides({
        @AttributeOverride(name = "latitude", column = @Column(name = "pickup_latitude")),
        @AttributeOverride(name = "longitude", column = @Column(name = "pickup_longitude")),
        @AttributeOverride(name = "address", column = @Column(name = "pickup_address"))
    })
    private Location pickupLocation;

    @Column(name = "pickup_geocoded_address", length = Location.MAX_ADDRESS_LENGTH)
    private String pickupGeocodedAddress;

    @Column(name = "cancelled_reason", length = MAX_REASON_LENGTH)
    private String cancelledReason;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private PaymentMethod payment;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("changedAt ASC")
    private List<OrderStatusHistory> statusHistory = new ArrayList<>();

    /** For JPA only. */
    protected Order() {
    }

    /**
     * Places a new order in {@link OrderStatus#PENDING}.
     *
     * @param orderNo        order number, e.g. "LH-0001"
     * @param customer       who placed it
     * @param service        Walk-in or Pickup &amp; Delivery
     * @param load           the kilos
     * @param soap           the chosen soap
     * @param price          server-calculated breakdown for exactly this service, load and soap
     * @param schedule       pickup or drop-off time
     * @param pickupLocation pinned location; required for Pickup &amp; Delivery, ignored for Walk-in
     * @throws IllegalArgumentException if a value is missing or the price does not match the order
     */
    public Order(String orderNo, Customer customer, ServiceType service, LaundryLoad load, Soap soap,
                 PriceBreakdown price, TimeSlot schedule, Location pickupLocation) {
        if (orderNo == null || !ORDER_NO.matcher(orderNo).matches()) {
            throw new IllegalArgumentException("Order number must look like LH-0001");
        }
        Check.notNull(service, "Service");
        if (service.requiresPinnedLocation() && pickupLocation == null) {
            throw new IllegalArgumentException("Pin your location for Pickup & Delivery");
        }
        this.orderNo = orderNo;
        this.customer = Check.notNull(customer, "Customer");
        this.serviceCode = service.getCode();
        this.soap = Check.notNull(soap, "Fabric soap");
        this.schedule = Check.notNull(schedule, "Schedule");
        this.pickupLocation = service.requiresPinnedLocation() ? pickupLocation : null;
        applyPrice(load, price);
        this.status = OrderStatus.PENDING;
        statusHistory.add(new OrderStatusHistory(this, OrderStatus.PENDING, customer, null));
    }

    private void applyPrice(LaundryLoad load, PriceBreakdown price) {
        Check.notNull(load, "Laundry load");
        Check.notNull(price, "Price");
        if (price.serviceCode() != serviceCode
                || price.billableKg().compareTo(load.getBillableKg()) != 0
                || !price.soapName().equals(soapName != null ? soapName : soap.getName())) {
            throw new IllegalArgumentException("Price breakdown does not match this order");
        }
        this.weightKg = load.getWeightKg();
        this.billableKg = load.getBillableKg();
        this.soapName = price.soapName();
        this.ratePerKg = price.ratePerKg();
        this.soapAddOnPerKg = price.soapAddOnPerKg();
        this.laundryCost = price.laundryCost();
        this.soapCost = price.soapCost();
        this.deliveryFee = price.serviceFee();
        this.total = price.total();
    }

    /**
     * Stores the street address Google returns for the pinned point (reverse geocoding).
     *
     * @param address formatted address, or {@code null} if none was found
     * @throws IllegalStateException if the order has no pinned location (walk-in)
     */
    public void recordGeocodedAddress(String address) {
        if (pickupLocation == null) {
            throw new IllegalStateException("Walk-in orders have no pinned location");
        }
        this.pickupGeocodedAddress = Check.optionalText(address, "Geocoded address", Location.MAX_ADDRESS_LENGTH);
    }

    /**
     * Updates the kilos after weighing ("Laundry picked up &amp; weighed") with a fresh server price.
     *
     * @param measured the weighed load
     * @param price    new breakdown for the weighed load
     * @throws IllegalStateException if the order is no longer pending or is already paid
     */
    public void recordWeighedLoad(LaundryLoad measured, PriceBreakdown price) {
        if (status != OrderStatus.PENDING) {
            throw new IllegalStateException("Weight can only change while the order is pending");
        }
        if (payment != null && payment.isPaid()) {
            throw new IllegalStateException("Weight cannot change after payment");
        }
        applyPrice(measured, price);
        if (payment != null) {
            payment.changeAmount(total);
        }
    }

    /**
     * "Confirm pickup" with the weighed kilos: re-prices the order with the rates saved on it.
     *
     * @param measured the weighed load
     * @throws IllegalStateException if the order is no longer pending or is already paid
     */
    public void recordWeighedLoad(LaundryLoad measured) {
        recordWeighedLoad(measured, PriceCalculator.reprice(getPriceBreakdown(), measured));
    }

    /**
     * Moves the order to another status (A2 "Update status").
     *
     * @param next      new status
     * @param changedBy who changed it
     * @param note      optional note
     * @throws IllegalStateException if the order is final or the status does not apply to its service
     */
    public void changeStatus(OrderStatus next, User changedBy, String note) {
        Check.notNull(next, "Status");
        if (status.isFinal()) {
            throw new IllegalStateException("Order " + orderNo + " is already " + status.getLabel());
        }
        if (!next.appliesTo(serviceCode)) {
            throw new IllegalStateException(next.getLabel() + " does not apply to " + getServiceType());
        }
        if (next == status) {
            return;
        }
        status = next;
        statusHistory.add(new OrderStatusHistory(this, next, changedBy, note));
    }

    /**
     * Cancels the order.
     *
     * @param reason    why it was cancelled
     * @param changedBy who cancelled it
     */
    public void cancel(String reason, User changedBy) {
        String checkedReason = Check.text(reason, "Cancel reason", MAX_REASON_LENGTH);
        changeStatus(OrderStatus.CANCELLED, changedBy, checkedReason);
        this.cancelledReason = checkedReason;
    }

    /**
     * Links the payment created for this order. Called by the payment constructor.
     *
     * @param payment the order's payment
     * @throws IllegalStateException if the order already has a payment
     */
    public void attachPayment(PaymentMethod payment) {
        Check.notNull(payment, "Payment");
        if (this.payment != null && this.payment != payment) {
            throw new IllegalStateException("Order " + orderNo + " already has a payment");
        }
        if (payment.getOrder() != this) {
            throw new IllegalArgumentException("Payment belongs to another order");
        }
        this.payment = payment;
    }

    /** @return the service with the delivery fee saved on this order */
    // OOP: Polymorphism — callers get a WalkInService or PickupDeliveryService and use it through ServiceType.
    public ServiceType getServiceType() {
        return ServiceType.of(serviceCode, deliveryFee);
    }

    /** @return the price lines saved on this order */
    public PriceBreakdown getPriceBreakdown() {
        return new PriceBreakdown(serviceCode, getServiceType().getDisplayName(), billableKg, ratePerKg,
                laundryCost, soapName, soapAddOnPerKg, soapCost, deliveryFee, total);
    }

    /** @return order number, e.g. "LH-0001" */
    public String getOrderNo() {
        return orderNo;
    }

    /** @return the customer */
    public Customer getCustomer() {
        return customer;
    }

    /** @return the service code */
    public ServiceCode getServiceCode() {
        return serviceCode;
    }

    /** @return current status */
    public OrderStatus getStatus() {
        return status;
    }

    /** @return weighed or requested kilos */
    public BigDecimal getWeightKg() {
        return weightKg;
    }

    /** @return kilos charged */
    public BigDecimal getBillableKg() {
        return billableKg;
    }

    /** @return the chosen soap */
    public Soap getSoap() {
        return soap;
    }

    /** @return soap name at the time of ordering */
    public String getSoapName() {
        return soapName;
    }

    /** @return order total */
    public BigDecimal getTotal() {
        return total;
    }

    /** @return pickup or drop-off time */
    public TimeSlot getSchedule() {
        return schedule;
    }

    /** @return pinned location, or {@code null} for walk-in */
    public Location getPickupLocation() {
        return pickupLocation;
    }

    /** @return address Google found for the pin, or {@code null} */
    public String getPickupGeocodedAddress() {
        return pickupGeocodedAddress;
    }

    /** @return cancel reason, or {@code null} */
    public String getCancelledReason() {
        return cancelledReason;
    }

    /** @return the payment, or {@code null} before one is attached */
    public PaymentMethod getPayment() {
        return payment;
    }

    /** @return status changes, oldest first (read-only) */
    public List<OrderStatusHistory> getStatusHistory() {
        return Collections.unmodifiableList(statusHistory);
    }
}
