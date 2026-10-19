package com.laundryhub.backend.testsupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;
import com.laundryhub.backend.domain.order.TimeSlot;
import com.laundryhub.backend.domain.pricing.PriceCalculator;
import com.laundryhub.backend.domain.user.Admin;
import com.laundryhub.backend.domain.user.Customer;
import com.laundryhub.backend.domain.user.Rider;
import com.laundryhub.backend.domain.user.Staff;

/**
 * Plain domain objects for unit tests (no Spring, no database). Values follow the design
 * defaults in CLAUDE.md §2: ₱30/kg, ₱60 delivery fee, 8 kg minimum, shop at 14.6, 121.0.
 */
public final class Fixtures {

    /** Shop latitude, the same as {@code application-test.yml}. */
    public static final BigDecimal SHOP_LAT = new BigDecimal("14.600000");

    /** Shop longitude, the same as {@code application-test.yml}. */
    public static final BigDecimal SHOP_LNG = new BigDecimal("121.000000");

    /** Mobile number used for every test customer. */
    public static final String CUSTOMER_MOBILE = "09170000001";

    /** A valid GCash number. */
    public static final String GCASH_NUMBER = "09170001021";

    /** The "Today, 2–4 PM" delivery chip. */
    public static final String DELIVERY_SLOT = "2–4 PM";

    /** The "morning" walk-in chip. */
    public static final String WALK_IN_SLOT = "morning";

    private static final LocalTime SLOT_START = LocalTime.of(14, 0);
    private static final LocalTime SLOT_END = LocalTime.of(16, 0);

    private Fixtures() {
    }

    /** @return the shop position */
    public static Location shop() {
        return new Location(SHOP_LAT, SHOP_LNG, null);
    }

    /** @return a pin about 1.5 km from the shop */
    public static Location nearPin() {
        return new Location(new BigDecimal("14.61"), new BigDecimal("121.01"), "Blk 1 Lot 2, Rizal St.");
    }

    /** @return a pin about 15 km from the shop, outside the default 5 km radius */
    public static Location farPin() {
        return new Location(new BigDecimal("14.7"), new BigDecimal("121.1"), "Far away");
    }

    /** @return the "Standard" soap (₱0/kg, shown "Included") */
    public static Soap standard() {
        return new Soap("Standard", "Everyday clean for regular clothes", BigDecimal.ZERO, "#E4EEF8", 1);
    }

    /** @return the "Hypoallergenic" soap (+₱5/kg) */
    public static Soap hypoallergenic() {
        return new Soap("Hypoallergenic", "Gentle on sensitive skin", new BigDecimal("5"), "#E6F2EA", 2);
    }

    /** @return the "Premium Scented" soap (+₱8/kg) */
    public static Soap premiumScented() {
        return new Soap("Premium Scented", "Long-lasting fresh fragrance", new BigDecimal("8"), "#F3E8F7", 3);
    }

    /** @return the "Baby-Safe" soap (+₱6/kg) */
    public static Soap babySafe() {
        return new Soap("Baby-Safe", "Mild and dye-free formula", new BigDecimal("6"), "#FFF1E8", 4);
    }

    /** @return a customer named Ana Reyes */
    public static Customer customer() {
        return new Customer("Ana Reyes", "ana@example.com", CUSTOMER_MOBILE);
    }

    /**
     * @param email  e-mail, also used to make the plate number unique
     * @param onDuty duty switch
     * @return a rider
     */
    public static Rider rider(String email, boolean onDuty) {
        Rider rider = new Rider("Rider " + email, email, "09180000002", "SMP " + Math.abs(email.hashCode() % 10000));
        if (onDuty) {
            rider.goOnDuty();
        }
        return rider;
    }

    /** @return the shop owner (may change prices and accounts) */
    public static Admin admin() {
        return new Admin("Admin Owner", "owner@laundryhub.test", Admin.DEFAULT_POSITION);
    }

    /** @return a counter staff member (may not change prices or accounts) */
    public static Staff staff() {
        return new Staff("Counter Staff", "counter@laundryhub.test", "Counter staff");
    }

    /** @return the "Today, 2–4 PM" slot */
    public static TimeSlot todaySlot() {
        return new TimeSlot(LocalDate.now(), DELIVERY_SLOT, SLOT_START, SLOT_END);
    }

    /**
     * A Pickup &amp; Delivery order priced by the real calculator, pinned near the shop.
     *
     * @param orderNo  order number, e.g. LH-0001
     * @param kg       kilos (at least the minimum)
     * @param soap     fabric soap
     * @param settings pricing settings
     * @return a pending order without a payment
     */
    public static Order deliveryOrder(String orderNo, String kg, Soap soap, PricingSettings settings) {
        ServiceType service = ServiceType.of(ServiceCode.PICKUP_DELIVERY, settings);
        LaundryLoad load = new LaundryLoad(new BigDecimal(kg), settings.getMinKg());
        return new Order(orderNo, customer(), service, load, soap,
                new PriceCalculator(settings).calculate(load, soap, service), todaySlot(), nearPin());
    }

    /**
     * A Walk-in order priced by the real calculator.
     *
     * @param orderNo  order number, e.g. LH-0002
     * @param kg       kilos (at least the minimum)
     * @param soap     fabric soap
     * @param settings pricing settings
     * @return a pending order without a payment
     */
    public static Order walkInOrder(String orderNo, String kg, Soap soap, PricingSettings settings) {
        ServiceType service = ServiceType.of(ServiceCode.WALK_IN, settings);
        LaundryLoad load = new LaundryLoad(new BigDecimal(kg), settings.getMinKg());
        return new Order(orderNo, customer(), service, load, soap,
                new PriceCalculator(settings).calculate(load, soap, service),
                TimeSlot.partOfDay(LocalDate.now().plusDays(1), WALK_IN_SLOT), null);
    }
}
