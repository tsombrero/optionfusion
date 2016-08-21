package com.optionfusion.ui.favorites;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.optionfusion.R;
import com.optionfusion.module.OptionFusionApplication;

import butterknife.Bind;
import butterknife.ButterKnife;

public class FavoritesFragment extends Fragment {

    private FavoritesAdapter adapter;

    public static FavoritesFragment newInstance() {
        return new FavoritesFragment();
    }

    @Bind(R.id.list)
    RecyclerView recyclerView;

    @Bind(R.id.emptylayout)
    View emptyLayout;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_favorites, container, false);
        ButterKnife.bind(this, ret);

        setHasOptionsMenu(true);

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        adapter = new FavoritesAdapter(this);
        recyclerView.setAdapter(adapter);

        return ret;
    }

    public void showEmpty(boolean showEmpty) {
        emptyLayout.setVisibility(showEmpty ? View.VISIBLE : View.GONE);
    }
}