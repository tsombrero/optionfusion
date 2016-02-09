package com.optionfusion.ui.widgets;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
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
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;

import com.optionfusion.R;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.module.OptionFusionApplication;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;


public class SymbolSearchTextView extends AutoCompleteTextView {

    @Inject
    ClientInterfaces.SymbolLookupClient symbolQueryClient;

    private SuggestionAdapter suggestionAdapter;

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

    private final static String ALLOWED_NON_ALPHANUMERIC_CHARS = "&.- ";

    private void init() {
        if (!isInEditMode())
            OptionFusionApplication.from(getContext()).getComponent().inject(this);

        setHint(getContext().getString(R.string.stock_search_hint));
        setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        suggestionAdapter = new SuggestionAdapter(getContext());
        setAdapter(suggestionAdapter);
        InputFilter filter = new InputFilter() {
            public CharSequence filter(CharSequence source, int start, int end,
                                       Spanned dest, int dstart, int dend) {
                for (int i = start; i < end; i++) {
                    if (!Character.isLetterOrDigit(source.charAt(i))
                            && ALLOWED_NON_ALPHANUMERIC_CHARS.indexOf(source.charAt(i)) == -1) {
                        return "";
                    }
                }
                return null;
            }
        };
        setFilters(new InputFilter[]{filter});
        setThreshold(1);

        setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (symbolSearchListener == null)
                    return;

                synchronized (suggestionAdapter) {
                    String ticker = suggestionAdapter.getItem(position).getTicker();
                    symbolSearchListener.onSymbolSearch(ticker);
                }
            }
        });
    }

    protected static class SuggestionAdapter extends ArrayAdapter<ClientInterfaces.SymbolLookupResult> {

        private List<ClientInterfaces.SymbolLookupResult> list = null;

        public SuggestionAdapter(Context context) {
            super(context, 0);
        }

        @Override
        public ClientInterfaces.SymbolLookupResult getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getCount() {
            return list.size();
        }

        public void setList(List<ClientInterfaces.SymbolLookupResult> list) {
            this.list = list;
            notifyDataSetChanged();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = newView(parent);
            }

            SuggestionItemViewHolder holder = (SuggestionItemViewHolder) convertView.getTag();
            holder.description.setText(list.get(position).getDescription());
            holder.ticker.setText(list.get(position).getTicker());
            return convertView;
        }

        public View newView(ViewGroup parent) {
            View ret = View.inflate(parent.getContext(), R.layout.item_lookup_suggestion, null);
            ret.setTag(new SuggestionItemViewHolder(ret));
            return ret;
        }
    }

    protected static class SuggestionItemViewHolder {
        @Bind(R.id.ticker)
        TextView ticker;

        @Bind(R.id.description)
        TextView description;

        public SuggestionItemViewHolder(View view) {
            ButterKnife.bind(this, view);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        if (cursorCache != null)
            cursorCache.evictAll();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onTextChanged(CharSequence text, int start, int lengthBefore, int lengthAfter) {
        if (cursorCache == null)
            return;

        new AsyncTask<String, Void, List<ClientInterfaces.SymbolLookupResult>>() {

            public String query;

            @Override
            protected List<ClientInterfaces.SymbolLookupResult> doInBackground(String... params) {
                query = params[0];
                List<ClientInterfaces.SymbolLookupResult> ret = cursorCache.get(query);
                return ret;
            }

            @Override
            protected void onPostExecute(List<ClientInterfaces.SymbolLookupResult> results) {
                if (TextUtils.equals(query, getText())) {
                    suggestionAdapter.setList(results);
                }
            }
        }.execute(text.toString());
    }

    public void setSymbolSearchListener(SymbolSearchListener symbolSearchListener) {
        this.symbolSearchListener = symbolSearchListener;
    }

    public interface SymbolSearchListener {
        void onSymbolSearch(String symbol);
    }

    LruCache<String, List<ClientInterfaces.SymbolLookupResult>> cursorCache = new LruCache<String, List<ClientInterfaces.SymbolLookupResult>>(20) {
        List<String> noResultKeys = new ArrayList<>();

        @Override
        protected List<ClientInterfaces.SymbolLookupResult> create(String key) {

            if (TextUtils.isEmpty(key) || key.length() == 0)
                return null;

            for (String noResultKey : noResultKeys) {
                if (key.startsWith(noResultKey))
                    return null;
            }

            try {
                List<ClientInterfaces.SymbolLookupResult> ret = symbolQueryClient.getSymbolsMatching(key);

                if (ret == null || ret.size() == 0)
                    noResultKeys.add(key);

                return ret;
            } catch (Exception e) {
                Log.i(TAG, "create: Failed", e);
            }
            return null;
        }
    };

    private String cursorStringify(Cursor cursor) {
        StringBuilder sb =  new StringBuilder("{");
        if (cursor != null)
            cursor.moveToPosition(-1);

        while (cursor != null && cursor.moveToNext()) {
            sb.append(cursor.getString(1)).append(", ");
        }
        sb.append("}");
        if (cursor != null)
            cursor.moveToPosition(-1);
        return sb.toString();
    }
}
