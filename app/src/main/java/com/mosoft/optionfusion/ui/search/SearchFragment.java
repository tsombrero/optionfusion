package com.mosoft.optionfusion.ui.search;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.cache.OptionChainProvider;
import com.mosoft.optionfusion.module.OptionFusionApplication;
import com.mosoft.optionfusion.ui.widgets.SymbolSearchView;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;

public class SearchFragment extends Fragment implements SymbolSearchView.SymbolLookupListener {

    @Bind(R.id.edit_symbol)
    EditText editSymbolView;

    SymbolSearchView searchView;

    @Inject
    OptionChainProvider optionChainProvider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        setHasOptionsMenu(true);
        return ret;
    }

    @OnEditorAction(R.id.edit_symbol)
    public boolean onEditorAction(int action) {
        switch (action) {
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_GO:
            case EditorInfo.IME_ACTION_SEARCH:
                break;
            default:
                return false;
        }

        final String symbol = editSymbolView.getText().toString();

        if (TextUtils.isEmpty(symbol)) {
            Toast.makeText(getActivity(), "Enter a ticker symbol", Toast.LENGTH_SHORT);
            return true;
        }

        ((Host) getActivity()).openResultsFragment(symbol);
        return true;
    }

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    @Override
    public void onSymbolClicked(String symbol) {
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
        searchView.setLookupListener(this);
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

