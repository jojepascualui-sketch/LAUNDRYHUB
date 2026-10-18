package com.laundryhub.backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;
import com.laundryhub.backend.domain.order.TimeSlot;
import com.laundryhub.backend.domain.pricing.PriceBreakdown;
import com.laundryhub.backend.domain.pricing.PriceCalculator;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.service.geo.GeocodingService;
import com.laundryhub.backend.web.dto.CreateOrderRequest;
import com.laundryhub.backend.web.dto.GeocodeResponse;
import com.laundryhub.backend.web.dto.OrderResponse;
import com.laundryhub.backend.web.dto.OrderSummaryResponse;
import com.laundryhub.backend.web.dto.PinRequest;
import com.laundryhub.backend.web.dto.PriceBreakdownResponse;
import com.laundryhub.backend.web.dto.QuoteRequest;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The customer order flow (02–06) and the Orders tab. The server recalculates every price:
 * nothing the app sends about money is trusted.
 */
@Service
public class OrderService {

    /** How many days ahead a pickup or drop-off may be booked. */
    public static final int MAX_DAYS_AHEAD = 7;

    private final OrderRepository orders;
    private final SoapRepository soaps;
    private final PricingSettingsRepository pricing;
    private final RiderJobRepository jobs;
    private final AccountLookup accounts;
    private final DeliveryZoneService zone;
    private final GeocodingService geocoding;
    private final OrderNumberGenerator orderNumbers;

    /**
     * @param orders       orders
     * @param soaps        soaps
     * @param pricing      pricing settings
     * @param jobs         rider jobs
     * @param accounts     signed-in account lookup
     * @param zone         delivery radius check
     * @param geocoding    reverse geocoding
     * @param orderNumbers order number sequence
     */
    public OrderService(OrderRepository orders, SoapRepository soaps, PricingSettingsRepository pricing,
                        RiderJobRepository jobs, AccountLookup accounts, DeliveryZoneService zone,
                        GeocodingService geocoding, OrderNumberGenerator orderNumbers) {
        this.orders = orders;
        this.soaps = soaps;
        this.pricing = pricing;
        this.jobs = jobs;
        this.accounts = accounts;
        this.zone = zone;
        this.geocoding = geocoding;
        this.orderNumbers = orderNumbers;
    }

    /**
     * The authoritative price for the "Estimated total" bar and the 05 "Order summary".
     *
     * @param request service, kilos and soap
     * @return the breakdown
     * @throws IllegalArgumentException if the kilos are below the minimum or the soap is unavailable
     */
    @Transactional(readOnly = true)
    public PriceBreakdownResponse quote(QuoteRequest request) {
        PricingSettings settings = pricing.getSettings();
        return PriceBreakdownResponse.from(price(settings, request.serviceType(), request.weightKg(),
                findSoap(request.soapId())));
    }

    private static PriceBreakdown price(PricingSettings settings, ServiceCode code, BigDecimal weightKg, Soap soap) {
        // OOP: Polymorphism — the ServiceType subclass decides the fee.
        ServiceType service = ServiceType.of(code, settings);
        LaundryLoad load = new LaundryLoad(weightKg, settings.getMinKg());
        return new PriceCalculator(settings).calculate(load, soap, service);
    }

