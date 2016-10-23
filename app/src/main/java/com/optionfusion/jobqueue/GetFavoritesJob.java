package com.optionfusion.jobqueue;

import android.util.Log;

import com.birbit.android.jobqueue.Params;
import com.optionfusion.com.backend.optionFusion.model.JsonMap;
import com.optionfusion.com.backend.optionFusion.model.Position;
import com.optionfusion.common.OptionKey;
import com.optionfusion.db.Schema;
import com.optionfusion.events.FavoritesUpdatedEvent;
import com.optionfusion.model.provider.VerticalSpread;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GetFavoritesJob extends BaseApiJob {

    private static final String TAG = "GetWatchlistJob";

    public GetFavoritesJob() {
        super(new Params(1)
                .requireNetwork()
                .setPersistent(false)
                .setGroupId(GROUP_ID_WATCHLIST));
    }

    @Override
    public void onRun() throws Throwable {
        super.onRun();
        List<Position> favorites = accountClient.getFavorites();
        Set<String> favoriteUnderlyingSymbols = new HashSet<>();

        if (favorites == null)
            throw new RuntimeException("Failed getting favorites");

        Log.d(TAG, "Got " + (favorites == null ? "0" : favorites.size()) + " favorites from FusionUser");

        for (Position pos : favorites) {
            favoriteUnderlyingSymbols.add(pos.getUnderlyingSymbol());
            JsonMap legs = pos.getLegs();

            Schema.ContentValueBuilder cvFavorite = new Schema.ContentValueBuilder();

            long expiration = 0;

            for (String leg : legs.keySet()) {

                Long qty = Long.valueOf((String)(legs.get(leg)));

                if (expiration == 0)
                    expiration = OptionKey.parse(leg).getExpiration();

                if (qty > 0) {
                    cvFavorite
                            .put(Schema.Favorites.BUY_QUANTITY, qty)
                            .put(Schema.Favorites.BUY_SYMBOL, leg);
                } else if (qty < 0) {
                    cvFavorite
                            .put(Schema.Favorites.SELL_QUANTITY, qty)
                            .put(Schema.Favorites.SELL_SYMBOL, leg);
                }
            }

            cvFavorite
                    .put(Schema.Favorites.CURRENT_ASK, pos.getAsk())
                    .put(Schema.Favorites.CURRENT_BID, pos.getBid())
                    .put(Schema.Favorites.PRICE_ACQUIRED, pos.getCost())
                    .put(Schema.Favorites.TIMESTAMP_ACQUIRED, pos.getAcquiredTimestamp())
                    .put(Schema.Favorites.TIMESTAMP_QUOTE, pos.getQuoteTimestamp())
                    .put(Schema.Favorites.IS_DELETED, (pos.getDeletedTimestamp() > 0) ? 1 : 0)
                    .put(Schema.Favorites.UNDERLYING_SYMBOL, pos.getUnderlyingSymbol())
                    .put(Schema.Favorites.TIMESTAMP_EXPIRATION, expiration)
            ;

            dbHelper.getWritableDatabase().insertWithOnConflict(Schema.Favorites.TABLE_NAME, "", cvFavorite.build(), SQLiteDatabase.CONFLICT_REPLACE);
        }

        for (String favoriteUnderlyingSymbol : favoriteUnderlyingSymbols) {
            optionChainClient.getOptionChain(favoriteUnderlyingSymbol, true);
        }

        bus.post(new FavoritesUpdatedEvent());
    }
}
