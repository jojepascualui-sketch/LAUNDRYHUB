package com.laundryhub.backend.web.admin;

import java.net.URI;
import java.util.List;

import com.laundryhub.backend.security.CurrentUserId;
import com.laundryhub.backend.service.AdminCatalogService;
import com.laundryhub.backend.web.dto.PricingSettingsResponse;
import com.laundryhub.backend.web.dto.PricingUpdateRequest;
import com.laundryhub.backend.web.dto.SoapRequest;
import com.laundryhub.backend.web.dto.SoapResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A3 Pricing &amp; soaps (role ADMIN only; staff may not change prices). */
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin pricing", description = "A3 Pricing & soaps (ADMIN)")
public class AdminPricingController {

    private final AdminCatalogService catalog;

    /**
     * @param catalog pricing and soap management
     */
    public AdminPricingController(AdminCatalogService catalog) {
        this.catalog = catalog;
    }

    /** @return rates, payment methods and every soap */
    @GetMapping("/pricing")
    @Operation(summary = "A3 form")
    public PricingSettingsResponse pricing() {
        return catalog.getPricing();
    }

    /**
     * @param editorId the signed-in admin
     * @param request  rates and payment methods
     * @return the saved form
     */
    @PutMapping("/pricing")
    @Operation(summary = "A3 Save changes (rates, radius, payment methods)")
    public PricingSettingsResponse updatePricing(@CurrentUserId Long editorId,
                                                 @Valid @RequestBody PricingUpdateRequest request) {
        return catalog.updatePricing(editorId, request);
    }

    /** @return every soap, available or not */
    @GetMapping("/soaps")
    @Operation(summary = "A3 Fabric soaps table")
    public List<SoapResponse> soaps() {
        return catalog.allSoaps();
    }

    /**
     * @param id soap id
     * @return the soap
     */
    @GetMapping("/soaps/{id}")
    @Operation(summary = "One soap")
    public SoapResponse soap(@PathVariable Long id) {
        return catalog.getSoap(id);
    }

    /**
     * @param editorId the signed-in admin
     * @param request  the new soap
     * @return 201 with the soap
     */
    @PostMapping("/soaps")
    @Operation(summary = "A3 + Add soap")
    public ResponseEntity<SoapResponse> createSoap(@CurrentUserId Long editorId,
                                                   @Valid @RequestBody SoapRequest request) {
        SoapResponse soap = catalog.createSoap(editorId, request);
        return ResponseEntity.created(URI.create("/api/admin/soaps/" + soap.id())).body(soap);
    }

    /**
     * @param editorId the signed-in admin
     * @param id       soap id
     * @param request  new values (name, add-on, Available, ...)
     * @return the soap
     */
    @PutMapping("/soaps/{id}")
    @Operation(summary = "A3 Edit soap row / Available checkbox")
    public SoapResponse updateSoap(@CurrentUserId Long editorId, @PathVariable Long id,
                                   @Valid @RequestBody SoapRequest request) {
        return catalog.updateSoap(editorId, id, request);
    }

    /**
     * @param editorId the signed-in admin
     * @param id       soap id; 409 if orders used it
     */
    @DeleteMapping("/soaps/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete an unused soap (used soaps: untick Available)")
    public void deleteSoap(@CurrentUserId Long editorId, @PathVariable Long id) {
        catalog.deleteSoap(editorId, id);
    }
}
