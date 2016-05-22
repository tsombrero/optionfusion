package com.optionfusion.jobqueue;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.model.DbSpread;

import javax.inject.Inject;

public class SetFavoriteJob extends BaseApiJob {

    private static final String TAG = "SetFavoriteJob";
    private final DbSpread spread;
    private final boolean isFavorite;

    @Inject
    SQLiteDatabase db;

    public SetFavoriteJob(DbSpread spread, boolean isFavorite) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST));

        this.spread = spread;
        this.isFavorite = isFavorite;

        Log.i(TAG, "New SetFavoriteJob : " + spread);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        spread.saveAsFavorite(db);
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
    }
}
