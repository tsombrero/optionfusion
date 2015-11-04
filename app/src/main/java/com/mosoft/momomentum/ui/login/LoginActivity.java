package com.mosoft.momomentum.ui.login;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.ui.MainActivity;
import com.mosoft.momomentum.util.Util;

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
                .commit();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.goFullscreen(this);
    }

    @Override
    public void startLogin(MomentumApplication.Provider provider) {
        MomentumApplication.from(this).setProvider(provider);

        Fragment frag;

        switch (provider) {
            case AMERITRADE:
                frag = LoginFragment.newInstance();
                break;
            default:
                startActivity(new Intent(this, MainActivity.class));
                finish();
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

