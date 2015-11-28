package com.mosoft.optionfusion.ui.login;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.MainActivity;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends Activity implements StartFragment.Host {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Fragment frag = StartFragment.newInstance();
        getFragmentManager().beginTransaction()
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
                frag = LoginFragment.newInstance();
                break;
            default:
                startActivity(new Intent(this, MainActivity.class));
                if (getFragmentManager().getBackStackEntryCount() > 1)
                    getFragmentManager().popBackStack();
                return;
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, frag, "tag_login")
                .addToBackStack(null)
                .commit();
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 1) {
            getFragmentManager().popBackStack();
        } else {
            finish();
        }
    }
}

