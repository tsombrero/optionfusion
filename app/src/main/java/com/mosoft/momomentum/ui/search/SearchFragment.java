package com.mosoft.momomentum.ui.search;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.Toast;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.provider.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnEditorAction;

public class SearchFragment extends Fragment {

    @Bind(R.id.edit_symbol)
    protected EditText editSymbolView;

    @Bind(R.id.progress)
    protected View progress;

    @Inject
    OptionChainProvider optionChainProvider;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        MomentumApplication.from(getActivity()).getComponent().inject(this);
        View ret = inflater.inflate(R.layout.fragment_search, container, false);
        ButterKnife.bind(this, ret);

        return ret;
    }

    @OnEditorAction(R.id.edit_symbol)
    public boolean onEditorAction(int action) {
        switch (action) {
            case EditorInfo.IME_ACTION_DONE:
            case EditorInfo.IME_ACTION_GO:
                break;
            default:
                return false;
        }

        final String symbol = editSymbolView.getText().toString();

        if (TextUtils.isEmpty(symbol)) {
            Toast.makeText(getActivity(), "Enter a ticker symbolView", Toast.LENGTH_SHORT);
            return true;
        }

        progress.setVisibility(View.VISIBLE);

        optionChainProvider.get(symbol, new OptionChainProvider.OptionChainCallback() {
            @Override
            public void call(OptionChain optionChain) {
                if (optionChain != null)
                    ((Host) getActivity()).openResultsFragment(optionChain);

                progress.setVisibility(View.GONE);
            }
        });
        return true;
    }

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    public interface Host {
        void openResultsFragment(OptionChain optionChain);
    }
}
