package com.laundryhub.app.data.api;

import com.laundryhub.app.data.api.dto.PricingDto;
import com.laundryhub.app.data.api.dto.ShopDto;

import retrofit2.Call;
import retrofit2.http.GET;

/** Public catalog (docs/SCREEN_API_MAP.md: 01–05). */
// OOP: Abstraction — Retrofit generates the HTTP code behind this interface.
public interface CatalogApi {

    /** @return rates, minimum load, delivery fee, payment methods and soaps */
    @GET("pricing")
    Call<PricingDto> pricing();

    /** @return the shop card (04b) */
    @GET("shop")
    Call<ShopDto> shop();
}
