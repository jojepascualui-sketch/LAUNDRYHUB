package com.laundryhub.app.ui.auth;

import android.app.Activity;
import android.os.CancellationSignal;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.laundryhub.app.R;

/**
 * Google sign-in through Android Credential Manager. Needs the OAuth web client id in
 * {@code GOOGLE_WEB_CLIENT_ID} (android-app/local.properties), the same id the backend checks.
 */
// OOP: Inheritance — implements the GoogleSignInClient contract.
public class CredentialManagerGoogleSignIn implements GoogleSignInClient {

    // OOP: Encapsulation — the client id is fixed at construction.
    private final String webClientId;

    /**
     * @param webClientId OAuth web client id; blank disables Google sign-in
     */
    public CredentialManagerGoogleSignIn(@NonNull String webClientId) {
        this.webClientId = webClientId;
    }

    // OOP: Polymorphism — the Credential Manager implementation of signIn().
    @Override
    public void signIn(@NonNull Activity activity, @NonNull Callback callback) {
        if (webClientId.isBlank()) {
            callback.onFailure(R.string.google_not_configured);
            return;
        }
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(new GetSignInWithGoogleOption.Builder(webClientId).build())
                .build();
        CredentialManager.create(activity).getCredentialAsync(activity, request, new CancellationSignal(),
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse response) {
                        Credential credential = response.getCredential();
                        if (credential instanceof CustomCredential custom
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(custom.getType())) {
                            callback.onIdToken(GoogleIdTokenCredential.createFrom(custom.getData()).getIdToken());
                        } else {
                            callback.onFailure(R.string.error_unexpected);
                        }
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        callback.onFailure(e instanceof GetCredentialCancellationException
                                ? R.string.google_cancelled : R.string.error_unexpected);
                    }
                });
    }
}
