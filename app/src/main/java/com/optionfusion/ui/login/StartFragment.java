package com.optionfusion.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.client.ClientInterfaces;
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

    @Bind(R.id.sign_in_text)
    TextView signInText;

    @Inject
    ClientInterfaces.AccountClient accountClient;

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
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(((Host) getActivity()).getGoogleApiClient());
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
            if (result.isSuccess()) {
                accountClient.setGoogleAccount(result.getSignInAccount());
                getFragmentHost().startLogin(OptionFusionApplication.Provider.OPTION_FUSION_BACKEND);
            } else {
                Toast.makeText(getActivity(), getString(R.string.error_sign_in), Toast.LENGTH_SHORT);
            }
        }
    }

    private Host getFragmentHost() {
        return (Host) getActivity();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Toast.makeText(getActivity(), "Connection Failed: " + connectionResult.toString(), Toast.LENGTH_SHORT);
    }

    public interface Host {
        void startLogin(OptionFusionApplication.Provider provider);

        GoogleApiClient getGoogleApiClient();
    }
}
