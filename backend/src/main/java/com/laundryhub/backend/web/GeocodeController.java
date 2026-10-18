package com.laundryhub.backend.web;

import java.math.BigDecimal;

import com.laundryhub.backend.service.OrderService;
import com.laundryhub.backend.web.dto.GeocodeResponse;
import com.laundryhub.backend.web.dto.PinRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 04a: address and delivery-radius check for a dropped pin (role CUSTOMER). */
@RestController
@RequestMapping("/api/geocode")
@Validated
@Tag(name = "Customer orders")
public class GeocodeController {

    private final OrderService orders;

    /**
     * @param orders pin check logic
     */
    public GeocodeController(OrderService orders) {
        this.orders = orders;
    }

    /**
     * @param lat pin latitude
     * @param lng pin longitude
     * @return Google's address, distance from the shop and whether delivery is possible
     */
    @GetMapping("/reverse")
    @Operation(summary = "04a Pin location: address + inside delivery radius?")
    public GeocodeResponse reverse(@RequestParam @DecimalMin("-90") @DecimalMax("90") BigDecimal lat,
                                   @RequestParam @DecimalMin("-180") @DecimalMax("180") BigDecimal lng) {
        return orders.checkPin(new PinRequest(lat, lng));
    }
}
