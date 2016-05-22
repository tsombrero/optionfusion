package com.optionfusion.ui.favorites;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.optionfusion.R;
import com.optionfusion.module.OptionFusionApplication;

import butterknife.ButterKnife;

public class FavoritesFragment extends Fragment {

    public static Fragment newInstance() {
        return new FavoritesFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, ret);
        return ret;
    }

}
