package com.laundryhub.app.testing;

import androidx.annotation.NonNull;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.api.dto.ShopDto;
import com.laundryhub.app.data.api.dto.SlotDto;
import com.laundryhub.app.data.order.OrderRepository;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PinCheck;
import com.laundryhub.app.model.PlacedOrder;
import com.laundryhub.app.model.PriceLines;
import com.laundryhub.app.model.Pricing;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.model.Soap;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * In-memory backend with the design's sample data (₱30/kg, 8 kg, ₱60, four soaps, the design's
 * time chips). Answers synchronously. Quotes use the server formula and are marked fromServer.
 */
public class FakeOrderRepository implements OrderRepository {

    /** Soap ids in design order. */
    public static final long STANDARD = 1, HYPOALLERGENIC = 2, PREMIUM = 3, BABY_SAFE = 4;

    public final Pricing pricing = new Pricing(new BigDecimal("30.00"), new BigDecimal("8.00"),
            new BigDecimal("60.00"), true, true, List.of(
                    new Soap(STANDARD, "Standard", "Everyday clean for regular clothes", BigDecimal.ZERO, "#E4EEF8"),
                    new Soap(HYPOALLERGENIC, "Hypoallergenic", "Gentle on sensitive skin", new BigDecimal("5.00"), "#E6F2EA"),
                    new Soap(PREMIUM, "Premium Scented", "Long-lasting fresh fragrance", new BigDecimal("8.00"), "#F3E8F7"),
                    new Soap(BABY_SAFE, "Baby-Safe", "Mild and dye-free formula", new BigDecimal("6.00"), "#FFF1E8")));

    /** Shop used for the radius check (the test backend's shop). */
    public static final Location SHOP = Location.of(14.6, 121.0);

    /** Delivery radius in km. */
    public static final BigDecimal RADIUS_KM = new BigDecimal("5.0");

    public ApiException failPricing;
    public ApiException failPinCheck;
    public boolean holdPinChecks;
    public final List<ApiCallback<PinCheck>> heldPinChecks = new ArrayList<>();
    public ApiException failPlace;
    public boolean holdQuotes;
    public final List<ApiCallback<PriceLines>> heldQuotes = new ArrayList<>();
    public int quoteCalls;
    public OrderDraft placedDraft;

    @Override
    public void loadPricing(@NonNull ApiCallback<Pricing> callback) {
        if (failPricing != null) {
            callback.onError(failPricing);
        } else {
            callback.onSuccess(pricing);
        }
    }

    @Override
    public void loadShop(@NonNull ApiCallback<ShopDto> callback) {
        callback.onSuccess(new ShopDto("Laundry HUB", "[SHOP ADDRESS]", "[SHOP HOURS]"));
    }

    @Override
    public void loadSlots(@NonNull ServiceType service, @NonNull ApiCallback<List<SlotDto>> callback) {
        String today = LocalDate.now().toString();
        String tomorrow = LocalDate.now().plusDays(1).toString();
        callback.onSuccess(service == ServiceType.PICKUP_DELIVERY
                ? List.of(new SlotDto(today, "2–4 PM", "Today, 2–4 PM"), new SlotDto(today, "4–6 PM", "Today, 4–6 PM"),
                        new SlotDto(tomorrow, "9–11 AM", "Tomorrow, 9–11 AM"),
                        new SlotDto(tomorrow, "1–3 PM", "Tomorrow, 1–3 PM"))
                : List.of(new SlotDto(today, "morning", "Today, morning"),
                        new SlotDto(today, "afternoon", "Today, afternoon"),
                        new SlotDto(tomorrow, "morning", "Tomorrow, morning"),
                        new SlotDto(tomorrow, "afternoon", "Tomorrow, afternoon")));
    }

    /** Address "Pinned near lat,lng"; inside when within 5 km (rough flat-earth distance, fine for tests). */
    @Override
    public void checkPin(@NonNull Location pin, @NonNull ApiCallback<PinCheck> callback) {
        if (holdPinChecks) {
            heldPinChecks.add(callback);
            return;
        }
        if (failPinCheck != null) {
            callback.onError(failPinCheck);
            return;
        }
        callback.onSuccess(pinCheck(pin));
    }

    /** The server's answer for a pin. */
    public PinCheck pinCheck(Location pin) {
        double dLat = (pin.getLatitude().doubleValue() - SHOP.getLatitude().doubleValue()) * 111.19;
        double dLng = (pin.getLongitude().doubleValue() - SHOP.getLongitude().doubleValue()) * 107.6;
        BigDecimal km = BigDecimal.valueOf(Math.sqrt(dLat * dLat + dLng * dLng)).setScale(1, java.math.RoundingMode.HALF_UP);
        return new PinCheck("Pinned near " + pin, km, RADIUS_KM, km.compareTo(RADIUS_KM) <= 0);
    }

    @Override
    public void quote(@NonNull ServiceType service, int kg, long soapId, @NonNull ApiCallback<PriceLines> callback) {
        quoteCalls++;
        if (holdQuotes) {
            heldQuotes.add(callback);
            return;
        }
        callback.onSuccess(serverLines(service, kg, soapId));
    }

    /** The server's answer for these inputs. */
    public PriceLines serverLines(ServiceType service, int kg, long soapId) {
        PriceLines local = pricing.estimate(service, kg, pricing.soap(soapId));
        return new PriceLines(local.billableKg(), local.ratePerKg(), local.laundryCost(), local.soapName(),
                local.soapAddOnPerKg(), local.soapCost(), local.deliveryFee(), local.total(), true);
    }

    @Override
    public void placeOrder(@NonNull OrderDraft draft, @NonNull ApiCallback<PlacedOrder> callback) {
        placedDraft = draft;
        if (failPlace != null) {
            callback.onError(failPlace);
            return;
        }
        PriceLines lines = serverLines(draft.getService(), draft.getKg(), draft.getSoapId());
        callback.onSuccess(new PlacedOrder("LH-0001",
                draft.getService() == ServiceType.PICKUP_DELIVERY ? "Pickup & Delivery" : "Walk-in",
                BigDecimal.valueOf(draft.getKg()), lines.soapName(), draft.getSlot().text(),
                draft.getPayment().name().equals("GCASH") ? "GCash" : "Cash", lines.total()));
    }

    /** Orders returned by the Orders tab, newest first. */
    public final java.util.List<com.laundryhub.app.model.tracking.OrderSummary> myOrders = new ArrayList<>();

    /** Tracking returned for every order (07); {@code null} answers with an error. */
    public com.laundryhub.app.model.tracking.Tracking tracking;

    @Override
    public void myOrders(@NonNull ApiCallback<java.util.List<com.laundryhub.app.model.tracking.OrderSummary>> callback) {
        callback.onSuccess(java.util.List.copyOf(myOrders));
    }

    @Override
    public void tracking(long orderId, @NonNull ApiCallback<com.laundryhub.app.model.tracking.Tracking> callback) {
        if (tracking == null) {
            callback.onError(new ApiException(404, "Order not found", null));
        } else {
            callback.onSuccess(tracking);
        }
    }
}
