package com.laundryhub.app.data.api.dto;

import java.math.BigDecimal;

/** Body of {@code POST /api/orders} ("Place order"). No prices: the server calculates them. */
public final class CreateOrderRequestDto {

    // OOP: Encapsulation — an immutable request; Gson skips null fields.
    private final String serviceType;
    private final BigDecimal weightKg;
    private final long soapId;
    private final String scheduleDate;
    private final String slotLabel;
    private final PinDto pin;
    private final String addressDetails;
    private final String paymentMethod;
    private final String gcashNumber;

    /**
     * @param serviceType    WALK_IN or PICKUP_DELIVERY
     * @param weightKg       kilos
     * @param soapId         soap
     * @param scheduleDate   ISO day of the chip
     * @param slotLabel      chip label
     * @param pin            pin (delivery) or {@code null}
     * @param addressDetails address details or {@code null}
     * @param paymentMethod  CASH or GCASH
     * @param gcashNumber    GCash number or {@code null}
     */
    public CreateOrderRequestDto(String serviceType, BigDecimal weightKg, long soapId, String scheduleDate,
                                 String slotLabel, PinDto pin, String addressDetails, String paymentMethod,
                                 String gcashNumber) {
        this.serviceType = serviceType;
        this.weightKg = weightKg;
        this.soapId = soapId;
        this.scheduleDate = scheduleDate;
        this.slotLabel = slotLabel;
        this.pin = pin;
        this.addressDetails = addressDetails;
        this.paymentMethod = paymentMethod;
        this.gcashNumber = gcashNumber;
    }

    /** @return the payment method sent */
    public String getPaymentMethod() {
        return paymentMethod;
    }

    /** @return the GCash number sent, or {@code null} */
    public String getGcashNumber() {
        return gcashNumber;
    }

    /** A pinned point. */
    public static final class PinDto {
        private final BigDecimal latitude;
        private final BigDecimal longitude;

        /**
         * @param latitude  degrees
         * @param longitude degrees
         */
        public PinDto(BigDecimal latitude, BigDecimal longitude) {
            this.latitude = latitude;
            this.longitude = longitude;
        }
    }
}
