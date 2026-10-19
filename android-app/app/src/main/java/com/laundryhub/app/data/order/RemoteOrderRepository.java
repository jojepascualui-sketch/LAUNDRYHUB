package com.laundryhub.app.data.order;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiCalls;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.CatalogApi;
import com.laundryhub.app.data.api.OrderApi;
import com.laundryhub.app.data.api.dto.CreateOrderRequestDto;
import com.laundryhub.app.data.api.dto.OrderDto;
import com.laundryhub.app.data.api.dto.PriceBreakdownDto;
import com.laundryhub.app.data.api.dto.PricingDto;
import com.laundryhub.app.data.api.dto.QuoteRequestDto;
import com.laundryhub.app.data.api.dto.ShopDto;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.data.api.dto.GeocodeDto;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PinCheck;
import com.laundryhub.app.model.PlacedOrder;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.model.Soap;
import com.laundryhub.app.ui.auth.AuthValidator;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Calls the backend and turns its JSON into app models. */
// OOP: Inheritance — implements the OrderRepository contract.
public class RemoteOrderRepository implements OrderRepository {

    // OOP: Encapsulation — the Retrofit interfaces are fixed at construction.
    private final CatalogApi catalog;
    private final OrderApi orders;

    /**
     * @param catalog pricing and shop endpoints
     * @param orders  order endpoints
     */
    public RemoteOrderRepository(@NonNull CatalogApi catalog, @NonNull OrderApi orders) {
        this.catalog = catalog;
        this.orders = orders;
    }

    // OOP: Polymorphism — the Retrofit-backed implementation of each OrderRepository method.
    @Override
    public void loadPricing(@NonNull ApiCallback<Pricing> callback) {
        ApiCalls.enqueue(catalog.pricing(), mapped(callback, RemoteOrderRepository::toPricing));
    }

    @Override
    public void loadShop(@NonNull ApiCallback<ShopDto> callback) {
        ApiCalls.enqueue(catalog.shop(), callback);
    }

    @Override
    public void loadSlots(@NonNull ServiceType service, @NonNull ApiCallback<List<SlotDto>> callback) {
        ApiCalls.enqueue(orders.slots(service.name()), callback);
    }

    @Override
    public void checkPin(@NonNull Location pin, @NonNull ApiCallback<PinCheck> callback) {
        ApiCalls.enqueue(orders.reverseGeocode(pin.getLatitude(), pin.getLongitude()),
                mapped(callback, RemoteOrderRepository::toPinCheck));
    }

    @Override
    public void quote(@NonNull ServiceType service, int kg, long soapId, @NonNull ApiCallback<PriceLines> callback) {
        ApiCalls.enqueue(orders.quote(new QuoteRequestDto(service.name(), BigDecimal.valueOf(kg), soapId)),
                mapped(callback, RemoteOrderRepository::toLines));
    }

    @Override
    public void placeOrder(@NonNull OrderDraft draft, @NonNull ApiCallback<PlacedOrder> callback) {
        ApiCalls.enqueue(orders.place(toRequest(draft)), mapped(callback, RemoteOrderRepository::toPlaced));
    }

    /**
     * Builds the "Place order" body. Spaces in the GCash number are removed.
     *
     * @param draft a complete draft
     * @return the request
     * @throws IllegalArgumentException if the draft is incomplete
     */
    @NonNull
    static CreateOrderRequestDto toRequest(@NonNull OrderDraft draft) {
        if (draft.getService() == null || draft.getSoapId() == null || draft.getSlot() == null) {
            throw new IllegalArgumentException("Service, soap and time are required");
        }
        Location pin = draft.getPin();
        boolean gcash = draft.getPayment().requiresMobileNumber();
        String details = draft.getAddressDetails().trim();
        return new CreateOrderRequestDto(draft.getService().name(), BigDecimal.valueOf(draft.getKg()),
                draft.getSoapId(), draft.getSlot().date(), draft.getSlot().label(),
                pin == null ? null : new CreateOrderRequestDto.PinDto(pin.getLatitude(), pin.getLongitude()),
                details.isEmpty() ? null : details, draft.getPayment().apiName(),
                gcash ? AuthValidator.normalizeMobile(draft.getGcashNumber()) : null);
    }

    private static Pricing toPricing(PricingDto dto) {
        List<Soap> soaps = new ArrayList<>();
        for (PricingDto.SoapDto soap : dto.getSoaps()) {
            soaps.add(new Soap(soap.getId(), soap.getName(), soap.getDescription(), soap.getAddOnPerKg(),
                    soap.getTintHex()));
        }
        return new Pricing(dto.getRatePerKg(), dto.getMinKg(), dto.getDeliveryFee(), dto.isCashEnabled(),
                dto.isGcashEnabled(), soaps);
    }

    private static PinCheck toPinCheck(GeocodeDto dto) {
        String address = dto.getAddress() == null || dto.getAddress().isBlank() ? null : dto.getAddress();
        return new PinCheck(address, dto.getDistanceKm(), dto.getDeliveryRadiusKm(), dto.isInsideDeliveryArea());
    }

    private static PriceLines toLines(PriceBreakdownDto dto) {
        return new PriceLines(dto.getBillableKg(), dto.getRatePerKg(), dto.getLaundryCost(), dto.getSoapName(),
                dto.getSoapAddOnPerKg(), dto.getSoapCost(), dto.getDeliveryFee(), dto.getTotal(), true);
    }

    private static PlacedOrder toPlaced(OrderDto dto) {
        return new PlacedOrder(dto.getOrderNo(), dto.getServiceName(), dto.getWeightKg(), dto.getSoapName(),
                dto.getSchedule() == null ? "" : dto.getSchedule().getText(),
                dto.getPayment() == null ? "" : dto.getPayment().getDisplayName(), dto.getPrice().getTotal());
    }

    /** Maps a successful body; a body that cannot be mapped is reported as an error. */
    private static <A, B> ApiCallback<A> mapped(ApiCallback<B> callback, Function<A, B> mapper) {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull A value) {
                B result;
                try {
                    result = mapper.apply(value);
                } catch (RuntimeException e) {
                    callback.onError(new ApiException(ApiException.NO_RESPONSE, null, null));
                    return;
                }
                callback.onSuccess(result);
            }

            @Override
            public void onError(@NonNull ApiException error) {
                callback.onError(error);
            }
        };
    }
}
