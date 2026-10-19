package com.laundryhub.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.order.InvalidWeightException;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.payment.PaymentChoice;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.RiderJobRepository;
import com.laundryhub.backend.repository.ShopSettingsRepository;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.service.geo.GeocodingService;
import com.laundryhub.backend.testsupport.Fixtures;
import com.laundryhub.backend.web.dto.CreateOrderRequest;
import com.laundryhub.backend.web.dto.OrderResponse;
import com.laundryhub.backend.web.dto.PinRequest;
import com.laundryhub.backend.web.dto.PriceBreakdownResponse;
import com.laundryhub.backend.web.dto.QuoteRequest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** OrderService: the server prices every order itself and enforces the minimum load and the radius. */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final long CUSTOMER_ID = 7L;
    private static final long SOAP_ID = 2L;
    private static final long MISSING_SOAP_ID = 99L;

    @Mock
    private OrderRepository orders;
    @Mock
    private SoapRepository soaps;
    @Mock
    private PricingSettingsRepository pricing;
    @Mock
    private RiderJobRepository jobs;
    @Mock
    private AccountLookup accounts;
    @Mock
    private GeocodingService geocoding;
    @Mock
    private OrderNumberGenerator orderNumbers;
    @Mock
    private ShopSettingsRepository shopSettings;
    @Captor
    private ArgumentCaptor<Order> savedOrder;
    @Captor
    private ArgumentCaptor<List<RiderJob>> savedJobs;

    private final PricingSettings settings = PricingSettings.defaults();
    private OrderService service;

    @BeforeEach
    void setUp() {
        DeliveryZoneService zone = new DeliveryZoneService(new LaundryHubProperties(null, null,
                new LaundryHubProperties.Shop(Fixtures.SHOP_LAT, Fixtures.SHOP_LNG)), shopSettings);
        service = new OrderService(orders, soaps, pricing, jobs, accounts, zone, geocoding, orderNumbers);
        lenient().when(accounts.customer(CUSTOMER_ID)).thenReturn(Fixtures.customer());
        lenient().when(soaps.findById(SOAP_ID)).thenReturn(Optional.of(Fixtures.hypoallergenic()));
        lenient().when(soaps.findById(MISSING_SOAP_ID)).thenReturn(Optional.empty());
        lenient().when(pricing.lockSettings()).thenReturn(settings);
        lenient().when(pricing.getSettings()).thenReturn(settings);
        lenient().when(orderNumbers.next()).thenReturn("LH-0001");
        lenient().when(geocoding.reverseGeocode(any(), any())).thenReturn(Optional.of("Rizal St, Poblacion"));
    }

    private static CreateOrderRequest delivery(String kg, PinRequest pin, PaymentChoice payment) {
        return new CreateOrderRequest(ServiceCode.PICKUP_DELIVERY, new BigDecimal(kg), SOAP_ID, LocalDate.now(),
                Fixtures.DELIVERY_SLOT, pin, "Blk 1 Lot 2", payment,
                payment == PaymentChoice.GCASH ? Fixtures.GCASH_NUMBER : null);
    }

    private static CreateOrderRequest walkIn(String kg, LocalDate date) {
        return new CreateOrderRequest(ServiceCode.WALK_IN, new BigDecimal(kg), SOAP_ID, date, Fixtures.WALK_IN_SLOT,
                near(), null, PaymentChoice.CASH, null);
    }

    private static PinRequest near() {
        return new PinRequest(new BigDecimal("14.61"), new BigDecimal("121.01"));
    }

    private static PinRequest far() {
        return new PinRequest(new BigDecimal("14.7"), new BigDecimal("121.1"));
    }

    @Test
    void placesADeliveryOrderAtTheServerPriceWithTwoJobs() {
        OrderResponse response = service.placeOrder(CUSTOMER_ID, delivery("10", near(), PaymentChoice.GCASH));

        verify(orders).save(savedOrder.capture());
        Order order = savedOrder.getValue();
        // CLAUDE.md example: 300 + 50 + 60.
        assertThat(order.getTotal()).isEqualTo(new BigDecimal("410.00"));
        assertThat(order.getPayment().getAmount()).isEqualTo(new BigDecimal("410.00"));
        assertThat(order.getPickupGeocodedAddress()).isEqualTo("Rizal St, Poblacion");
        assertThat(response.price().total()).isEqualTo(new BigDecimal("410.00"));
        assertThat(response.orderNo()).isEqualTo("LH-0001");
        verify(jobs).saveAll(savedJobs.capture());
        assertThat(savedJobs.getValue()).hasSize(2);
    }

    @Test
    void pricesWithTheCurrentAdminSettingsUnderTheLock() {
        settings.update(new BigDecimal("35"), new BigDecimal("8"), new BigDecimal("75"), new BigDecimal("5"), true,
                true, "09170009999", Fixtures.admin());

        service.placeOrder(CUSTOMER_ID, delivery("10", near(), PaymentChoice.CASH));

        verify(pricing).lockSettings();
        verify(orders).save(savedOrder.capture());
        // 10 × 35 + 10 × 5 + 75: the client never sends a price, so only the settings count.
        assertThat(savedOrder.getValue().getTotal()).isEqualTo(new BigDecimal("475.00"));
    }

    @Test
    void rejectsAnOrderBelowTheMinimumLoad() {
        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, walkIn("7.5", LocalDate.now())))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("Minimum of 8 kg per order");
        verify(orders, never()).save(any());
        verify(jobs, never()).saveAll(any());
    }

    @Test
    void usesTheAdminMinimumLoad() {
        settings.update(new BigDecimal("30"), new BigDecimal("10"), new BigDecimal("60"), new BigDecimal("5"), true,
                true, "09170009999", Fixtures.admin());

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, walkIn("9", LocalDate.now())))
                .isInstanceOf(InvalidWeightException.class)
                .hasMessage("Minimum of 10 kg per order");
    }

    @Test
    void rejectsAPinOutsideTheDeliveryRadius() {
        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, delivery("10", far(), PaymentChoice.CASH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("We deliver within 5 km.");
        verify(orders, never()).save(any());
    }

    @Test
    void deliveryNeedsAPin() {
        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, delivery("10", null, PaymentChoice.CASH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Pin your location on the map for Pickup & Delivery");
        verifyNoInteractions(geocoding);
    }

    @Test
    void walkInIgnoresThePinAndCreatesNoJobs() {
        service.placeOrder(CUSTOMER_ID, walkIn("8", LocalDate.now().plusDays(1)));

        verify(orders).save(savedOrder.capture());
        assertThat(savedOrder.getValue().getPickupLocation()).isNull();
        assertThat(savedOrder.getValue().getTotal()).isEqualTo(new BigDecimal("280.00"));
        verify(jobs).saveAll(savedJobs.capture());
        assertThat(savedJobs.getValue()).isEmpty();
        verifyNoInteractions(geocoding);
    }

    @Test
    void refusesADisabledPaymentMethod() {
        settings.update(new BigDecimal("30"), new BigDecimal("8"), new BigDecimal("60"), new BigDecimal("5"), true,
                false, null, Fixtures.admin());

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, delivery("10", near(), PaymentChoice.GCASH)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("GCash is not available right now");
    }

    @Test
    void scheduleMustBeWithinTheNextWeek() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDate tooLate = LocalDate.now().plusDays(OrderService.MAX_DAYS_AHEAD + 1);

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, walkIn("8", yesterday)))
                .hasMessage("Choose a time between today and 7 days from now");
        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, walkIn("8", tooLate)))
                .hasMessage("Choose a time between today and 7 days from now");
    }

    @Test
    void unknownSoapIsNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(ServiceCode.WALK_IN, new BigDecimal("8"), MISSING_SOAP_ID,
                LocalDate.now(), Fixtures.WALK_IN_SLOT, null, null, PaymentChoice.CASH, null);

        assertThatThrownBy(() -> service.placeOrder(CUSTOMER_ID, request)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void quoteUsesTheSameFormulaWithoutTakingTheLock() {
        PriceBreakdownResponse quote = service.quote(new QuoteRequest(ServiceCode.PICKUP_DELIVERY,
                new BigDecimal("10"), SOAP_ID));

        assertThat(quote.total()).isEqualTo(new BigDecimal("410.00"));
        verify(pricing, never()).lockSettings();
        assertThatThrownBy(() -> service.quote(new QuoteRequest(ServiceCode.WALK_IN, new BigDecimal("5"), SOAP_ID)))
                .isInstanceOf(InvalidWeightException.class);
    }

    @Test
    void ordersBelongToTheirCustomer() {
        when(orders.findByIdAndCustomerId(1L, CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(CUSTOMER_ID, 1L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Order not found");
    }
}
