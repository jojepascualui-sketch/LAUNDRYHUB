package com.laundryhub.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.laundryhub.app.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The design's segmented control (00a "Customer / Rider / Admin", A4 tabs): a #E3E8E4 track
 * with equal-width segments; the selected one is a white pill with ink text.
 * Styles: {@code Widget.LaundryHub.SegmentedControl} and {@code ...SegmentedControl.Segment}.
 */
// OOP: Inheritance — a LinearLayout that adds segment selection.
public class SegmentedControl extends LinearLayout {

    /** Receives the index of the segment the user picked. */
    // OOP: Abstraction — screens only learn "segment N was picked".
    public interface OnSegmentSelectedListener {
        /**
         * @param index the picked segment, 0-based
         */
        void onSegmentSelected(int index);
    }

    // OOP: Encapsulation — segments and selection change only through setSegments/setSelectedIndex.
    private final List<TextView> segments = new ArrayList<>();
    private int selectedIndex;
    @Nullable
    private OnSegmentSelectedListener listener;

    /**
     * @param context context
     */
    public SegmentedControl(@NonNull Context context) {
        this(context, null);
    }

    /**
     * @param context context
     * @param attrs   XML attributes ({@code app:segments}, {@code app:selectedIndex})
     */
    public SegmentedControl(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs, 0, R.style.Widget_LaundryHub_SegmentedControl);
        setOrientation(HORIZONTAL);
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.SegmentedControl, 0,
                R.style.Widget_LaundryHub_SegmentedControl);
        try {
            CharSequence[] labels = a.getTextArray(R.styleable.SegmentedControl_segments);
            if (labels != null) {
                setSegments(labels);
            }
            setSelectedIndex(a.getInt(R.styleable.SegmentedControl_selectedIndex, 0));
        } finally {
            a.recycle();
        }
        if (getBackground() == null) {
            setBackgroundResource(R.drawable.bg_segmented_track);
        }
        int padding = getResources().getDimensionPixelSize(R.dimen.segmented_padding);
        setPadding(padding, padding, padding, padding);
    }

    /**
     * Replaces the segments.
     *
     * @param labels one label per segment, in order
     */
    public void setSegments(@NonNull CharSequence[] labels) {
        removeAllViews();
        segments.clear();
        int gap = getResources().getDimensionPixelSize(R.dimen.segmented_gap);
        for (int i = 0; i < labels.length; i++) {
            TextView segment = new TextView(getContext(), null, 0, R.style.Widget_LaundryHub_SegmentedControl_Segment);
            segment.setText(labels[i]);
            segment.setGravity(Gravity.CENTER);
            segment.setClickable(true);
            segment.setFocusable(true);
            final int index = i;
            segment.setOnClickListener(v -> select(index, true));
            ViewCompat.setAccessibilityDelegate(segment, new androidx.core.view.AccessibilityDelegateCompat() {
                @Override
                public void onInitializeAccessibilityNodeInfo(@NonNull android.view.View host,
                                                              @NonNull AccessibilityNodeInfoCompat info) {
                    super.onInitializeAccessibilityNodeInfo(host, info);
                    info.setClassName(android.widget.RadioButton.class.getName());
                    info.setCheckable(true);
                    info.setChecked(host.isSelected());
                }
            });
            LayoutParams params = new LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f);
            if (i > 0) {
                params.setMarginStart(gap);
            }
            addView(segment, params);
            segments.add(segment);
        }
        select(Math.min(selectedIndex, Math.max(0, labels.length - 1)), false);
    }

    /**
     * @param listener called when the user picks a segment; {@code null} to remove
     */
    public void setOnSegmentSelectedListener(@Nullable OnSegmentSelectedListener listener) {
        this.listener = listener;
    }

    /**
     * Selects a segment without notifying the listener (e.g. when restoring state).
     *
     * @param index segment to select
     * @throws IllegalArgumentException if the index is out of range
     */
    public void setSelectedIndex(int index) {
        if (segments.isEmpty()) {
            selectedIndex = Math.max(0, index);
            return;
        }
        if (index < 0 || index >= segments.size()) {
            throw new IllegalArgumentException("Segment index " + index + " is out of range");
        }
        select(index, false);
    }

    /** @return the selected segment index */
    public int getSelectedIndex() {
        return selectedIndex;
    }

    /** @return the segment views, read-only */
    @NonNull
    public List<TextView> getSegmentViews() {
        return Collections.unmodifiableList(segments);
    }

    private void select(int index, boolean fromUser) {
        selectedIndex = index;
        for (int i = 0; i < segments.size(); i++) {
            segments.get(i).setSelected(i == index);
        }
        if (fromUser && listener != null) {
            listener.onSegmentSelected(index);
        }
    }
}
