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
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.client.FusionClientProvider;
import com.optionfusion.module.OptionFusionApplication;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class StartFragment extends Fragment implements GoogleApiClient.OnConnectionFailedListener {

    public static final int RC_SIGN_IN = 8000;

    private static final String TAG = "StartFragment";

    public static Fragment newInstance() {
        return new StartFragment();
    }

    @Bind(R.id.sign_in_button)
    SignInButton signInButton;

    @Inject
    FusionClientProvider fusionClientProvider;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.splash, container, false);
        ButterKnife.bind(this, ret);

        signInButton.setSize(SignInButton.SIZE_WIDE);
        signInButton.setColorScheme(SignInButton.COLOR_LIGHT);

        return ret;
    }

    @OnClick(R.id.sign_in_button)
    public void signIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(fusionClientProvider.getGoogleApiClient());
        signInIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
        startActivityForResult(signInIntent, RC_SIGN_IN);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            Log.d(TAG, result.toString());
            fusionClientProvider.handleSignInResult(result);
            getFragmentHost().startLogin(OptionFusionApplication.Provider.OPTION_FUSION_BACKEND);
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
        return (Host) getActivity();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), "Connection Failed: " + connectionResult.toString(), Toast.LENGTH_SHORT);
    }

    public interface Host {
        void startLogin(OptionFusionApplication.Provider provider);
    }
}
