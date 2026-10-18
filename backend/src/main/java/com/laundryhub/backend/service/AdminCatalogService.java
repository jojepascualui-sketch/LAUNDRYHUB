package com.laundryhub.backend.service;

import java.util.List;

import com.laundryhub.backend.domain.catalog.PricingSettings;
import com.laundryhub.backend.domain.catalog.Soap;
import com.laundryhub.backend.domain.user.ConsoleUser;
import com.laundryhub.backend.exception.ConflictException;
import com.laundryhub.backend.exception.NotFoundException;
import com.laundryhub.backend.repository.OrderRepository;
import com.laundryhub.backend.repository.PricingSettingsRepository;
import com.laundryhub.backend.repository.SoapRepository;
import com.laundryhub.backend.web.dto.PricingSettingsResponse;
import com.laundryhub.backend.web.dto.PricingUpdateRequest;
import com.laundryhub.backend.web.dto.SoapRequest;
import com.laundryhub.backend.web.dto.SoapResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * A3 Pricing &amp; soaps. New prices apply to orders placed after saving; placed orders keep
 * the prices they were placed with.
 */
@Service
public class AdminCatalogService {

    private final PricingSettingsRepository pricing;
    private final SoapRepository soaps;
    private final OrderRepository orders;
    private final AccountLookup accounts;

    /**
     * @param pricing  pricing settings
     * @param soaps    soaps
     * @param orders   orders (a soap used by orders cannot be deleted)
     * @param accounts signed-in account lookup
     */
    public AdminCatalogService(PricingSettingsRepository pricing, SoapRepository soaps, OrderRepository orders,
                               AccountLookup accounts) {
        this.pricing = pricing;
        this.soaps = soaps;
        this.orders = orders;
        this.accounts = accounts;
    }

    /** @return the A3 form, with every soap (available or not) */
    @Transactional(readOnly = true)
    public PricingSettingsResponse getPricing() {
        return PricingSettingsResponse.from(pricing.getSettings(), allSoaps());
    }

    /**
     * A3 "Save changes" for rates and payment methods.
     *
     * @param editorId the signed-in admin
     * @param request  the form
     * @return the saved form
     */
    @Transactional
    public PricingSettingsResponse updatePricing(Long editorId, PricingUpdateRequest request) {
        ConsoleUser editor = accounts.consoleUser(editorId);
        PricingSettings settings = pricing.lockSettings();
        settings.update(request.ratePerKg(), request.minKg(), request.deliveryFee(), request.deliveryRadiusKm(),
                request.cashEnabled(), request.gcashEnabled(), blankToNull(request.shopGcashNumber()), editor);
        pricing.flush();
        return PricingSettingsResponse.from(settings, allSoaps());
    }

    /** @return every soap in display order */
    @Transactional(readOnly = true)
    public List<SoapResponse> allSoaps() {
        return soaps.findAllByOrderBySortOrderAsc().stream().map(SoapResponse::from).toList();
    }

    /**
     * @param soapId soap id
     * @return the soap
     */
    @Transactional(readOnly = true)
    public SoapResponse getSoap(Long soapId) {
        return SoapResponse.from(findSoap(soapId));
    }

    /**
     * A3 "+ Add soap".
     *
     * @param editorId the signed-in admin
     * @param request  the soap
     * @return the new soap
     */
    @Transactional
    public SoapResponse createSoap(Long editorId, SoapRequest request) {
        accounts.consoleUser(editorId);
        if (soaps.existsByNameIgnoreCase(request.name().trim())) {
            throw new ConflictException("A soap named \"" + request.name().trim() + "\" already exists.");
        }
        int sortOrder = request.sortOrder() != null ? request.sortOrder()
                : soaps.findFirstByOrderBySortOrderDesc().map(s -> s.getSortOrder() + 1).orElse(1);
        Soap soap = new Soap(request.name(), request.description(), request.addOnPerKg(),
                request.tintHex() == null ? Soap.DEFAULT_TINT : request.tintHex(), sortOrder);
        soap.setAvailable(request.available());
        return SoapResponse.from(soaps.save(soap));
    }

    /**
     * Edits a soap row (name, add-on, Available checkbox, ...).
     *
     * @param editorId the signed-in admin
     * @param soapId   soap id
     * @param request  new values
     * @return the saved soap
     */
    @Transactional
    public SoapResponse updateSoap(Long editorId, Long soapId, SoapRequest request) {
        accounts.consoleUser(editorId);
        Soap soap = findSoap(soapId);
        if (soaps.existsByNameIgnoreCaseAndIdNot(request.name().trim(), soapId)) {
            throw new ConflictException("A soap named \"" + request.name().trim() + "\" already exists.");
        }
        soap.rename(request.name());
        soap.changeDescription(request.description());
        soap.changeAddOnPerKg(request.addOnPerKg());
        soap.setAvailable(request.available());
        if (request.tintHex() != null) {
            soap.changeTint(request.tintHex());
        }
        if (request.sortOrder() != null) {
            soap.moveTo(request.sortOrder());
        }
        return SoapResponse.from(soap);
    }

    /**
     * Deletes a soap that no order has used. Used soaps can only be made unavailable.
     *
     * @param editorId the signed-in admin
     * @param soapId   soap id
     */
    @Transactional
    public void deleteSoap(Long editorId, Long soapId) {
        accounts.consoleUser(editorId);
        Soap soap = findSoap(soapId);
        if (orders.existsBySoapId(soapId)) {
            throw new ConflictException(soap.getName() + " is used by orders. Untick \"Available\" instead.");
        }
        soaps.delete(soap);
    }

    private Soap findSoap(Long soapId) {
        return soaps.findById(soapId).orElseThrow(() -> new NotFoundException("Soap not found"));
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
