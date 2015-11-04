package com.mosoft.momomentum.ui.login;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.module.MomentumApplication;

import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.mosoft.momomentum.module.MomentumApplication.Provider.*;

public class StartFragment extends Fragment {

    public static Fragment newInstance() {
        return new StartFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MomentumApplication.from(getActivity()).getComponent().inject(this);
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
        void startLogin(MomentumApplication.Provider provider);
    }
}
