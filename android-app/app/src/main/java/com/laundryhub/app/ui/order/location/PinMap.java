package com.laundryhub.app.ui.order.location;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.laundryhub.app.R;
import com.laundryhub.app.model.Location;

import java.util.function.Consumer;

/**
 * The Google map on 04a: tapping moves the orange design pin (#E0662A) there, and the pin can be
 * dragged. Every move is reported so the ViewModel can reverse-geocode and check the radius.
 */
public final class PinMap {

    /** Street-level zoom for the pin. */
    public static final float PIN_ZOOM = 16f;

    /** Zoom when nothing is pinned yet (around the shop). */
    public static final float AREA_ZOOM = 14f;

    /** Where the map opens if the shop position is not set yet: Metro Manila. */
    public static final LatLng FALLBACK_CENTER = new LatLng(14.5995, 120.9842);

    /** The pin's tip is the bottom centre of the icon. */
    private static final float ANCHOR_U = 0.5f;
    private static final float ANCHOR_V = 1f;

    // OOP: Encapsulation — the map and marker are private; callers set and receive Locations only.
    private final GoogleMap map;
    private final BitmapDescriptor pinIcon;
    private final Consumer<Location> onPinMoved;
    @Nullable
    private Marker marker;

    /**
     * @param context    for the pin drawable
     * @param map        the ready map
     * @param onPinMoved receives the new location after a tap or a drag
     */
    public PinMap(@NonNull Context context, @NonNull GoogleMap map, @NonNull Consumer<Location> onPinMoved) {
        this.map = map;
        this.onPinMoved = onPinMoved;
        this.pinIcon = icon(context);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
        map.setOnMapClickListener(latLng -> moved(latLng));
        map.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDragStart(@NonNull Marker m) {
            }

            @Override
            public void onMarkerDrag(@NonNull Marker m) {
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker m) {
                moved(m.getPosition());
            }
        });
    }

    private void moved(LatLng latLng) {
        Location location = Location.of(latLng.latitude, latLng.longitude);
        show(location);
        onPinMoved.accept(location);
    }

    /**
     * Shows the pin at a location (from the ViewModel), without reporting it back.
     *
     * @param location where the pin is, or {@code null} to hide it
     */
    public void show(@Nullable Location location) {
        if (location == null) {
            if (marker != null) {
                marker.remove();
                marker = null;
            }
            return;
        }
        LatLng position = toLatLng(location);
        if (marker == null) {
            marker = map.addMarker(new MarkerOptions().position(position).icon(pinIcon).anchor(ANCHOR_U, ANCHOR_V)
                    .draggable(true));
        } else if (!marker.getPosition().equals(position)) {
            marker.setPosition(position);
        }
    }

    /**
     * Moves the camera to the pin, or to the shop when nothing is pinned.
     *
     * @param pin     current pin, or {@code null}
     * @param shop    shop position, or {@code null}
     * @param animate animate (after "Use my current location") or jump (first show)
     */
    public void focus(@Nullable Location pin, @Nullable Location shop, boolean animate) {
        LatLng target = pin != null ? toLatLng(pin) : shop != null ? toLatLng(shop) : FALLBACK_CENTER;
        float zoom = pin != null ? PIN_ZOOM : AREA_ZOOM;
        if (animate) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
        } else {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(target, zoom));
        }
    }

    private static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
    }

    /** The design's pin (vector) as a marker bitmap. */
    private static BitmapDescriptor icon(Context context) {
        Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_map_pin);
        if (drawable == null) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        drawable.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        drawable.draw(new Canvas(bitmap));
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
