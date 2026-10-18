package com.laundryhub.backend.service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.ShopSettings;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.order.ServiceCode;
import com.laundryhub.backend.domain.order.ServiceType;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.ShopSettingsRepository;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.web.dto.PricingResponse;
import com.laundryhub.backend.web.dto.ScheduleResponse;
import com.laundryhub.backend.web.dto.ServiceOptionResponse;
import com.laundryhub.backend.web.dto.ShopResponse;
import com.laundryhub.backend.web.dto.SoapResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read-only catalog for the customer app: prices, soaps, services, time chips and the shop card. */
@Service
public class CatalogService {

    private final PricingSettingsRepository pricing;
    private final SoapRepository soaps;
    private final ShopSettingsRepository shop;
    private final DeliveryZoneService zone;

    /**
     * @param pricing pricing settings
     * @param soaps   soaps
     * @param shop    shop settings
     * @param zone    shop position
     */
    public CatalogService(PricingSettingsRepository pricing, SoapRepository soaps, ShopSettingsRepository shop,
                          DeliveryZoneService zone) {
        this.pricing = pricing;
        this.soaps = soaps;
        this.shop = shop;
        this.zone = zone;
    }

    /** @return rates, services and available soaps (01, 02, 03, 05) */
    @Transactional(readOnly = true)
    public PricingResponse pricing() {
        PricingSettings settings = pricing.getSettings();
        List<ServiceOptionResponse> services = Arrays.stream(ServiceCode.values())
                .map(code -> ServiceOptionResponse.from(ServiceType.of(code, settings)))
                .toList();
        List<SoapResponse> available = soaps.findByAvailableTrueOrderBySortOrderAsc().stream()
                .map(SoapResponse::from)
                .toList();
        return new PricingResponse(settings.getRatePerKg(), settings.getMinKg(), settings.getDeliveryFee(),
                settings.getDeliveryRadiusKm(), settings.isCashEnabled(), settings.isGcashEnabled(), services,
                available);
    }

    /** @return the shop card (04b) and marker (07) */
    @Transactional(readOnly = true)
    public ShopResponse shop() {
        ShopSettings settings = shop.getSettings();
        Location position = zone.findShopLocation();
        return new ShopResponse(settings.getShopName(), settings.getAddress(), settings.getHoursLabel(),
                position == null ? null : position.getLatitude(), position == null ? null : position.getLongitude(),
                settings.getContactNumber());
    }

    /**
     * Time chips: "Pickup time" on 04a or "Drop-off time" on 04b, exactly as in the design.
     *
     * @param service chosen service
     * @return the chips with concrete dates
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> slots(ServiceCode service) {
        LocalDate today = LocalDate.now();
        // OOP: Polymorphism — each ServiceType offers its own chips.
        return ServiceType.of(service, pricing.getSettings()).getSlotOptions().stream()
                .map(option -> ScheduleResponse.from(option.on(today), today))
                .toList();
    }
}
