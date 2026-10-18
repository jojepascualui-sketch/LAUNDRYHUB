package com.laundryhub.backend.domain.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Money;
import com.laundryhub.backend.domain.order.LaundryLoad;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.domain.user.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

/**
 * Shop-wide prices and rules from A3 "Rates" and "Payment methods". There is exactly one row.
 * The server uses these values for every price calculation.
 */
@Entity
@Table(name = "pricing_settings")
public class PricingSettings {

    /** Id of the only row. */
    public static final long SINGLETON_ID = 1L;

    /** Default price per kilo (₱30). */
    public static final BigDecimal DEFAULT_RATE_PER_KG = new BigDecimal("30.00");

    /** Default pickup and delivery fee (₱60). */
    public static final BigDecimal DEFAULT_DELIVERY_FEE = new BigDecimal("60.00");

    /** Default delivery radius in kilometres. */
    public static final BigDecimal DEFAULT_DELIVERY_RADIUS_KM = new BigDecimal("5.00");

    @Id
    private Long id;

    // OOP: Encapsulation — all values change together through update(), which validates them.
    @Column(name = "rate_per_kg", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal ratePerKg;

    @Column(name = "min_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal minKg;

    @Column(name = "delivery_fee", nullable = false, precision = 10, scale = Money.SCALE)
    private BigDecimal deliveryFee;

    @Column(name = "delivery_radius_km", nullable = false, precision = 5, scale = 2)
    private BigDecimal deliveryRadiusKm;

    @Column(name = "cash_enabled", nullable = false)
    private boolean cashEnabled;

    @Column(name = "gcash_enabled", nullable = false)
    private boolean gcashEnabled;

    @Column(name = "shop_gcash_number", length = 11)
    private String shopGcashNumber;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    /** For JPA only. */
    protected PricingSettings() {
    }

    /** @return settings with the design defaults: ₱30/kg, 8 kg minimum, ₱60 delivery, 5 km, Cash and GCash on */
    public static PricingSettings defaults() {
        PricingSettings settings = new PricingSettings();
        settings.id = SINGLETON_ID;
        settings.apply(DEFAULT_RATE_PER_KG, LaundryLoad.MIN_KG, DEFAULT_DELIVERY_FEE, DEFAULT_DELIVERY_RADIUS_KM,
                true, true, null);
        return settings;
    }

    /**
     * Saves the A3 form ("Save changes").
     *
     * @param ratePerKg        price per kilo, more than 0
     * @param minKg            minimum load in kilos, more than 0
     * @param deliveryFee      pickup and delivery fee, 0 or more
     * @param deliveryRadiusKm delivery radius in km, more than 0
     * @param cashEnabled      whether Cash is offered
     * @param gcashEnabled     whether GCash is offered
     * @param shopGcashNumber  shop GCash number 09XXXXXXXXX, required when GCash is on
     * @param editor           the console user saving the form; must be allowed to manage pricing
     * @throws IllegalArgumentException if a value is invalid
     * @throws IllegalStateException    if the editor may not change prices
     */
    public void update(BigDecimal ratePerKg, BigDecimal minKg, BigDecimal deliveryFee, BigDecimal deliveryRadiusKm,
                       boolean cashEnabled, boolean gcashEnabled, String shopGcashNumber, ConsoleUser editor) {
        Check.notNull(editor, "Editor");
        if (!editor.canManagePricing()) {
            throw new IllegalStateException(editor.getFullName() + " may not change prices");
        }
        if (gcashEnabled && shopGcashNumber == null) {
            throw new IllegalArgumentException("Shop GCash number is required when GCash is enabled");
        }
        apply(ratePerKg, minKg, deliveryFee, deliveryRadiusKm, cashEnabled, gcashEnabled, shopGcashNumber);
        this.updatedBy = editor;
    }

    private void apply(BigDecimal ratePerKg, BigDecimal minKg, BigDecimal deliveryFee, BigDecimal deliveryRadiusKm,
                       boolean cashEnabled, boolean gcashEnabled, String shopGcashNumber) {
        if (!cashEnabled && !gcashEnabled) {
            throw new IllegalArgumentException("At least one payment method must be enabled");
        }
        this.ratePerKg = Money.of(Check.positive(ratePerKg, "Price per kilo"));
        this.minKg = Check.positive(minKg, "Minimum load");
        this.deliveryFee = Money.of(Check.nonNegative(deliveryFee, "Pickup & delivery fee"));
        this.deliveryRadiusKm = Check.positive(deliveryRadiusKm, "Delivery radius");
        this.cashEnabled = cashEnabled;
        this.gcashEnabled = gcashEnabled;
        this.shopGcashNumber = shopGcashNumber == null ? null : Check.mobileNumber(shopGcashNumber, "Shop GCash number");
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    /** @return id of the single row */
    public Long getId() {
        return id;
    }

    /** @return price per kilo */
    public BigDecimal getRatePerKg() {
        return ratePerKg;
    }

    /** @return minimum load in kilos */
    public BigDecimal getMinKg() {
        return minKg;
    }

    /** @return pickup and delivery fee */
    public BigDecimal getDeliveryFee() {
        return deliveryFee;
    }

    /** @return delivery radius in kilometres */
    public BigDecimal getDeliveryRadiusKm() {
        return deliveryRadiusKm;
    }

    /** @return whether Cash is offered */
    public boolean isCashEnabled() {
        return cashEnabled;
    }

    /** @return whether GCash is offered */
    public boolean isGcashEnabled() {
        return gcashEnabled;
    }

    /** @return shop GCash number, or {@code null} */
    public String getShopGcashNumber() {
        return shopGcashNumber;
    }

    /** @return when the settings were last saved */
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
