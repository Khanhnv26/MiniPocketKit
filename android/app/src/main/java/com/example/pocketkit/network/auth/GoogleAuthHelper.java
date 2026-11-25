package com.example.pocketkit.network.auth;

import android.app.Activity;

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
import androidx.credentials.exceptions.NoCredentialException;

import com.example.pocketkit.BuildConfig;
import com.example.pocketkit.R;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;

public final class GoogleAuthHelper {

    public interface Callback {
        void onIdTokenReceived(@NonNull String idToken);

        void onCancelled();

        void onError(@NonNull String message);
    }

    private GoogleAuthHelper() {
    }

    public static void requestIdToken(@NonNull Activity activity, @NonNull Callback callback) {
        String webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID != null
                ? BuildConfig.GOOGLE_WEB_CLIENT_ID.trim()
                : "";
        if (webClientId.isEmpty()) {
            callback.onError(activity.getString(R.string.cloud_google_not_configured));
            return;
        }

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        CredentialManager credentialManager = CredentialManager.create(activity);
        credentialManager.getCredentialAsync(
                activity,
                request,
                null,
                ContextCompat.getMainExecutor(activity),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential
                                && GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                            try {
                                GoogleIdTokenCredential tokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = tokenCredential.getIdToken();
                                if (idToken != null && !idToken.trim().isEmpty()) {
                                    callback.onIdTokenReceived(idToken.trim());
                                    return;
                                }
                            } catch (Exception ignored) {
                            }
                        }
                        callback.onError(activity.getString(R.string.cloud_google_failed));
                    }

                    @Override
                    public void onError(@NonNull GetCredentialException e) {
                        if (e instanceof GetCredentialCancellationException) {
                            callback.onCancelled();
                            return;
                        }
                        if (e instanceof NoCredentialException) {
                            callback.onError(activity.getString(R.string.cloud_google_no_account));
                            return;
                        }
                        callback.onError(activity.getString(R.string.cloud_google_failed));
                    }
                }
        );
    }
}
