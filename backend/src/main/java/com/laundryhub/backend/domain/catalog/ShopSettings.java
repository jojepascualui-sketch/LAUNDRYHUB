package com.laundryhub.backend.domain.catalog;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Location;
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
 * The shop itself: name, "[SHOP ADDRESS]", "Open [SHOP HOURS]" (screen 04b) and the
 * map position used for the shop marker (07) and the delivery radius. There is exactly one row.
 */
@Entity
@Table(name = "shop_settings")
public class ShopSettings {

    /** Id of the only row. */
    public static final long SINGLETON_ID = 1L;

    private static final int MAX_NAME_LENGTH = 80;
    private static final int MAX_HOURS_LENGTH = 80;

    @Id
    private Long id;

    // OOP: Encapsulation — fields change only through update() and pinLocation().
    @Column(name = "shop_name", nullable = false, length = MAX_NAME_LENGTH)
    private String shopName;

    @Column(name = "address", nullable = false, length = Location.MAX_ADDRESS_LENGTH)
    private String address;

    @Column(name = "hours_label", nullable = false, length = MAX_HOURS_LENGTH)
    private String hoursLabel;

    @Column(name = "latitude", precision = 9, scale = Location.COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 9, scale = Location.COORDINATE_SCALE)
    private BigDecimal longitude;

    @Column(name = "contact_number", length = 11)
    private String contactNumber;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    /** For JPA only. */
    protected ShopSettings() {
    }

    /**
     * Changes the shop details.
     *
     * @param shopName      e.g. "Laundry HUB"
     * @param address       street address shown on 04b
     * @param hoursLabel    opening hours, shown after "Open "
     * @param contactNumber shop number 09XXXXXXXXX, or {@code null}
     * @param editor        console user making the change
     */
    public void update(String shopName, String address, String hoursLabel, String contactNumber, ConsoleUser editor) {
        this.shopName = Check.text(shopName, "Shop name", MAX_NAME_LENGTH);
        this.address = Check.text(address, "Shop address", Location.MAX_ADDRESS_LENGTH);
        this.hoursLabel = Check.text(hoursLabel, "Shop hours", MAX_HOURS_LENGTH);
        this.contactNumber = contactNumber == null ? null : Check.mobileNumber(contactNumber, "Shop contact number");
        this.updatedBy = Check.notNull(editor, "Editor");
    }

    /**
     * Sets where the shop is on the map.
     *
     * @param location shop position
     * @param editor   console user making the change
     */
    public void pinLocation(Location location, ConsoleUser editor) {
        Check.notNull(location, "Shop location");
        this.latitude = location.getLatitude();
        this.longitude = location.getLongitude();
        this.updatedBy = Check.notNull(editor, "Editor");
    }

    @PrePersist
    @PreUpdate
    void touch() {
        updatedAt = LocalDateTime.now();
    }

    /** @return the shop position, or {@code null} until the owner pins it */
    public Location getLocation() {
        return latitude == null ? null : new Location(latitude, longitude, address);
    }

    /** @return shop name */
    public String getShopName() {
        return shopName;
    }

    /** @return street address */
    public String getAddress() {
        return address;
    }

    /** @return opening hours label */
    public String getHoursLabel() {
        return hoursLabel;
    }

    /** @return shop contact number, or {@code null} */
    public String getContactNumber() {
        return contactNumber;
    }
}
