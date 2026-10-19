package com.laundryhub.app.data.api;

import com.laundryhub.app.data.api.dto.OrderSummaryDto;
import com.laundryhub.app.data.api.dto.TrackingDto;
import com.laundryhub.app.data.api.dto.CreateOrderRequestDto;
import com.laundryhub.app.data.api.dto.GeocodeDto;
import com.laundryhub.app.data.api.dto.OrderDto;
import com.laundryhub.app.data.api.dto.PriceBreakdownDto;
import com.laundryhub.app.data.api.dto.QuoteRequestDto;
import com.laundryhub.app.data.api.dto.SlotDto;

import java.math.BigDecimal;
import java.util.List;

import retrofit2.Call;
import retrofit2.http.Path;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/** Customer order endpoints (role CUSTOMER). */
// OOP: Abstraction — Retrofit generates the HTTP code behind this interface.
public interface OrderApi {

    /**
     * @param service WALK_IN or PICKUP_DELIVERY
     * @return the four time chips
     */
    @GET("orders/slots")
    Call<List<SlotDto>> slots(@Query("service") String service);

    /**
     * 04a: address for a pin and whether it is inside the delivery radius.
     *
     * @param lat latitude
     * @param lng longitude
     * @return address and radius check
     */
    @GET("geocode/reverse")
    Call<GeocodeDto> reverseGeocode(@Query("lat") BigDecimal lat, @Query("lng") BigDecimal lng);

    /**
     * @param body service, kilos, soap
     * @return the server price
     */
    @POST("orders/quote")
    Call<PriceBreakdownDto> quote(@Body QuoteRequestDto body);

    /**
     * @param body the order
     * @return the placed order
     */
    /** @return the customer's orders, newest first (Orders tab) */
    @GET("orders/mine")
    Call<List<OrderSummaryDto>> mine();

    /**
     * @param id order id
     * @return 07 Track order
     */
    @GET("orders/{id}/tracking")
    Call<TrackingDto> tracking(@Path("id") long id);

    @POST("orders")
    Call<OrderDto> place(@Body CreateOrderRequestDto body);
}
