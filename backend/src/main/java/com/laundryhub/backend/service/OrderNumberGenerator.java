package com.laundryhub.backend.service;

import java.util.List;

import com.laundryhub.backend.repository.OrderRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/**
 * Hands out order numbers LH-0001, LH-0002, ... Call it while holding the pricing-settings
 * row lock ({@code PricingSettingsRepository.lockSettings()}) so two orders never get the same number.
 */
@Component
public class OrderNumberGenerator {

    /** Prefix of every order number. */
    public static final String PREFIX = "LH-";

    /** Digits shown at least, e.g. 4 gives "LH-0001". */
    public static final int MIN_DIGITS = 4;

    private final OrderRepository orders;

    /**
     * @param orders order repository
     */
    public OrderNumberGenerator(OrderRepository orders) {
        this.orders = orders;
    }

    /** @return the next free order number */
    public String next() {
        List<String> highest = orders.findOrderNosHighestFirst(PageRequest.of(0, 1));
        long last = highest.isEmpty() ? 0 : Long.parseLong(highest.get(0).substring(PREFIX.length()));
        return PREFIX + String.format("%0" + MIN_DIGITS + "d", last + 1);
    }
}
