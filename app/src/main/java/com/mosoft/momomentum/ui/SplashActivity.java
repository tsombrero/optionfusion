package com.mosoft.momomentum.ui;

import android.app.Activity;
import android.os.Bundle;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.util.Util;

public class SplashActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Util.goFullscreen(this);
    }


}
