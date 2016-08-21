package com.optionfusion.ui.favorites;

import android.content.Context;
import android.database.Cursor;
import android.os.Handler;
import android.support.annotation.MainThread;
import android.support.annotation.Nullable;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.birbit.android.jobqueue.JobManager;
import com.optionfusion.R;
import com.optionfusion.common.TextUtils;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.events.FavoritesUpdatedEvent;
import com.optionfusion.jobqueue.SetFavoriteJob;
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
import java.util.concurrent.RunnableFuture;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.optionfusion.db.Schema.*;

public class FavoritesAdapter extends RecyclerView.Adapter<FavoritesAdapter.BaseViewHolder> implements SharedViewHolders.SpreadFavoriteListener {

    @Inject
    DbHelper dbHelper;

    @Inject
    EventBus bus;

    @Inject
    Context context;

    @Inject
    JobManager jobManager;

    Handler handler = new Handler();
    private final FavoritesFragment fragment;
    List<FavoriteSpread> favoriteSpreads = new ArrayList<>();

    FavoritesAdapter(FavoritesFragment favoritesFragment) {
        OptionFusionApplication.from(favoritesFragment.getActivity()).getComponent().inject(this);
        bus.register(this);
        fragment = favoritesFragment;
        onEvent(null);
    }

    @Override
    public BaseViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == 0) {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_spread_details, parent, false);
            return new FavoriteViewHolder(itemView, context, this, (ResultsAdapter.SpreadSelectedListener) fragment.getActivity());
        } else {
            View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_undo, parent, false);
            return new UndoViewHolder(itemView, context, jobManager);
        }
    }

    @Override
    public int getItemViewType(int position) {
        return favoriteSpreads.get(position).isDeleted() ? 1 : 0;
    }

    @Override
    public void onBindViewHolder(BaseViewHolder holder, int position) {
        holder.bind(favoriteSpreads.get(position));
    }

    @Override
    public int getItemCount() {
        return favoriteSpreads.size();
    }

    @Subscribe(threadMode = ThreadMode.ASYNC)
    public void onEvent(FavoritesUpdatedEvent event) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        final List<FavoriteSpread> newSpreads = new ArrayList<>();
        Cursor c = null;
        try {
            c = db.query(vw_Favorites.VIEW_NAME, getProjection(vw_Favorites.values()), null, null, null, null,
                    vw_Favorites.UNDERLYING_SYMBOL + ", " + vw_Favorites.TIMESTAMP_EXPIRATION + ", " + vw_Favorites.BUY_STRIKE + " ASC");
            while (c != null && c.moveToNext()) {
                newSpreads.add(new FavoriteSpread(c));
            }
        } finally {
            if (c != null)
                c.close();
        }

        handler.post(new Runnable() {

            @Override
            public void run() {
                populate(newSpreads);
            }
        });
    }

    @MainThread
    private void populate(final List<FavoriteSpread> newSpreads) {

        final List<FavoriteSpread> oldSpreads = favoriteSpreads;

        favoriteSpreads = newSpreads;
        fragment.showEmpty(favoriteSpreads.isEmpty());

        if (oldSpreads != null) {
            final DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldSpreads.size();
                }

                @Override
                public int getNewListSize() {
                    return newSpreads.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    FavoriteSpread oldItem = oldSpreads.get(oldItemPosition);
                    FavoriteSpread newItem = newSpreads.get(newItemPosition);
                    return TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                            && TextUtils.equals(oldItem.getUnderlyingSymbol(), newItem.getUnderlyingSymbol());
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    FavoriteSpread oldItem = oldSpreads.get(oldItemPosition);
                    FavoriteSpread newItem = newSpreads.get(newItemPosition);

                    return TextUtils.equals(oldItem.getDescription(), newItem.getDescription())
                            && TextUtils.equals(oldItem.getUnderlyingSymbol(), newItem.getUnderlyingSymbol())
                            && oldItem.isDeleted() == newItem.isDeleted();
                }
            }, true);

            diffResult.dispatchUpdatesTo(FavoritesAdapter.this);
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
                Favorites col = null;
                try {
                    col = Favorites.valueOf(colName);
                } catch (Exception e) {
                }

                if (col == null) {
                    continue;
                }

                putValue(col, cursor, i);
            }
        }

        public boolean isDeleted() {
            return getBoolean(Favorites.IS_DELETED);
        }
    }

    static abstract class BaseViewHolder extends RecyclerView.ViewHolder {

        BaseViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        abstract void bind(FavoriteSpread item);
    }

    public static class UndoViewHolder extends BaseViewHolder {
        private FavoriteSpread item;
        private final Context context;
        private final JobManager jobManager;

        UndoViewHolder(View itemView, Context context, JobManager jobManager) {
            super(itemView);
            this.context = context;
            this.jobManager = jobManager;
        }

        void bind(FavoriteSpread item) {
            this.item = item;
        }

        @OnClick(R.id.close)
        public void onClose() {
            jobManager.addJobInBackground(new SetFavoriteJob(context, item, false));
        }

        @OnClick(R.id.undoButton)
        public void onUndo() {
            jobManager.addJobInBackground(new SetFavoriteJob(context, item, true));
        }
    }

    public static class FavoriteViewHolder extends BaseViewHolder {

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

        FavoriteViewHolder(View itemView, Context context, SharedViewHolders.SpreadFavoriteListener spreadFavoriteListener, ResultsAdapter.SpreadSelectedListener spreadSelectedListener) {
            super(itemView);
            this.context = context;
            this.spreadFavoriteListener = spreadFavoriteListener;
            this.spreadSelectedListener = spreadSelectedListener;
            briefTradeDetailsHolder = new SharedViewHolders.BriefTradeDetailsHolder(briefDetailsLayout);
            tradeHeaderHolder = new SharedViewHolders.TradeDetailsHeaderHolder(spreadHeaderLayout, spreadFavoriteListener);
        }

        void bind(FavoriteSpread item) {
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
