package com.laundryhub.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.AccessibilityDelegateCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat;

import com.laundryhub.app.R;

/**
 * The rider's "On duty / Off duty" pill (R1/R5): #1D7A4A with the knob right when on duty,
 * #4A5863 with the knob left when off. A tap asks the listener; the screen calls
 * {@link #setOnDuty(boolean)} once the server has saved the change.
 */
// OOP: Inheritance — a LinearLayout that draws the design's switch.
public class DutySwitch extends LinearLayout {

    /** Receives a tap. */
    public interface OnToggleListener {
        /**
         * @param onDuty the position the rider asked for
         */
        void onToggle(boolean onDuty);
    }

    // OOP: Encapsulation — the label and knob are private; callers only set the duty state.
    private final TextView label;
    private final View knob;
    private boolean onDuty;
    @Nullable
    private OnToggleListener listener;

    /**
     * @param context context
     */
    public DutySwitch(@NonNull Context context) {
        this(context, null);
    }

    /**
     * @param context context
     * @param attrs   XML attributes
     */
    public DutySwitch(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(HORIZONTAL);
        setGravity(Gravity.CENTER_VERTICAL);
        setBackgroundResource(R.drawable.bg_duty_button);
        setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.touch_target));
        setPaddingRelative(getResources().getDimensionPixelSize(R.dimen.duty_padding_start), 0,
                getResources().getDimensionPixelSize(R.dimen.duty_padding_end), 0);
        setClickable(true);
        setFocusable(true);
        LayoutInflater.from(context).inflate(R.layout.view_duty_switch, this, true);
        label = findViewById(R.id.duty_label);
        knob = findViewById(R.id.duty_knob);
        setOnClickListener(v -> {
            if (listener != null) {
                listener.onToggle(!onDuty);
            }
        });
        ViewCompat.setAccessibilityDelegate(this, new AccessibilityDelegateCompat() {
            @Override
            public void onInitializeAccessibilityNodeInfo(@NonNull View host, @NonNull AccessibilityNodeInfoCompat info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Switch.class.getName());
                info.setCheckable(true);
                info.setChecked(onDuty);
            }
        });
        render();
    }

    /**
     * @param onDuty switch position from the server
     */
    public void setOnDuty(boolean onDuty) {
        this.onDuty = onDuty;
        render();
    }

    /** @return the switch position */
    public boolean isOnDuty() {
        return onDuty;
    }

    /**
     * @param listener called on tap; {@code null} to remove
     */
    public void setOnToggleListener(@Nullable OnToggleListener listener) {
        this.listener = listener;
    }

    private void render() {
        setActivated(onDuty);
        label.setText(onDuty ? R.string.on_duty : R.string.off_duty);
        setContentDescription(label.getText());
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) knob.getLayoutParams();
        params.gravity = Gravity.CENTER_VERTICAL | (onDuty ? Gravity.END : Gravity.START);
        knob.setLayoutParams(params);
    }
}
