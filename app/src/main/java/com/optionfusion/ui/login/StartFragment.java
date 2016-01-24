package com.optionfusion.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.client.FusionClient;
import com.optionfusion.client.GoogClient;
import com.optionfusion.module.OptionFusionApplication;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StartFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    public static final int RC_SIGN_IN = 8000;

    private GoogleApiClient googleApiClient;

    private static final String TAG = "StartFragment";

    public static Fragment newInstance() {
        return new StartFragment();
    }

    @Bind(R.id.sign_in_button)
    SignInButton signInButton;

    @Inject
    FusionClient fusionClient;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.splash, container, false);
        ButterKnife.bind(this, ret);

        // Configure sign-in to request the user's ID, email address, and basic
        // profile. ID and basic profile are included in DEFAULT_SIGN_IN.
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestProfile()
                .requestId()
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);
        signInButton.setScopes(gso.getScopeArray());


        return ret;
    }

    @OnClick(R.id.sign_in_button)
    public void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.i(TAG, result.toString());
            handleSignInResult(result);
        }
    }

    private void handleSignInResult(GoogleSignInResult result) {
        if (result.isSuccess()) {
            // Signed in successfully, show authenticated UI.
            GoogleSignInAccount acct = result.getSignInAccount();
            String dname = acct.getDisplayName();
        } else {
            Log.e(TAG, "Sign in failed");
            // Signed out, show unauthenticated UI.
        }
    }

//    @OnClick({R.id.no_thanks, R.id.logo_ameritrade})
//    public void onClickAmeritrade(View view) {
//        switch (view.getId()) {
//            case R.id.logo_ameritrade:
//                getFragmentHost().startLogin(AMERITRADE);
//                break;
//            case R.id.no_thanks:
//            default:
//                getFragmentHost().startLogin(GOOGLE_FINANCE);
//        }
//    }

    private Host getFragmentHost() {
        return (Host)getActivity();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), "Connection Failed: " + connectionResult.toString(), Toast.LENGTH_SHORT);
    }

    public interface Host {
        void startLogin(OptionFusionApplication.Provider provider);
    }
}
