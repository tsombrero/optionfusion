package com.mosoft.optionfusion.ui.search;

import android.content.Context;
import android.preference.Preference;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.model.provider.Interfaces.StockQuote;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.results.ListViewHolders;
import com.mosoft.optionfusion.ui.widgets.SymbolSearchView;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;

public class SearchFragment extends Fragment implements SymbolSearchView.SearchSubmitListener {

    @Bind(R.id.toolbar)
    Toolbar toolbar;

    @Bind(R.id.list)
    RecyclerView recyclerView;

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

        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(new ArrayAdapter<StockQuote>(getContext()));

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

    private class StockQuoteAdapter extends RecyclerView.Adapter<ListViewHolders.BaseViewHolder> {

        private final Context context;

        public StockQuoteAdapter() {
            context = getActivity();
        }

        public void update() {
            getActivity().getPreferences()
        }

        @Override
        public ListViewHolders.BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        }

        @Override
        public void onBindViewHolder(ListViewHolders.BaseViewHolder holder, int position) {
        }

        @Override
        public int getItemCount() {
            return 0;
        }
    }
}

