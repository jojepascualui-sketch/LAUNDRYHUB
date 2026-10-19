package com.laundryhub.app.ui.track;

import android.content.Context;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.Dash;
import com.google.android.gms.maps.model.Gap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.laundryhub.app.R;
import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.tracking.Tracking;
import com.laundryhub.app.ui.common.MarkerIcons;

import java.util.ArrayList;
import java.util.List;

/** The 07 map: shop (dark house), rider (truck, live GPS) and pin, joined by the dashed accent route. */
public final class TrackMap {

    private static final float STOP_ZOOM = 15f;
    private static final float CENTER = 0.5f;
    private static final float BOTTOM = 1f;

    // OOP: Encapsulation — map and icons are private; callers pass the Tracking model.
    private final GoogleMap map;
    private final View mapView;
    private final BitmapDescriptor shopIcon;
    private final BitmapDescriptor riderIcon;
    private final BitmapDescriptor pinIcon;
    private final int routeColor;
    private final float routeWidth;
    private final float dash;
    private final float gap;
    private final int padding;

    /**
     * @param context for icons and sizes
     * @param map     the ready map
     * @param mapView the view the map fills
     */
    public TrackMap(@NonNull Context context, @NonNull GoogleMap map, @NonNull View mapView) {
        this.map = map;
        this.mapView = mapView;
        shopIcon = MarkerIcons.fromDrawable(context, R.drawable.marker_shop);
        riderIcon = MarkerIcons.fromDrawable(context, R.drawable.marker_truck);
        pinIcon = MarkerIcons.fromDrawable(context, R.drawable.marker_pin_small);
        routeColor = ContextCompat.getColor(context, R.color.accent);
        routeWidth = context.getResources().getDimension(R.dimen.track_route_width);
        dash = context.getResources().getDimension(R.dimen.track_route_dash);
        gap = context.getResources().getDimension(R.dimen.track_route_gap);
        padding = context.getResources().getDimensionPixelSize(R.dimen.route_camera_padding);
        map.getUiSettings().setMapToolbarEnabled(false);
        map.getUiSettings().setZoomControlsEnabled(false);
        map.getUiSettings().setRotateGesturesEnabled(false);
        map.getUiSettings().setTiltGesturesEnabled(false);
    }

    /**
     * @param tracking the latest data
     */
    public void show(@NonNull Tracking tracking) {
        map.clear();
        List<LatLng> route = new ArrayList<>();
        add(route, tracking.getShop(), shopIcon, CENTER);
        add(route, tracking.isLive() ? tracking.getRiderPosition() : null, riderIcon, CENTER);
        add(route, tracking.getPin(), pinIcon, BOTTOM);
        if (route.size() > 1) {
            map.addPolyline(new PolylineOptions().addAll(route).color(routeColor).width(routeWidth)
                    .pattern(List.of(new Dash(dash), new Gap(gap))));
        }
        frame(route);
    }

    private void add(List<LatLng> route, @Nullable Location point, BitmapDescriptor icon, float anchorV) {
        if (point == null) {
            return;
        }
        LatLng latLng = new LatLng(point.getLatitude().doubleValue(), point.getLongitude().doubleValue());
        route.add(latLng);
        map.addMarker(new MarkerOptions().position(latLng).icon(icon).anchor(CENTER, anchorV));
    }

    private void frame(List<LatLng> points) {
        if (points.isEmpty()) {
            return;
        }
        if (points.size() == 1 || mapView.getWidth() == 0) {
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(points.get(0), STOP_ZOOM));
            return;
        }
        LatLngBounds.Builder bounds = new LatLngBounds.Builder();
        points.forEach(bounds::include);
        map.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds.build(), mapView.getWidth(), mapView.getHeight(),
                padding));
    }
}
