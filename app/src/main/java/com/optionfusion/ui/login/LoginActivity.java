package com.optionfusion.ui.login;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.R;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.client.FusionClient;
import com.optionfusion.client.FusionClientProvider;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.MainActivity;
import com.optionfusion.util.SharedPrefStore;

import javax.inject.Inject;

public class LoginActivity extends AppCompatActivity implements StartFragment.Host {

    private static final String TAG = "LoginActivity";

    private static final boolean NEEDS_PERMISSIONS = false;

    public static final int GOOGLE_API_CLIENTID = 1;


    @Inject
    FusionClientProvider fusionClientProvider;

    @Inject
    SharedPrefStore sharedPrefStore;

    @Inject
    ClientInterfaces.AccountClient accountClient;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OptionFusionApplication.from(this).getComponent().inject(this);

        final Fragment frag = StartFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, frag, "tag_start")
                .addToBackStack(null)
                .commit();

        googleApiClient = FusionClient.getGoogleApiClient(this, GOOGLE_API_CLIENTID);

        new AsyncTask<Void, Void, GoogleSignInResult>() {
            @Override
            protected GoogleSignInResult doInBackground(Void... params) {
                return accountClient.trySilentSignIn(googleApiClient);
            }

            @Override
            protected void onPostExecute(GoogleSignInResult googleSignInResult) {
                if (googleSignInResult != null && googleSignInResult.isSuccess()) {
                    startLogin(OptionFusionApplication.Provider.OPTION_FUSION_BACKEND);
                } else {
                    ((StartFragment)frag).showSignInButton();
                }
            }
        }.execute(null, null);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (NEEDS_PERMISSIONS)
            checkPermissions();
    }

    protected void checkPermissions() {
        int permissionCheck = ContextCompat.checkSelfPermission(this,
                Manifest.permission.GET_ACCOUNTS);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {

            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.GET_ACCOUNTS)) {

                // Show an expanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                Toast.makeText(this, "It gives us permissions!", Toast.LENGTH_SHORT);

            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // granted
                } else {
                    // denied
                }
                return;
            }
        }
    }

    @Override
    public void startLogin(OptionFusionApplication.Provider provider) {
        OptionFusionApplication.from(this).setProvider(provider);

        Fragment frag;

        switch (provider) {
            case AMERITRADE:
                frag = AmeritradeLoginFragment.newInstance();
                break;
            default:
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, frag, "tag_login")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    @Override
    public void onBackPressed() {
        if (getSupportFragmentManager().getBackStackEntryCount() > 1) {
            getSupportFragmentManager().popBackStack();
        } else {
            finish();
        }
    }


}

