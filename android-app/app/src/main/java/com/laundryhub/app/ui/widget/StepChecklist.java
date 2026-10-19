package com.laundryhub.app.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.TextViewCompat;

import com.laundryhub.app.R;

import java.util.List;

/**
 * The job checklist on R2/R3: a dot per step joined by a line. Done steps are accent with a white
 * check, the current step has an accent ring and a bold label, upcoming steps are grey.
 */
// OOP: Inheritance — a vertical LinearLayout of step rows.
public class StepChecklist extends LinearLayout {

    /**
     * @param context context
     */
    public StepChecklist(@NonNull Context context) {
        this(context, null);
    }

    /**
     * @param context context
     * @param attrs   XML attributes
     */
    public StepChecklist(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    /**
     * @param titles  step titles in order
     * @param current index of the current step; {@code titles.size()} when all are done
     */
    public void setSteps(@NonNull List<String> titles, int current) {
        removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(getContext());
        for (int i = 0; i < titles.size(); i++) {
            View row = inflater.inflate(R.layout.item_job_step, this, false);
            boolean hasLine = i < titles.size() - 1;
            bind(row, titles.get(i), i < current, i == current, hasLine);
            // A row with a line is at least dot + 18 dp line tall (CSS: line flex 1, min-height 18).
            row.setMinimumHeight(hasLine ? lineRowHeight() : 0);
            addView(row);
        }
    }

    private int lineRowHeight() {
        return getResources().getDimensionPixelSize(R.dimen.step_dot)
                + getResources().getDimensionPixelSize(R.dimen.step_line_min);
    }

    private void bind(View row, String title, boolean done, boolean isCurrent, boolean hasLine) {
        View dot = row.findViewById(R.id.dot);
        View check = row.findViewById(R.id.check);
        View line = row.findViewById(R.id.line);
        TextView label = row.findViewById(R.id.label);

        dot.setBackgroundResource(done ? R.drawable.bg_step_done
                : isCurrent ? R.drawable.bg_step_current : R.drawable.bg_step_upcoming);
        check.setVisibility(done ? VISIBLE : GONE);
        line.setVisibility(hasLine ? VISIBLE : GONE);
        line.setBackgroundColor(ContextCompat.getColor(getContext(), done ? R.color.accent : R.color.border));

        label.setText(title);
        TextViewCompat.setTextAppearance(label, isCurrent ? R.style.TextAppearance_LaundryHub_StepLabel_Current
                : R.style.TextAppearance_LaundryHub_StepLabel);
        label.setTextColor(ContextCompat.getColor(getContext(), done || isCurrent ? R.color.ink : R.color.subtle));
        label.setContentDescription(done ? getContext().getString(R.string.step_done, title)
                : isCurrent ? getContext().getString(R.string.step_current, title) : title);
    }
}