    /**
     * 05 "Place order". Validates kilos (minimum load), soap, time chip, pin (required for
     * delivery and inside the delivery radius) and payment method, then saves the order with the
     * server price, its payment, and the PICKUP and RETURN_DELIVERY rider jobs for delivery orders.
     *
     * @param customerId the signed-in customer
     * @param request    the order form
     * @return the placed order (06)
     */
    @Transactional
    public OrderResponse placeOrder(Long customerId, CreateOrderRequest request) {
        Customer customer = accounts.customer(customerId);
        Soap soap = findSoap(request.soapId());
        boolean needsPin = request.serviceType() == ServiceCode.PICKUP_DELIVERY;
        Location pin = needsPin ? pinOf(request) : null;
        // Ask Google before taking the lock, so a slow answer does not hold up other orders.
        String geocoded = pin == null ? null
                : geocoding.reverseGeocode(pin.getLatitude(), pin.getLongitude()).orElse(null);

        PricingSettings settings = pricing.lockSettings();
        ServiceType service = ServiceType.of(request.serviceType(), settings);
        LaundryLoad load = new LaundryLoad(request.weightKg(), settings.getMinKg());
        PriceBreakdown price = new PriceCalculator(settings).calculate(load, soap, service);
        TimeSlot schedule = schedule(service, request.scheduleDate(), request.slotLabel());
        if (pin != null) {
            zone.requireInsideRadius(pin, settings);
        }
        if (!request.paymentMethod().isEnabledIn(settings)) {
            throw new IllegalArgumentException(request.paymentMethod().getDisplayName() + " is not available right now");
        }

        Order order = new Order(orderNumbers.next(), customer, service, load, soap, price, schedule, pin);
        if (pin != null) {
            order.recordGeocodedAddress(geocoded);
        }
        // OOP: Polymorphism — PaymentChoice builds a CashPayment or a GCashPayment.
        request.paymentMethod().attachTo(order, request.gcashNumber());
        orders.save(order);
        jobs.saveAll(RiderJob.createFor(order));
        return OrderResponse.from(order, LocalDate.now());
    }

    private static Location pinOf(CreateOrderRequest request) {
        if (request.pin() == null) {
            throw new IllegalArgumentException("Pin your location on the map for Pickup & Delivery");
        }
        return new Location(request.pin().latitude(), request.pin().longitude(), request.addressDetails());
    }

    private static TimeSlot schedule(ServiceType service, LocalDate date, String slotLabel) {
        LocalDate today = LocalDate.now();
        if (date.isBefore(today) || date.isAfter(today.plusDays(MAX_DAYS_AHEAD))) {
            throw new IllegalArgumentException("Choose a time between today and " + MAX_DAYS_AHEAD + " days from now");
        }
        return service.findSlot(slotLabel).withDate(date);
    }

    private Soap findSoap(Long soapId) {
        return soaps.findById(soapId).orElseThrow(() -> new NotFoundException("Fabric soap not found"));
    }

    /**
     * 04a: address for the dropped pin and whether it is inside the delivery radius.
     *
     * @param pin the point
     * @return address and distance
     */
    @Transactional(readOnly = true)
    public GeocodeResponse checkPin(PinRequest pin) {
        PricingSettings settings = pricing.getSettings();
        Location point = new Location(pin.latitude(), pin.longitude(), null);
        Location shop = zone.findShopLocation();
        BigDecimal distance = shop == null ? null : DeliveryZoneService.roundKm(shop.distanceKmTo(point));
        boolean inside = distance != null && distance.compareTo(settings.getDeliveryRadiusKm()) <= 0;
        String address = geocoding.reverseGeocode(point.getLatitude(), point.getLongitude()).orElse(null);
        return new GeocodeResponse(point.getLatitude(), point.getLongitude(), address, distance,
                settings.getDeliveryRadiusKm(), inside);
    }

    /**
     * Orders tab.
     *
     * @param customerId the signed-in customer
     * @return their orders, newest first
     */
    @Transactional(readOnly = true)
    public List<OrderSummaryResponse> mine(Long customerId) {
        accounts.customer(customerId);
        LocalDate today = LocalDate.now();
        return orders.findByCustomerIdOrderByCreatedAtDesc(customerId).stream()
                .map(order -> OrderSummaryResponse.from(order, today))
                .toList();
    }

    /**
     * One of the customer's orders.
     *
     * @param customerId the signed-in customer
     * @param orderId    order id
     * @return the order
     * @throws NotFoundException if it does not exist or belongs to someone else
     */
    @Transactional(readOnly = true)
    public OrderResponse get(Long customerId, Long orderId) {
        accounts.customer(customerId);
        return OrderResponse.from(findOwned(customerId, orderId), LocalDate.now());
    }

    /**
     * @param customerId the signed-in customer
     * @param orderId    order id
     * @return the order, if it is theirs
     * @throws NotFoundException otherwise (no hint that another customer's order exists)
     */
    Order findOwned(Long customerId, Long orderId) {
        return orders.findByIdAndCustomerId(orderId, customerId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
    }
}
