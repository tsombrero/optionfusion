package com.optionfusion.ui.login;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.Auth;
import com.optionfusion.R;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.MainActivity;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends FragmentActivity implements StartFragment.Host {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Fragment frag = StartFragment.newInstance();
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, frag, "tag_start")
                .addToBackStack(null)
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
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

