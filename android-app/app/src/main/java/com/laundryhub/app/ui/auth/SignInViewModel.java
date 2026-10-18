package com.laundryhub.app.ui.auth;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.laundryhub.app.data.ApiCallback;
import com.laundryhub.app.data.ApiException;
import com.laundryhub.app.data.auth.AuthRepository;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.model.Role;
import com.laundryhub.app.ui.common.Event;
import com.laundryhub.app.ui.common.UiText;

import java.util.Map;

/** 00a / 00c state: selected tab, form errors, and the one-time "signed in" event. */
// OOP: Inheritance — an Android ViewModel that survives rotation.
public class SignInViewModel extends ViewModel {

    /** Server field name for the email / mobile input. */
    static final String FIELD_EMAIL = "email";

    /** Server field name for the password input. */
    static final String FIELD_PASSWORD = "password";

    // OOP: Encapsulation — screens observe read-only LiveData; only this class changes it.
    private final AuthRepository auth;
    private final MutableLiveData<SignInTab> tab = new MutableLiveData<>(SignInTab.CUSTOMER);
    private final MutableLiveData<SignInFormState> form = new MutableLiveData<>(SignInFormState.idle());
    private final MutableLiveData<Event<Role>> signedIn = new MutableLiveData<>();

    /**
     * @param auth sign-in calls
     */
    public SignInViewModel(@NonNull AuthRepository auth) {
        this.auth = auth;
    }

    /** @return selected tab */
    @NonNull
    public LiveData<SignInTab> getTab() {
        return tab;
    }

    /** @return form state */
    @NonNull
    public LiveData<SignInFormState> getForm() {
        return form;
    }

    /** @return fires once with the signed-in role */
    @NonNull
    public LiveData<Event<Role>> getSignedIn() {
        return signedIn;
    }

    /**
     * Customer / Rider / Admin segment picked.
     *
     * @param selected the tab
     */
    public void selectTab(@NonNull SignInTab selected) {
        if (selected != tab.getValue()) {
            tab.setValue(selected);
            form.setValue(SignInFormState.idle());
        }
    }

    /**
     * "Sign in".
     *
     * @param identifier email (or rider mobile), trimmed
     * @param password   password
     * @param remember   "Remember me"
     */
    public void signIn(@NonNull String identifier, @NonNull String password, boolean remember) {
        if (isLoading()) {
            return;
        }
        SignInTab current = currentTab();
        Integer idError = current.validateIdentifier(identifier);
        UiText pwError = password.isEmpty() ? UiText.of(com.laundryhub.app.R.string.error_required) : null;
        if (idError != null || pwError != null) {
            form.setValue(new SignInFormState(false, idError == null ? null : UiText.of(idError), pwError, null));
            return;
        }
        form.setValue(SignInFormState.busy());
        auth.signIn(current.normalizeIdentifier(identifier), password, current.getRole(), remember, sessionCallback());
    }

    /**
     * Credential Manager returned a Google ID token.
     *
     * @param idToken the token
     */
    public void signInWithGoogle(@NonNull String idToken) {
        if (isLoading()) {
            return;
        }
        form.setValue(SignInFormState.busy());
        auth.signInWithGoogle(idToken, sessionCallback());
    }

    /**
     * Google sign-in did not produce a token.
     *
     * @param message why, as a string resource
     */
    public void googleFailed(@StringRes int message) {
        form.setValue(new SignInFormState(false, null, null, UiText.of(message)));
    }

    private ApiCallback<Session> sessionCallback() {
        return new ApiCallback<>() {
            @Override
            public void onSuccess(@NonNull Session session) {
                form.setValue(SignInFormState.idle());
                signedIn.setValue(new Event<>(session.getRole()));
            }

            @Override
            public void onError(@NonNull ApiException error) {
                Map<String, String> fields = error.getFieldErrors();
                UiText id = fields.containsKey(FIELD_EMAIL) ? UiText.of(fields.get(FIELD_EMAIL)) : null;
                UiText pw = fields.containsKey(FIELD_PASSWORD) ? UiText.of(fields.get(FIELD_PASSWORD)) : null;
                UiText banner = id == null && pw == null ? ErrorMapper.bannerFor(error) : null;
                form.setValue(new SignInFormState(false, id, pw, banner));
            }
        };
    }

    private boolean isLoading() {
        SignInFormState state = form.getValue();
        return state != null && state.loading();
    }

    private SignInTab currentTab() {
        SignInTab value = tab.getValue();
        return value == null ? SignInTab.CUSTOMER : value;
    }
}
