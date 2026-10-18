package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.auth.SignUpForm;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/** 00b "Create account" state. */
// OOP: Inheritance — an Android ViewModel that survives rotation.
public class SignUpViewModel extends ViewModel {

    // OOP: Encapsulation — screens observe read-only LiveData.
    private final AuthRepository auth;
    private final MutableLiveData<SignUpFormState> form = new MutableLiveData<>(SignUpFormState.idle());
    private final MutableLiveData<Event<Role>> signedIn = new MutableLiveData<>();

    /**
     * @param auth sign-up calls
     */
    public SignUpViewModel(@NonNull AuthRepository auth) {
        this.auth = auth;
    }

    /** @return form state */
    @NonNull
    public LiveData<SignUpFormState> getForm() {
        return form;
    }

    /** @return fires once with the new customer's role */
    @NonNull
    public LiveData<Event<Role>> getSignedIn() {
        return signedIn;
    }

    /**
     * "Create account".
     *
     * @param entered the fields as typed (trimmed)
     */
    public void createAccount(@NonNull SignUpForm entered) {
        SignUpFormState current = form.getValue();
        if (current != null && current.loading()) {
            return;
        }
        Map<SignUpField, Integer> invalid = SignUpValidator.validate(entered);
        if (!invalid.isEmpty()) {
            Map<SignUpField, UiText> errors = new EnumMap<>(SignUpField.class);
            invalid.forEach((field, res) -> errors.put(field, UiText.of(res)));
            form.setValue(new SignUpFormState(false, errors, null));
            return;
        }
        SignUpForm cleaned = new SignUpForm(entered.fullName(), AuthValidator.normalizeMobile(entered.mobileNumber()),
                entered.email(), entered.password(), entered.confirmPassword(), true);
        form.setValue(new SignUpFormState(true, Collections.emptyMap(), null));
        auth.register(cleaned, callback());
    }

    /**
     * @param idToken Google ID token from Credential Manager
     */
    public void signInWithGoogle(@NonNull String idToken) {
        form.setValue(new SignUpFormState(true, Collections.emptyMap(), null));
        auth.signInWithGoogle(idToken, callback());
    }

    /**
     * @param message why Google sign-in failed
     */
    public void googleFailed(@StringRes int message) {
        form.setValue(new SignUpFormState(false, Collections.emptyMap(), UiText.of(message)));
    }

    private ApiCallback<Session> callback() {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Session session) {
                form.setValue(SignUpFormState.idle());
                signedIn.setValue(new Event<>(session.getRole()));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                Map<SignUpField, UiText> errors = new EnumMap<>(SignUpField.class);
                error.getFieldErrors().forEach((name, message) -> {
                    SignUpField field = SignUpField.fromApi(name);
                    if (field != null) {
                        errors.put(field, UiText.of(message));
                    }
                });
                form.setValue(new SignUpFormState(false, errors, errors.isEmpty() ? ErrorMapper.bannerFor(error) : null));
            }
        };
    }
}
