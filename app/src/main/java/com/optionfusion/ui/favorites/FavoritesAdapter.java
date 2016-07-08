package com.optionfusion.ui.favorites;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.optionfusion.R;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.events.FavoritesUpdatedEvent;
import com.optionfusion.model.DbSpread;
import com.optionfusion.model.provider.VerticalSpread;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.SharedViewHolders;
import com.optionfusion.ui.results.ResultsAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.FavoriteViewHolder> implements SharedViewHolders.SpreadFavoriteListener {

    @Inject
    DbHelper dbHelper;

    @Inject
    EventBus bus;

    @Inject
    Context context;

    Handler handler = new Handler();
    private final FavoritesFragment fragment;
    List<FavoriteSpread> favoriteSpreads = new ArrayList<>();

    public FavoritesAdapter(FavoritesFragment favoritesFragment) {
        OptionFusionApplication.from(favoritesFragment.getActivity()).getComponent().inject(this);
        populate();
        bus.register(this);
        fragment = favoritesFragment;
    }

    @Override
    public FavoriteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spread_details, parent, false);
        return new FavoriteViewHolder(itemView, context, this, (ResultsAdapter.SpreadSelectedListener) fragment.getActivity());
    }

    @Override
    public void onBindViewHolder(FavoriteViewHolder holder, int position) {
        holder.bind(favoriteSpreads.get(position));
    }

    @Override
    public int getItemCount() {
        return favoriteSpreads.size();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(FavoritesUpdatedEvent event) {
        populate();
    }

    public void populate() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        List<FavoriteSpread> newSpreads = new ArrayList<>();

        Cursor c = null;
        try {
            boolean needsUpdate = false;
            c = db.query(Schema.vw_Favorites.VIEW_NAME, Schema.getProjection(Schema.vw_Favorites.values()), null, null, null, null, null);
            while (c != null && c.moveToNext()) {
                newSpreads.add(new FavoriteSpread(c));
                int pos = newSpreads.size() - 1;
                if (favoriteSpreads.size() <= pos || !newSpreads.get(pos).equals(favoriteSpreads.get(pos)))
                    needsUpdate = true;
            }

            favoriteSpreads = newSpreads;

            if (needsUpdate)
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        notifyDataSetChanged();
                    }
                });
        } finally {
            if (c != null)
                c.close();
        }
    }

    @Override
    public void setFavorite(VerticalSpread spread, boolean isFavorite) {
        if (isFavorite)
            ((DbSpread) spread).saveAsFavorite(dbHelper.getWritableDatabase(), bus);
        else
            ((DbSpread) spread).unFavorite(dbHelper.getWritableDatabase(), bus);
    }

    public class FavoriteSpread extends DbSpread {
        private static final String TAG = "FavoriteSpread";

        public FavoriteSpread(Cursor cursor) {
            super(cursor);

            for (int i = 0; i < cursor.getColumnCount(); i++) {
                String colName = cursor.getColumnName(i);
                Schema.Favorites col = null;
                try {
                    col = Schema.Favorites.valueOf(colName);
                } catch (Exception e) {
                }

                if (col == null) {
                    continue;
                }

                putValue(col, cursor, i);
            }
        }
    }

    public static class FavoriteViewHolder extends RecyclerView.ViewHolder {

        private final Context context;
        private final SharedViewHolders.SpreadFavoriteListener spreadFavoriteListener;
        private final ResultsAdapter.SpreadSelectedListener spreadSelectedListener;
        @Bind(R.id.details_brief)
        View briefDetailsLayout;

        @Bind(R.id.header)
        View spreadHeaderLayout;

        private final SharedViewHolders.TradeDetailsHeaderHolder tradeHeaderHolder;
        private VerticalSpread spread;
        private final SharedViewHolders.BriefTradeDetailsHolder briefTradeDetailsHolder;

        public FavoriteViewHolder(View itemView, Context context, SharedViewHolders.SpreadFavoriteListener spreadFavoriteListener, ResultsAdapter.SpreadSelectedListener spreadSelectedListener) {
            super(itemView);
            this.context = context;
            this.spreadFavoriteListener = spreadFavoriteListener;
            this.spreadSelectedListener = spreadSelectedListener;
            ButterKnife.bind(this, itemView);
            briefTradeDetailsHolder = new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout);
            tradeHeaderHolder = new SharedViewHolders.TradeDetailsHeaderHolder(spreadHeaderLayout, spreadFavoriteListener);
        }

        public void bind(FavoriteSpread item) {
            this.spread = item;
            briefTradeDetailsHolder.bind(spread);
            tradeHeaderHolder.bind(spread);
        }

        @OnClick(R.id.btn_more)
        @Nullable
        public void onClickMore() {
            if (spreadSelectedListener != null)
                spreadSelectedListener.onResultSelected(spread, spreadHeaderLayout, briefDetailsLayout);
        }
    }

}
