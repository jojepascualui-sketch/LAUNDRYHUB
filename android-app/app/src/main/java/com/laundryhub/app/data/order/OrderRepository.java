package com.laundryhub.app.data.order;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.api.dto.ShopDto;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PinCheck;
import com.laundryhub.app.model.PlacedOrder;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;

import java.util.List;

/** Everything the customer order flow (01–06) needs from the server. */
// OOP: Abstraction — ViewModels depend on this interface; tests use a fake.
public interface OrderRepository {

    /**
     * @param callback prices, payment methods and soaps
     */
    void loadPricing(@NonNull ApiCallback<Pricing> callback);

    /**
     * @param callback the shop card (04b)
     */
    void loadShop(@NonNull ApiCallback<ShopDto> callback);

    /**
     * @param service  chosen service
     * @param callback the time chips
     */
    void loadSlots(@NonNull ServiceType service, @NonNull ApiCallback<List<SlotDto>> callback);

    /**
     * 04a: reverse geocoding and the delivery-radius check (done by the backend).
     *
     * @param pin      the pinned location
     * @param callback address and whether the pin is inside the radius
     */
    void checkPin(@NonNull Location pin, @NonNull ApiCallback<PinCheck> callback);

    /**
     * @param service  service (walk-in when none is chosen yet)
     * @param kg       kilos
     * @param soapId   soap
     * @param callback the server price lines
     */
    void quote(@NonNull ServiceType service, int kg, long soapId, @NonNull ApiCallback<PriceLines> callback);

    /**
     * @param draft    a complete draft
     * @param callback the placed order (06)
     */
    void placeOrder(@NonNull OrderDraft draft, @NonNull ApiCallback<PlacedOrder> callback);

    /**
     * @param callback the customer's orders, newest first (Orders tab)
     */
    void myOrders(@NonNull ApiCallback<java.util.List<com.laundryhub.app.model.tracking.OrderSummary>> callback);

    /**
     * @param orderId  the order
     * @param callback 07 Track order data
     */
    void tracking(long orderId, @NonNull ApiCallback<com.laundryhub.app.model.tracking.Tracking> callback);
}
