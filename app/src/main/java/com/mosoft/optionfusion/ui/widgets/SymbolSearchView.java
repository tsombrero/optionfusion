package com.mosoft.optionfusion.ui.widgets;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.widget.SearchView;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.client.ClientInterfaces.SymbolLookupClient.SuggestionColumns;
import com.mosoft.optionfusion.module.OptionFusionApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;


public class SymbolSearchView extends SearchView implements SearchView.OnQueryTextListener, SearchView.OnSuggestionListener {

    @Inject
    ClientInterfaces.SymbolLookupClient symbolQueryClient;

    private SuggestionCursorAdapter suggestionAdapter;

    private SearchSubmitListener submitListener;

    private static final String TAG = "SymbolSearchView";

    public SymbolSearchView(Context context) {
        super(context);
        init();
    }

    public SymbolSearchView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SymbolSearchView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        OptionFusionApplication.from(getContext()).getComponent().inject(this);

        setOnSuggestionListener(this);
        setOnQueryTextListener(this);
        setQueryHint(getContext().getString(R.string.symbol));
        setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        suggestionAdapter = new SuggestionCursorAdapter(getContext());
        setSuggestionsAdapter(suggestionAdapter);
        applyToTextViews(new ButterKnife.Action() {
            @Override
            public void apply(View view, int index) {
                TextView textView = (TextView) view;
                textView.setTextColor(getContext().getResources().getColor(R.color.primary_text_inverse));
                textView.setHintTextColor(getContext().getResources().getColor(R.color.text_hint_inverse));

                InputFilter filter = new InputFilter() {
                    public CharSequence filter(CharSequence source, int start, int end,
                                               Spanned dest, int dstart, int dend) {
                        for (int i = start; i < end; i++) {
                            if (!Character.isLetterOrDigit(source.charAt(i))
                                    && ".-".indexOf(source.charAt(i)) == -1) {
                                return "";
                            }
                        }
                        return null;
                    }
                };
                textView.setFilters(new InputFilter[]{filter});
            }
        });
    }

    @Override
    public boolean onSuggestionSelect(int position) {
        if (submitListener == null)
            return false;

        String symbol = null;

        synchronized (suggestionAdapter) {
            suggestionAdapter.getCursor().moveToPosition(position);
            symbol = suggestionAdapter.getCursor().getString(SuggestionColumns.symbol.ordinal());
        }
        submitListener.onSearchSubmitted(symbol);
        return true;
    }

    @Override
    public boolean onSuggestionClick(int position) {
        return onSuggestionSelect(position);
    }

    protected static class SuggestionCursorAdapter extends CursorAdapter {
        private SuggestionCursorAdapter(Context context) {
            super(context, ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR, false);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            View ret = View.inflate(parent.getContext(), R.layout.item_lookup_suggestion, null);
            ret.setTag(new SuggestionItemViewHolder(ret));
            return ret;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            SuggestionItemViewHolder holder = (SuggestionItemViewHolder) view.getTag();
            holder.description.setText(cursor.getString(SuggestionColumns.description.ordinal()));
            holder.symbol.setText(cursor.getString(SuggestionColumns.symbol.ordinal()));
        }
    }

    protected static class SuggestionItemViewHolder {
        @Bind(R.id.symbol)
        TextView symbol;

        @Bind(R.id.description)
        TextView description;

        public SuggestionItemViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        if (submitListener != null)
            submitListener.onSearchSubmitted(query);

        return submitListener != null;
    }

    @Override
    public boolean onQueryTextChange(String newText) {

        if (TextUtils.isEmpty(newText)) {
            SymbolSearchView.this.postDelayed(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (TextUtils.isEmpty(getQuery())) {
                            setIconified(true);
                        }
                    } catch (Exception e) {
                    }
                }
            }, 1700);

            return true;
        }

        new AsyncTask<String, Void, Cursor>() {

            public String query;

            @Override
            protected Cursor doInBackground(String... params) {
                query = params[0];
                return cursorCache.get(query);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (TextUtils.equals(query, getQuery()))
                    suggestionAdapter.changeCursor(cursor);
            }
        }.execute(newText);
        return true;
    }

    public void setSubmitListener(SearchSubmitListener submitListener) {
        this.submitListener = submitListener;
    }

    public interface SearchSubmitListener {
        void onSearchSubmitted(String symbol);
    }

    LruCache<String, Cursor> cursorCache = new LruCache<String, Cursor>(20) {
        List<String> noResultKeys = new ArrayList<>();

        @Override
        protected Cursor create(String key) {

            if (TextUtils.isEmpty(key) || key.length() < 2)
                return null;

            for (String noResultKey : noResultKeys) {
                if (key.startsWith(noResultKey))
                    return null;
            }

            try {
                Cursor ret = symbolQueryClient.getSymbolsMatching(key);

                if (ret == null || ret.getCount() == 0)
                    noResultKeys.add(key);

                return ret;
            } catch (Exception e) {
                Log.i(TAG, "create: Failed", e);
            }
            return null;
        }

        @Override
        protected void entryRemoved(boolean evicted, String key, Cursor oldValue, Cursor newValue) {
            try {
                if (oldValue != null)
                    oldValue.close();
            } catch (Exception e) {
            }
        }
    };

    //Utility to scrounge up the SearchView's textiew
    private void applyToTextViews(ButterKnife.Action action) {
        Collection<TextView> views = findChildrenByClass(this, TextView.class);
        for (TextView view : views) {
            action.apply(view, 0);
        }
    }

    private static <V extends View> Collection<V> findChildrenByClass(ViewGroup viewGroup, Class<V> clazz) {

        return gatherChildrenByClass(viewGroup, clazz, new ArrayList<V>());
    }

    private static <V extends View> Collection<V> gatherChildrenByClass(ViewGroup viewGroup, Class<V> clazz, Collection<V> childrenFound) {

        for (int i = 0; i < viewGroup.getChildCount(); i++) {
            final View child = viewGroup.getChildAt(i);
            if (clazz.isAssignableFrom(child.getClass())) {
                childrenFound.add((V) child);
            }
            if (child instanceof ViewGroup) {
                gatherChildrenByClass((ViewGroup) child, clazz, childrenFound);
            }
        }

        return childrenFound;
    }
}
