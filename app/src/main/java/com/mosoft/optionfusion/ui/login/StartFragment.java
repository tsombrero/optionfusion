package com.mosoft.optionfusion.ui.login;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.module.OptionFusionApplication;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mosoft.optionfusion.module.OptionFusionApplication.Provider.*;

public class StartFragment extends Fragment {

    public static Fragment newInstance() {
        return new StartFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.splash, container, false);
        ButterKnife.bind(this, ret);
        return ret;
    }


    @OnClick({R.id.no_thanks, R.id.logo_ameritrade})
    public void onClickAmeritrade(View view) {
        switch (view.getId()) {
            case R.id.logo_ameritrade:
                getFragmentHost().startLogin(AMERITRADE);
                break;
            case R.id.no_thanks:
            default:
                getFragmentHost().startLogin(GOOGLE_FINANCE);
        }
    }

    private Host getFragmentHost() {
        return (Host)getActivity();
    }


    public interface Host {
        void startLogin(OptionFusionApplication.Provider provider);
    }
}
