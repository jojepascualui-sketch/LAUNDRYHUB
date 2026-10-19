package com.laundryhub.app.ui.common;

import android.graphics.Color;
import android.view.View;

import androidx.activity.ComponentActivity;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.laundryhub.app.R;

/** Edge-to-edge helpers so screens look like the design under the system bars. */
public final class SystemBars {

    private SystemBars() {
    }

    /**
     * For screens with the blue header: white status-bar icons, light navigation bar.
     *
     * @param activity the screen
     */
    public static void enableForBlueHeader(@NonNull ComponentActivity activity) {
        int bg = ContextCompat.getColor(activity, R.color.bg);
        EdgeToEdge.enable(activity, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.light(bg, bg));
    }

    /**
     * For screens on the light background: dark icons on both bars.
     *
     * @param activity the screen
     */
    public static void enableForLightScreen(@NonNull ComponentActivity activity) {
        int bg = ContextCompat.getColor(activity, R.color.bg);
        EdgeToEdge.enable(activity, SystemBarStyle.light(bg, bg), SystemBarStyle.light(bg, bg));
    }

    /**
     * For screens that start with a full-bleed map (R2/R3): the map shows through a transparent
     * status bar with dark icons; white navigation bar under the white footer.
     *
     * @param activity the screen
     */
    public static void enableForMapScreen(@NonNull ComponentActivity activity) {
        int surface = ContextCompat.getColor(activity, R.color.surface);
        EdgeToEdge.enable(activity, SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
                SystemBarStyle.light(surface, surface));
    }

    /**
     * Keeps the header's design top padding (46 dp) unless the status bar is taller, and pads
     * the bottom of the scrolling content above the navigation bar / keyboard.
     *
     * @param header        the blue header content
     * @param designTop     header top padding from the design
     * @param minGapBelowBar minimum space between status bar and header content
     * @param scrollContent the scrolling form
     */
    public static void applyAuthInsets(@NonNull View header, int designTop, int minGapBelowBar,
                                       @NonNull View scrollContent) {
        int contentBottom = scrollContent.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(header.getRootView(), (root, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.displayCutout());
            Insets ime = windowInsets.getInsets(WindowInsetsCompat.Type.ime());
            header.setPadding(header.getPaddingLeft(), Math.max(designTop, bars.top + minGapBelowBar),
                    header.getPaddingRight(), header.getPaddingBottom());
            scrollContent.setPadding(scrollContent.getPaddingLeft(), scrollContent.getPaddingTop(),
                    scrollContent.getPaddingRight(), contentBottom + Math.max(bars.bottom, ime.bottom));
            return WindowInsetsCompat.CONSUMED;
        });
    }

    /**
     * Pads a plain screen by the system bars.
     *
     * @param root the screen's root view
     */
    public static void padForSystemBars(@NonNull View root) {
        int left = root.getPaddingLeft();
        int top = root.getPaddingTop();
        int right = root.getPaddingRight();
        int bottom = root.getPaddingBottom();
        ViewCompat.setOnApplyWindowInsetsListener(root, (v, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(left + bars.left, top + bars.top, right + bars.right, bottom + bars.bottom);
            return WindowInsetsCompat.CONSUMED;
        });
    }
}
