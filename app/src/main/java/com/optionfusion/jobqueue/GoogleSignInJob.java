package com.optionfusion.jobqueue;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.optionfusion.BuildConfig;
import com.optionfusion.util.Constants;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class GoogleSignInJob extends BaseApiJob implements GoogleApiClient.OnConnectionFailedListener {
    private final FragmentActivity activity;
    private boolean mResolvingError;
    private GoogleApiClient apiClient;

    private static final String TAG = "GoogleSignInJob";
    private GoogleSignInAccount account;

    public GoogleSignInJob(FragmentActivity activity) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .singleInstanceBy(GoogleSignInJob.class.getSimpleName()));
        this.activity = activity;

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Constants.WEB_CLIENT_ID)
                .requestEmail()
                .requestId()
                .build();

        apiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .enableAutoManage(activity, this)
                .build();
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();

        if (!TextUtils.isEmpty(sharedPrefStore.getSessionId())) {
            Log.i(TAG, "Session restored");
            return;
        }

        GoogleSignInResult signInResult = trySilentSignIn();

        if (signInResult == null || !signInResult.isSuccess()) {

        }
    }

    private static final int REQUEST_RESOLVE_ERROR = 1001;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult result) {
        if (mResolvingError) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                mResolvingError = true;
                result.startResolutionForResult(activity, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                apiClient.connect();
            }
        } else {
            // Show dialog using GoogleApiAvailability.getErrorDialog()
            mResolvingError = true;
        }
    }

    public GoogleSignInResult trySilentSignIn() {
        Log.v(TAG, "trySilentSignIn", new RuntimeException("TACO"));

        GoogleSignInResult signinResult;
        try {
            signinResult =
                    Auth.GoogleSignInApi.silentSignIn(apiClient).await(15, TimeUnit.SECONDS);
        } catch (Throwable t) {
            return null;
        }

        if (signinResult != null && signinResult.isSuccess()) {
            account = signinResult.getSignInAccount();
            Log.i(TAG, "Account updated " + account);
            sharedPrefStore.setEmail(account.getEmail());
        }

        if (account == null) {
            return null;
        }

        if (BuildConfig.DEBUG) {
            try {
                GoogleIdToken token = GoogleIdToken.parse(new AndroidJsonFactory(), account.getIdToken());
                if (token.getPayload().getExpirationTimeSeconds() * 1000 < System.currentTimeMillis()) {
                    Log.e(TAG, "Token is expired " + token.getPayload().getExpirationTimeSeconds());
                } else {
                    Log.d(TAG, "Token is NOT expired");
                    GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new AndroidJsonFactory())
                            .setAudience(Arrays.asList(Constants.AUDIENCE_ANDROID_CLIENT_ID))
                            // If you retrieved the token on Android using the Play Services 8.3 API or newer, set
                            // the issuer to "https://accounts.google.com". Otherwise, set the issuer to
                            // "accounts.google.com". If you need to verify tokens from multiple sources, build
                            // a GoogleIdTokenVerifier for each issuer and try them both.
                            .setIssuer("https://accounts.google.com")
                            .build();

                    if (!verifier.verify(token)) {
                        Log.e(TAG, "Token failed verify " + token.getPayload().getAudience());
                    } else {
                        Log.i(TAG, "Token Verified");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            }
        }

        return signinResult;
    }

}
