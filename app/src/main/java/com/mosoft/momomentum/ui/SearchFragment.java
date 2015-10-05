package com.mosoft.momomentum.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.cache.OptionChainProvider;
import com.mosoft.momomentum.model.amtd.OptionChain;
import com.mosoft.momomentum.module.MomentumApplication;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SearchFragment extends Fragment {

    @Bind(R.id.edit_symbol)
    protected EditText editSymbolView;

    @Bind(R.id.submit)
    protected Button submitButton;

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

    @OnClick(R.id.submit)
    public void onClick(View view) {
        final String symbol = editSymbolView.getText().toString();

        if (TextUtils.isEmpty(symbol)) {
            Toast.makeText(getActivity(), "Enter a ticker symbolView", Toast.LENGTH_SHORT);
            return;
        }

        submitButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);

        optionChainProvider.get(symbol, new OptionChainProvider.OptionChainCallback() {
            @Override
            public void call(OptionChain optionChain) {
                if (optionChain != null)
                    ((FragmentHost) getActivity()).openSearchFragment(optionChain);

                submitButton.setEnabled(true);
                progress.setVisibility(View.GONE);
            }
        });
    }

    public static Fragment newInstance() {
        return new SearchFragment();
    }

    public interface FragmentHost {
         void openSearchFragment(OptionChain optionChain);
    }
}
