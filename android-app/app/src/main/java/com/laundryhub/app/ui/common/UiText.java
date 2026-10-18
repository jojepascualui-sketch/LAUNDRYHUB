package com.laundryhub.app.ui.common;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;

import java.util.Objects;

/**
 * Text for the UI that is either a string resource (our own messages) or a literal
 * (a message from the server). Lets ViewModels stay free of Context.
 */
public final class UiText {

    // OOP: Encapsulation — exactly one of the two is set.
    @StringRes
    private final int resId;
    @Nullable
    private final String literal;

    private UiText(@StringRes int resId, @Nullable String literal) {
        this.resId = resId;
        this.literal = literal;
    }

    /**
     * @param resId string resource
     * @return the text
     */
    @NonNull
    public static UiText of(@StringRes int resId) {
        return new UiText(resId, null);
    }

    /**
     * @param text literal text
     * @return the text
     * @throws IllegalArgumentException if the text is null
     */
    @NonNull
    public static UiText of(@NonNull String text) {
        if (text == null) {
            throw new IllegalArgumentException("Text is required");
        }
        return new UiText(0, text);
    }

    /**
     * @param context for resources
     * @return the text to show
     */
    @NonNull
    public String resolve(@NonNull Context context) {
        return literal != null ? literal : context.getString(resId);
    }

    /** @return the resource id, or 0 for a literal */
    @StringRes
    public int getResId() {
        return resId;
    }

    /** @return the literal, or {@code null} for a resource */
    @Nullable
    public String getLiteral() {
        return literal;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof UiText other && resId == other.resId && Objects.equals(literal, other.literal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(resId, literal);
    }

    @NonNull
    @Override
    public String toString() {
        return literal != null ? literal : "@string/" + resId;
    }
}
