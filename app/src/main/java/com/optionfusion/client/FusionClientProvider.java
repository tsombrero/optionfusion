package com.optionfusion.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.util.Constants;

public class FusionClientProvider extends ClientProvider implements ClientProvider.SymbolLookupClientProvider, ClientProvider.OptionChainClientProvider, ClientProvider.AccountClientProvider {

    FusionClient client;
    Context context;

    private static final String TAG = "FusionClientProvider";
    private GoogleSignInAccount acct;
    private SharedPreferences settings;
    private GoogleApiClient googleApiClient;

    public FusionClientProvider(Context context) {
        this.context = context;
    }

    @Override
    public ClientInterfaces.SymbolLookupClient getSymbolLookupClient() {
        if (client == null)
            client = new FusionClient(context, acct);
        return client;
    }

    public void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            acct = result.getSignInAccount();

            client = new FusionClient(context, acct);

            if (result.getSignInAccount() != null) {
                setAccountName(result.getSignInAccount().getEmail());
                // User is authorized.
            }
        } else {
            Log.e(TAG, "Sign in failed");
            // Signed out, show unauthenticated UI.
        }
        client = null;
    }

    private void setAccountName(String accountName) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putString("ACCOUNT_NAME", accountName);
        editor.commit();
        client = null;
    }

    public GoogleApiClient initGoogleApiClient() {
        settings = context.getSharedPreferences(TAG, 0);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .requestIdToken(Constants.WEB_CLIENT_ID)
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        googleApiClient = new GoogleApiClient.Builder(context)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        googleApiClient.connect();
        return googleApiClient;
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient() {
        if (client == null && acct != null)
            client = new FusionClient(context, acct);
        return client;
    }

    @Override
    public ClientInterfaces.AccountClient getAccountClient() {
        if (client == null && acct != null)
            client = new FusionClient(context, acct);
        return client;
    }
}
