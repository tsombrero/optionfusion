package com.optionfusion.jobqueue;

import android.content.Context;
import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.client.FusionClient;
import com.optionfusion.com.backend.optionFusion.model.Position;
import com.optionfusion.model.DbSpread;
import com.optionfusion.module.OptionFusionApplication;

public class SetFavoriteJob extends BaseApiJob {

    private static final String TAG = "SetFavoriteJob";
    private final DbSpread spread;
    private final boolean isFavorite;

    public SetFavoriteJob(Context context, DbSpread spread, boolean isFavorite) {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST));

        this.spread = spread;
        this.isFavorite = isFavorite;
        OptionFusionApplication.from(context).getComponent().inject(this);

        Log.i(TAG, "New SetFavoriteJob : " + spread);
    }

    @Override
    public void onAdded() {
        super.onAdded();
        if (isFavorite) {
            spread.saveAsFavorite(dbHelper.getWritableDatabase(), bus);
        } else {
            spread.unFavorite(dbHelper.getWritableDatabase(), bus);
        }
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        Position pos = spread.getPosition();
        if (isFavorite) {
            ((FusionClient) accountClient).putFavorite(pos);
        } else {
            ((FusionClient) accountClient).removeFavorite(pos);
        }
    }
}
