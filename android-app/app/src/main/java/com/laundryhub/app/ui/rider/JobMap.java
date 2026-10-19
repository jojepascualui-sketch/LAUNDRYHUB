package com.laundryhub.app.ui.rider;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;
import com.laundryhub.app.R;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.ui.common.MarkerIcons;

/**
 * The map on R2/R3: the rider (truck marker), the next stop (orange pin) and the route between
 * them (accent, 5 dp). Turn-by-turn directions are left to Google Maps ("Navigate in Google Maps").
 */
public final class JobMap {

    /** Street-level zoom when only one point is known. */
    private static final float STOP_ZOOM = 15f;

    /** The truck is centred on the rider; the pin's tip is its bottom centre. */
    private static final float CENTER = 0.5f;
    private static final float BOTTOM = 1f;

    // OOP: Encapsulation — the map and icons are private; callers pass Locations only.
    private final GoogleMap map;
    private final View mapView;
    private final BitmapDescriptor truckIcon;
    private final BitmapDescriptor pinIcon;
    private final int routeColor;
    private final float routeWidth;
    private final int cameraPadding;

    /**
     * @param context for icons and sizes
     * @param map     the ready map
     * @param mapView the view the map fills (its size frames the camera)
     */
    public JobMap(@NonNull Context context, @NonNull GoogleMap map, @NonNull View mapView) {
        this.map = map;
        this.mapView = mapView;
        truckIcon = MarkerIcons.fromDrawable(context, R.drawable.marker_truck);
        pinIcon = MarkerIcons.fromDrawable(context, R.drawable.ic_map_pin);
        routeColor = ContextCompat.getColor(context, R.color.accent);
        routeWidth = context.getResources().getDimension(R.dimen.route_width);
        cameraPadding = context.getResources().getDimensionPixelSize(R.dimen.route_camera_padding);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
    }

    /**
     * Redraws markers and route and frames them.
     *
     * @param rider where the rider is, or {@code null} if unknown
     * @param stop  where to go next, or {@code null} when the job is done
     */
    public void show(@Nullable Location rider, @Nullable Location stop) {
        map.clear();
        LatLng from = rider == null ? null : toLatLng(rider);
        LatLng to = stop == null ? null : toLatLng(stop);
        if (from != null && to != null) {
            map.addPolyline(new PolylineOptions().add(from, to).color(routeColor).width(routeWidth)
                    .startCap(new RoundCap()).endCap(new RoundCap()).jointType(JointType.ROUND));
        }
        if (to != null) {
            map.addMarker(new MarkerOptions().position(to).icon(pinIcon).anchor(CENTER, BOTTOM));
        }
        if (from != null) {
            map.addMarker(new MarkerOptions().position(from).icon(truckIcon).anchor(CENTER, CENTER).flat(false));
        }
        frame(from, to);
    }

    private void frame(@Nullable LatLng from, @Nullable LatLng to) {
        int width = mapView.getWidth();
        int height = mapView.getHeight();
        if (from != null && to != null && width > 0 && height > 0) {
            LatLngBounds bounds = new LatLngBounds.Builder().include(from).include(to).build();
            map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, width, height, cameraPadding));
        } else if (to != null || from != null) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(to != null ? to : from, STOP_ZOOM));
        }
    }

    private static LatLng toLatLng(Location location) {
        return new LatLng(location.getLatitude().doubleValue(), location.getLongitude().doubleValue());
    }
}
