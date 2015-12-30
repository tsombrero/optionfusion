package com.mosoft.optionfusion.ui.widgets;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.widget.CursorAdapter;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LruCache;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.mosoft.optionfusion.R;
import com.mosoft.optionfusion.client.ClientInterfaces;
import com.mosoft.optionfusion.client.ClientInterfaces.SymbolLookupClient.SuggestionColumns;
import com.mosoft.optionfusion.module.OptionFusionApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;


public class SymbolSearchTextView extends AutoCompleteTextView {

    @Inject
    ClientInterfaces.SymbolLookupClient symbolQueryClient;

    private SuggestionCursorAdapter suggestionAdapter;

    private SymbolSearchListener symbolSearchListener;

    private static final String TAG = "SymbolSearchView";

    public SymbolSearchTextView(Context context) {
        super(context);
        init();
    }

    public SymbolSearchTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SymbolSearchTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        OptionFusionApplication.from(getContext()).getComponent().inject(this);

        setHint(getContext().getString(R.string.stock_search_hint));
        setInputType(InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        suggestionAdapter = new SuggestionCursorAdapter(getContext());
        setAdapter(suggestionAdapter);
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
        setFilters(new InputFilter[]{filter});
        setThreshold(2);

        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (symbolSearchListener == null)
                    return;

                synchronized (suggestionAdapter) {
                    suggestionAdapter.getCursor().moveToPosition(position);
                    String symbol = suggestionAdapter.getCursor().getString(SuggestionColumns.symbol.ordinal());
                    symbolSearchListener.onSymbolSearch(symbol);
                }
            }
        });
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
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (cursorCache == null)
            return;

        new AsyncTask<String, Void, Cursor>() {

            public String query;

            @Override
            protected Cursor doInBackground(String... params) {
                query = params[0];
                return cursorCache.get(query);
            }

            @Override
            protected void onPostExecute(Cursor cursor) {
                if (TextUtils.equals(query, getText()))
                    suggestionAdapter.changeCursor(cursor);
            }
        }.execute(text.toString());
    }

    public void setSymbolSearchListener(SymbolSearchListener symbolSearchListener) {
        this.symbolSearchListener = symbolSearchListener;
    }

    public interface SymbolSearchListener {
        void onSymbolSearch(String symbol);
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
}
