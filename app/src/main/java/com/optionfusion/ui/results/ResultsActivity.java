package com.optionfusion.ui.results;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatActivity;

import com.optionfusion.R;

public class ResultsActivity extends AppCompatActivity {

    public static final String EXTRA_SYMBOL = "EXTRA_SYMBOL";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        String symbol = getSymbol();

        if (symbol == null)
            finish();

        Fragment fragment = ResultsFragment.newInstance(symbol);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment, symbol)
                .addToBackStack(null)
                .commitAllowingStateLoss();
    }

    public String getSymbol() {
        if (getIntent().hasExtra(EXTRA_SYMBOL))
            return getIntent().getExtras().getString(EXTRA_SYMBOL);

        return null;
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}
