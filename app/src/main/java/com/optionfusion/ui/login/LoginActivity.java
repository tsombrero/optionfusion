package com.optionfusion.ui.login;

import android.Manifest;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import com.optionfusion.R;
import com.optionfusion.client.FusionClientProvider;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.MainActivity;

import javax.inject.Inject;

public class LoginActivity extends FragmentActivity implements StartFragment.Host {

    private static final String TAG = "LoginActivity";

    private static final boolean NEEDS_PERMISSIONS = false;

    @Inject
    FusionClientProvider fusionClientProvider;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        OptionFusionApplication.from(this).getComponent().inject(this);

        fusionClientProvider.initGoogleApiClient();

        Fragment frag = StartFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, frag, "tag_start")
                .addToBackStack(null)
                .commit();
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
                Toast.makeText(this, "Gives it zee permissions!", Toast.LENGTH_SHORT);

            }

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.GET_ACCOUNTS},
                    0);

            AccountManager accountManager = AccountManager.get(this);
            for (Account account : accountManager.getAccounts()) {
                Log.d(TAG, account.toString());
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
                    AccountManager accountManager = AccountManager.get(this);
                    for (Account account : accountManager.getAccounts()) {
                        Log.d(TAG, account.toString());
                    }
                } else {
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
                if (getSupportFragmentManager().getBackStackEntryCount() > 1)
                    getSupportFragmentManager().popBackStack();
                return;
        }

        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, frag, "tag_login")
                .addToBackStack(null)
                .commit();
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

