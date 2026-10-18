package com.laundryhub.app.ui.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.text.InputType;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import com.laundryhub.app.R;

/**
 * The design's TextInput with its label: 13 sp bold label, 6 dp gap, 50 dp white input with a
 * 1.5 dp #C8D0CA border and 14 dp radius, and an error line (not in the mockups; warn color).
 * Layout: {@code res/layout/view_form_field.xml}; input style {@code Widget.LaundryHub.TextInput}.
 */
// OOP: Inheritance — a LinearLayout composed of label, input and error.
public class FormField extends LinearLayout {

    // OOP: Encapsulation — the child views are private; screens use getText/setLabel/setError.
    private final TextView label;
    private final EditText input;
    private final TextView error;

    /**
     * @param context context
     */
    public FormField(@NonNull Context context) {
        this(context, null);
    }

    /**
     * @param context context
     * @param attrs   {@code app:label}, {@code android:hint}, {@code android:inputType},
     *                {@code android:imeOptions}, {@code android:autofillHints}
     */
    public FormField(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        LayoutInflater.from(context).inflate(R.layout.view_form_field, this, true);
        label = findViewById(R.id.form_field_label);
        input = findViewById(R.id.form_field_input);
        error = findViewById(R.id.form_field_error);
        // Each FormField needs its own id so the system can save and restore the text.
        input.setId(View.generateViewId());

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.FormField);
        try {
            label.setText(a.getText(R.styleable.FormField_label));
            input.setHint(a.getText(R.styleable.FormField_android_hint));
            input.setInputType(a.getInt(R.styleable.FormField_android_inputType, InputType.TYPE_CLASS_TEXT));
            input.setImeOptions(a.getInt(R.styleable.FormField_android_imeOptions, EditorInfo.IME_ACTION_NEXT));
            String autofill = a.getString(R.styleable.FormField_android_autofillHints);
            if (autofill != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                input.setAutofillHints(autofill);
            }
        } finally {
            a.recycle();
        }
        input.setTypeface(androidx.core.content.res.ResourcesCompat.getFont(context, R.font.dm_sans));
        label.setLabelFor(input.getId());
    }

    /** @return the trimmed text */
    @NonNull
    public String getText() {
        return input.getText() == null ? "" : input.getText().toString().trim();
    }

    /**
     * @param text new text
     */
    public void setText(@Nullable CharSequence text) {
        input.setText(text);
    }

    /**
     * Changes the label (e.g. "Email" → "Staff email" when the tab changes).
     *
     * @param text label resource
     */
    public void setLabel(@StringRes int text) {
        label.setText(text);
    }

    /**
     * @param text placeholder resource
     */
    public void setHint(@StringRes int text) {
        input.setHint(text);
    }

    /**
     * Shows or hides the error line under the input.
     *
     * @param message the message, or {@code null} to hide it
     */
    public void setError(@Nullable CharSequence message) {
        error.setText(message);
        error.setVisibility(message == null ? GONE : VISIBLE);
        input.setContentDescription(message == null ? null : label.getText() + ". " + message);
    }

    /** @return the input, for IME actions and focus */
    @NonNull
    public EditText getInput() {
        return input;
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        input.setEnabled(enabled);
    }

    // The input has a generated id, so FormField saves the text itself (give FormField an id).
    @Override
    protected android.os.Parcelable onSaveInstanceState() {
        android.os.Bundle state = new android.os.Bundle();
        state.putParcelable(KEY_SUPER, super.onSaveInstanceState());
        state.putCharSequence(KEY_TEXT, input.getText());
        state.putCharSequence(KEY_ERROR, error.getVisibility() == VISIBLE ? error.getText() : null);
        return state;
    }

    @Override
    protected void onRestoreInstanceState(android.os.Parcelable state) {
        if (state instanceof android.os.Bundle bundle) {
            input.setText(bundle.getCharSequence(KEY_TEXT));
            setError(bundle.getCharSequence(KEY_ERROR));
            super.onRestoreInstanceState(bundle.getParcelable(KEY_SUPER));
        } else {
            super.onRestoreInstanceState(state);
        }
    }

    @Override
    protected void dispatchSaveInstanceState(android.util.SparseArray<android.os.Parcelable> container) {
        dispatchFreezeSelfOnly(container);
    }

    @Override
    protected void dispatchRestoreInstanceState(android.util.SparseArray<android.os.Parcelable> container) {
        dispatchThawSelfOnly(container);
    }

    private static final String KEY_SUPER = "super";
    private static final String KEY_TEXT = "text";
    private static final String KEY_ERROR = "error";
}
