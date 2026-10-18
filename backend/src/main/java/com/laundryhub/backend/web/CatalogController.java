package com.laundryhub.backend.web;

import com.laundryhub.backend.service.CatalogService;
import com.laundryhub.backend.web.dto.PricingResponse;
import com.laundryhub.backend.web.dto.ShopResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Public prices and shop details for the customer app. */
@RestController
@RequestMapping("/api")
@Tag(name = "Catalog", description = "01 Home · 02 Service · 03 Weight & soap · 04b Shop card · 05 Payment methods")
public class CatalogController {

    private final CatalogService catalog;

    /**
     * @param catalog catalog logic
     */
    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    /** @return rates, minimum load, delivery fee, services, available soaps, payment methods */
    @GetMapping("/pricing")
    @SecurityRequirements
    @Operation(summary = "Prices, services and soaps (01–05)")
    public PricingResponse pricing() {
        return catalog.pricing();
    }

    /** @return shop name, address, hours and position */
    @GetMapping("/shop")
    @SecurityRequirements
    @Operation(summary = "Shop card (04b) and shop marker (07)")
    public ShopResponse shop() {
        return catalog.shop();
    }
}
