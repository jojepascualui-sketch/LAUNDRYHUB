package com.laundryhub.backend.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.laundryhub.backend.config.LaundryHubProperties;
import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.exception.ServiceUnavailableException;
import com.laundryhub.backend.repository.ShopSettingsRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Where the shop is and whether a pin is inside the delivery radius. Distance is the
 * Haversine great-circle distance ({@link Location#distanceKmTo}).
 */
@Service
public class DeliveryZoneService {

    /** Decimal places shown for distances, e.g. "1.2 km". */
    public static final int DISTANCE_SCALE = 1;

    // OOP: Encapsulation — callers ask for the shop position; where it comes from stays private.
    private final LaundryHubProperties.Shop configuredShop;
    private final ShopSettingsRepository shopSettings;

    /**
     * @param properties   {@code SHOP_LAT} / {@code SHOP_LNG}
     * @param shopSettings fallback: the position saved in shop_settings
     */
    public DeliveryZoneService(LaundryHubProperties properties, ShopSettingsRepository shopSettings) {
        this.configuredShop = properties.shop();
        this.shopSettings = shopSettings;
    }

    /**
     * The shop position: {@code SHOP_LAT}/{@code SHOP_LNG} if set, else the position saved in shop_settings.
     *
     * @return the shop position, or {@code null} if neither is set
     */
    @Transactional(readOnly = true)
    public Location findShopLocation() {
        if (configuredShop.hasPosition()) {
            return new Location(configuredShop.lat(), configuredShop.lng(), null);
        }
        return shopSettings.getSettings().getLocation();
    }

    /**
     * @return the shop position
     * @throws ServiceUnavailableException if no shop position is configured
     */
    @Transactional(readOnly = true)
    public Location requireShopLocation() {
        Location shop = findShopLocation();
        if (shop == null) {
            throw new ServiceUnavailableException("The shop location is not set (SHOP_LAT / SHOP_LNG), "
                    + "so delivery pins cannot be checked");
        }
        return shop;
    }

    /**
     * @param pin a customer pin
     * @return distance from the shop in km, rounded to 0.1
     */
    @Transactional(readOnly = true)
    public BigDecimal distanceFromShopKm(Location pin) {
        return roundKm(requireShopLocation().distanceKmTo(Check.notNull(pin, "Pin")));
    }

    /**
     * Rejects pins outside the admin's delivery radius (A3 "Delivery radius").
     *
     * @param pin      the customer pin
     * @param settings current pricing settings
     * @return the distance from the shop in km
     * @throws IllegalArgumentException if the pin is too far
     */
    @Transactional(readOnly = true)
    public BigDecimal requireInsideRadius(Location pin, PricingSettings settings) {
        BigDecimal distance = distanceFromShopKm(pin);
        BigDecimal radius = settings.getDeliveryRadiusKm();
        if (distance.compareTo(radius) > 0) {
            throw new IllegalArgumentException("Your pin is " + distance.toPlainString() + " km from the shop. "
                    + "We deliver within " + radius.stripTrailingZeros().toPlainString() + " km.");
        }
        return distance;
    }

    /**
     * @param km a distance
     * @return the distance with one decimal
     */
    public static BigDecimal roundKm(double km) {
        return BigDecimal.valueOf(km).setScale(DISTANCE_SCALE, RoundingMode.HALF_UP);
    }
}
