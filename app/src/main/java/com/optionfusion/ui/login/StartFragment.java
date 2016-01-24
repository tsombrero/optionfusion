package com.optionfusion.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.module.OptionFusionApplication;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StartFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    public static final int RC_SIGN_IN = 8000;

    private GoogleApiClient googleApiClient;

    public static Fragment newInstance() {
        return new StartFragment();
    }

    @Bind(R.id.sign_in_button)
    SignInButton signInButton;

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
                .build();

        // Build a GoogleApiClient with access to the Google Sign-In API and the
        // options specified by gso.
        googleApiClient = new GoogleApiClient.Builder(getActivity())
                .enableAutoManage(getActivity(), this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();

        signInButton.setSize(SignInButton.SIZE_STANDARD);
        signInButton.setScopes(gso.getScopeArray());

        return ret;
    }

    @OnClick(R.id.sign_in_button)
    public void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(googleApiClient);
        startActivityForResult(signInIntent, RC_SIGN_IN);
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
