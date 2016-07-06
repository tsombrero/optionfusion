package com.optionfusion.ui.tradedetails;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.optionfusion.R;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;

import butterknife.ButterKnife;

public class TradeDetailsActivity extends AppCompatActivity {

    VerticalSpread spread;

    public final static String EXTRA_SPREAD = "spread";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        OptionFusionApplication.from(this).getComponent().inject(this);

        spread = getIntent().getParcelableExtra(EXTRA_SPREAD);

        setContentView(R.layout.activity_results);
        ButterKnife.bind(this);

        Fragment fragment = TradeDetailsFragment.newInstance(spread);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, spread.getDescription())
                .addToBackStack(null)
                .commitAllowingStateLoss();

    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
