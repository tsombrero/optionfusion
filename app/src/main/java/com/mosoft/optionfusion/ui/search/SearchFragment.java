package com.mosoft.optionfusion.ui.search;

import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.widgets.SymbolSearchView;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SearchFragment extends Fragment implements SymbolSearchView.SearchSubmitListener {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    SymbolSearchView searchView;

    @Inject
    OptionChainProvider optionChainProvider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        ((AppCompatActivity)getActivity()).setSupportActionBar(toolbar);

        setHasOptionsMenu(true);
        return ret;
    }

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onSearchSubmitted(String symbol) {
        ((Host) getActivity()).openResultsFragment(symbol);
    }

    public interface Host {
        void openResultsFragment(String symbol);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {

        inflater.inflate(R.menu.menu_search_fragment, menu);

        //Search stuff
        MenuItem searchItem = menu.findItem(R.id.action_search);
        searchView = (SymbolSearchView) searchItem.getActionView();
        searchView.setSubmitListener(this);
        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_search) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}

