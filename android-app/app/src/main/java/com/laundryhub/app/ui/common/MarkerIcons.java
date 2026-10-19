package com.laundryhub.app.ui.common;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

/** Turns the design's vector drawables into Google Maps marker icons. */
public final class MarkerIcons {

    private MarkerIcons() {
    }

    /**
     * @param context  for the drawable
     * @param drawable vector or layer-list with an intrinsic size
     * @return the marker icon (the default marker if the drawable is missing)
     */
    @NonNull
    public static BitmapDescriptor fromDrawable(@NonNull Context context, @DrawableRes int drawable) {
        Drawable d = ContextCompat.getDrawable(context, drawable);
        if (d == null || d.getIntrinsicWidth() <= 0 || d.getIntrinsicHeight() <= 0) {
            return BitmapDescriptorFactory.defaultMarker();
        }
        Bitmap bitmap = Bitmap.createBitmap(d.getIntrinsicWidth(), d.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        d.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
        d.draw(new Canvas(bitmap));
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }
}
