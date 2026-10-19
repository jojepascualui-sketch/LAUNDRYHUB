package com.laundryhub.app.data.api.dto;

/** {@code GET /api/shop}: the 04b shop card. Filled by Gson. */
public final class ShopDto {

    // OOP: Encapsulation — read-only fields set by Gson.
    private String name;
    private String address;
    private String hoursLabel;
    private java.math.BigDecimal latitude;
    private java.math.BigDecimal longitude;

    /** For Gson. */
    public ShopDto() {
    }

    /**
     * @param name       shop name
     * @param address    "[SHOP ADDRESS]"
     * @param hoursLabel "[SHOP HOURS]"
     */
    public ShopDto(String name, String address, String hoursLabel) {
        this.name = name;
        this.address = address;
        this.hoursLabel = hoursLabel;
    }

    /** @return shop name */
    public String getName() {
        return name;
    }

    /** @return street address */
    public String getAddress() {
        return address;
    }

    /** @return shop latitude, or {@code null} if not set (centres the 04a map) */
    public java.math.BigDecimal getLatitude() {
        return latitude;
    }

    /** @return shop longitude, or {@code null} if not set */
    public java.math.BigDecimal getLongitude() {
        return longitude;
    }

    /** @return opening hours */
    public String getHoursLabel() {
        return hoursLabel;
    }
}
