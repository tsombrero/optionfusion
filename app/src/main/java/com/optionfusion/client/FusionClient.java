package com.optionfusion.client;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.optionfusion.BuildConfig;
import com.optionfusion.R;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.com.backend.optionFusion.OptionFusion;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.EquityCollection;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.com.backend.optionFusion.model.OptionChain;
import com.optionfusion.com.backend.optionFusion.model.Position;
import com.optionfusion.common.protobuf.OptionChainProto;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.Options;
import com.optionfusion.db.SpreadPopulator;
import com.optionfusion.events.LoggedOutExceptionEvent;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.backend.FusionOptionChain;
import com.optionfusion.model.provider.backend.FusionStockQuote;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.util.Constants;
import com.optionfusion.util.SharedPrefStore;
import com.optionfusion.util.Util;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;
import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import dagger.Lazy;

public class FusionClient implements ClientInterfaces.SymbolLookupClient, ClientInterfaces.OptionChainClient, ClientInterfaces.AccountClient, ClientInterfaces.StockQuoteClient {

    public static final String USERDATA_TRUE = "1";
    public static final String USERDATA_NONE = "";
    public static final String USERDATA_NOTIFY_UPGRADE = "USERDATA_NOTIFY_UPGRADE";

    OptionFusion optionFusionApi;

    FusionUser fusionUser;

    private GoogleSignInAccount account;

    Context context;

    private static final String ROOT_URL = BuildConfig.ROOT_URL;

    private static final String TAG = "FusionClient";

    @Inject
    DbHelper dbHelper;

    @Inject
    Lazy<StockQuoteProvider> stockQuoteProvider;

    @Inject
    SharedPrefStore sharedPrefStore;

    @Inject
    EventBus bus;

//    GoogleSignInResult signinResult;
    private List<Interfaces.StockQuote> watchlist;

    private Map<String, String> userData = new HashMap<>();

    public FusionClient(Context context) {
        OptionFusionApplication.from(context).getComponent().inject(this);
        this.context = context;
    }

    @Override
    public List<ClientInterfaces.SymbolLookupResult> getSymbolsMatching(String query) {
        try {
            EquityCollection matches = getEndpoints().optionDataApi().getTickersMatching(query).execute();

            if (matches == null)
                return Collections.EMPTY_LIST;

            List<ClientInterfaces.SymbolLookupResult> ret = new ArrayList<>();

            for (Equity match : matches.getItems()) {
                ret.add(new ClientInterfaces.SymbolLookupResult(match));
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return Collections.EMPTY_LIST;
    }

    @Override
    public Interfaces.OptionChain getOptionChain(final String symbol) {
        try {
            Log.d(TAG, "Getting chain for " + symbol);
            OptionChain chain = getEndpoints().optionDataApi().getEodChain(symbol).execute();
            if (chain == null)
                return null;

            Log.d(TAG, "Parsing chain for " + symbol);
            OptionChainProto.OptionChain protoChain = OptionChainProto.OptionChain.parseFrom(chain.decodeChainData());
            writeChainToDb(protoChain);

            Log.d(TAG, "Returning chain for " + symbol);
            return new FusionOptionChain(protoChain, dbHelper);
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (e.getStatusCode() == 401) {
                bus.post(new LoggedOutExceptionEvent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            Log.d(TAG, "Done building chain for " + symbol);
        }
        return null;
    }

    @Override
    public Interfaces.StockQuote getStockQuote(final String symbol, final ClientInterfaces.Callback<Interfaces.StockQuote> callback) {
        if (callback == null) {
            List<Interfaces.StockQuote> quote = getStockQuotes(Collections.singletonList(symbol));
            if (quote == null || quote.isEmpty())
                return null;
            return quote.get(0);
        }

        new AsyncTask<Void, Void, Interfaces.StockQuote>() {
            @Override
            protected Interfaces.StockQuote doInBackground(Void... params) {
                List<Interfaces.StockQuote> quote = getStockQuotes(Collections.singletonList(symbol));
                if (quote == null || quote.isEmpty())
                    return null;
                return quote.get(0);
            }

            @Override
            protected void onPostExecute(Interfaces.StockQuote stockQuote) {
                callback.call(stockQuote);
            }
        };

        return null;
    }

    @Override
    public List<Interfaces.StockQuote> getStockQuotes(final Collection<String> symbols, final ClientInterfaces.Callback<List<Interfaces.StockQuote>> callback) {
        if (callback == null) {
            return getStockQuotes(symbols);
        }

        new AsyncTask<Void, Void, List<Interfaces.StockQuote>>() {
            @Override
            protected List<Interfaces.StockQuote> doInBackground(Void... params) {
                return getStockQuotes(symbols);
            }

            @Override
            protected void onPostExecute(List<Interfaces.StockQuote> stockQuote) {
                callback.call(stockQuote);
            }
        }.execute();

        return null;
    }

    public List<Interfaces.StockQuote> getStockQuotes(Collection<String> symbols) {

        String param = null;
        List<Interfaces.StockQuote> ret = null;

        if (symbols != null)
            param = TextUtils.join(",", symbols);

        try {
            EquityCollection e = getEndpoints().optionDataApi().getEquityQuotes(param).execute();
            if (e == null) {
                Log.w(TAG, "Failed getting quotes from service");
                return null;
            }

            Log.d(TAG, "Got " + e.getItems().size() + " quotes from service");
            ret = getStockQuoteList(e.getItems());
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (e.getStatusCode() == 401) {
                bus.post(new LoggedOutExceptionEvent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    public void putFavorite(Position position) {
        try {
            getEndpoints().optionDataApi().putPosition(position).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void removeFavorite(Position position) {
        try {
            getEndpoints().optionDataApi().removePosition(position).execute();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void writeChainToDb(OptionChainProto.OptionChain protoChain) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long now = System.currentTimeMillis();
        try {
            db.beginTransaction();
            Log.d(TAG, "Cleaning DB");

            db.delete(Options.TABLE_NAME,
                    Schema.Options.UNDERLYING_SYMBOL + "='" + protoChain.getSymbol() + "'", null);

            db.delete(Schema.VerticalSpreads.TABLE_NAME,
                    Schema.VerticalSpreads.UNDERLYING_SYMBOL + "='" + protoChain.getSymbol() + "'", null);

            Log.d(TAG, "Writing Option Records");
            for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
                writeDateChainToDb(db, dateChain, now, protoChain);
            }

            Log.d(TAG, "Writing Spread Records");
            SpreadPopulator.updateSpreads(protoChain.getSymbol(), db);

            Log.d(TAG, "Writing favorites");
            writeFavorites(db, protoChain.getSymbol());

            Log.d(TAG, "Cleaning up");
            clearBidAsk(db, protoChain.getSymbol());

            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
    }

    private void writeFavorites(SQLiteDatabase db, String symbol) {
        ContentValues cv = new Schema.ContentValueBuilder()
                .put(Schema.VerticalSpreads.IS_FAVORITE, 1)
                .build();

        Cursor c = null;
        try {
            c = db.query(Schema.Favorites.TABLE_NAME, Schema.getProjection(Schema.Favorites.values()), Schema.Favorites.UNDERLYING_SYMBOL + "=?", new String[]{symbol}, null, null, null);
            while (c != null && c.moveToNext()) {
                db.update(Schema.VerticalSpreads.TABLE_NAME,
                        cv,
                        Schema.VerticalSpreads.BUY_SYMBOL + "=? AND " + Schema.VerticalSpreads.SELL_SYMBOL + "=?",
                        new String[]{c.getString(Schema.Favorites.BUY_SYMBOL.ordinal()), c.getString(Schema.Favorites.SELL_SYMBOL.ordinal())}
                );
            }
        } finally {
            if (c != null)
                c.close();
        }

    }

    private void clearBidAsk(SQLiteDatabase db, String symbol) {
        ContentValues cv = new Schema.ContentValueBuilder()
                .put(Options.ASK, 0)
                .put(Options.BID, 0)
                .build();

        db.update(Options.TABLE_NAME,
                cv,
                Options.UNDERLYING_SYMBOL + "=?",
                new String[]{symbol});
    }

    private void writeDateChainToDb(SQLiteDatabase db, OptionChainProto.OptionDateChain dateChain, long now, OptionChainProto.OptionChain protoChain) {

        int i = 0;

        if (Util.getDaysFromNow(new DateTime(dateChain.getExpiration())) <= 1) {
            return;
        }

        double minPrice = protoChain.getUnderlyingPrice() / 500;

        for (OptionChainProto.OptionQuote optionQuote : dateChain.getOptionsList()) {
            if (optionQuote.getAsk() < minPrice && optionQuote.getBid() < minPrice) {
                continue;
            }

            Schema.ContentValueBuilder cv = new Schema.ContentValueBuilder();
            cv.put(Options.SYMBOL, Options.getKey(protoChain.getSymbol(), dateChain.getExpiration(), optionQuote.getOptionType(), optionQuote.getStrike()))
                    .put(Options.UNDERLYING_SYMBOL, protoChain.getSymbol())
                    .put(Options.UNDERLYING_PRICE, protoChain.getUnderlyingPrice())
                    .put(Options.BID, optionQuote.getBid())
                    .put(Options.ASK, optionQuote.getAsk())
                    .put(Options.STRIKE, optionQuote.getStrike())
                    .put(Options.EXPIRATION, Util.roundToNearestFriday(new DateTime(dateChain.getExpiration())).getMillis())
                    .put(Options.DAYS_TO_EXPIRATION, Math.max(1, Util.getDaysFromNow(new DateTime(dateChain.getExpiration()))))
                    .put(Options.IV, optionQuote.getIv())
                    .put(Options.OPTION_TYPE, optionQuote.getOptionType().name().substring(0, 1))
                    .put(Options.OPEN_INTEREST, optionQuote.getOpenInterest())
                    .put(Options.TIMESTAMP_FETCH, now)
                    .put(Options.TIMESTAMP_QUOTE, protoChain.getTimestamp());

            db.insertWithOnConflict(Options.TABLE_NAME, "", cv.build(), SQLiteDatabase.CONFLICT_REPLACE);
        }
        Log.v(TAG, "Wrote date chain " + dateChain.getExpiration());
    }

    @Override
    public FusionUser getAccountUser() {
        if (fusionUser == null) {

            optionFusionApi = null;

            synchronized (TAG) {
                if (fusionUser == null && Looper.getMainLooper() != Looper.myLooper()) {

                    if (account == null && TextUtils.isEmpty(sharedPrefStore.getSessionId())) {
                        bus.post(new LoggedOutExceptionEvent());
                        return null;
                    }

                    try {
                        FusionUser user = new FusionUser();
                        if (account != null && !TextUtils.isEmpty(account.getDisplayName()))
                            user.setDisplayName(account.getDisplayName());

                        fusionUser = getEndpoints().optionDataApi().loginUser(user).execute();

                        if (!TextUtils.isEmpty(fusionUser.getSessionId()))
                            sharedPrefStore.setSessionid(fusionUser.getSessionId());

                        if (fusionUser.getUserDatamap() != null)
                            for (Map.Entry<String, Object> stringObjectEntry : fusionUser.getUserDatamap().entrySet()) {
                                userData.put(stringObjectEntry.getKey(), String.valueOf(stringObjectEntry.getValue()));
                            }
                    } catch (GoogleJsonResponseException e) {
                        e.printStackTrace();
                        if (e.getStatusCode() == 401) {
                            bus.post(new LoggedOutExceptionEvent());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        return fusionUser;
    }

    @Override
    public List<Interfaces.StockQuote> setWatchlist(Collection<String> symbols) {

        watchlist = stockQuoteProvider.get().getFromSymbols(symbols);

        EquityCollection equityCollection = null;
        try {
            equityCollection = getEndpoints().optionDataApi().setWatchlist(TextUtils.join(",", symbols)).execute();
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (e.getStatusCode() == 401) {
                bus.post(new LoggedOutExceptionEvent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (equityCollection != null) {
            if (fusionUser != null)
                fusionUser.setWatchlist(equityCollection.getItems());

            watchlist = getStockQuoteList(equityCollection.getItems());
            return watchlist;
        }
        return null;
    }

    @Override
    public List<Interfaces.StockQuote> getWatchlist() {
        if (watchlist == null) {
            FusionUser user = getAccountUser();
            if (user != null) {
                watchlist = getStockQuoteList(user.getMaterializedWatchlist());
            }
        }
        return watchlist;
    }

    @Override
    public void setUserData(String userDataKey, String userDataValue) {
        synchronized (TAG) {
            if (userDataValue == null) {
                userData.remove(userDataKey);
                userDataValue = USERDATA_NONE;
            } else
                userData.put(userDataKey, userDataValue);
        }

        try {
            getEndpoints().optionDataApi().setUserData(userDataKey, userDataValue).execute();
        } catch (GoogleJsonResponseException e) {
            e.printStackTrace();
            if (e.getStatusCode() == 401) {
                bus.post(new LoggedOutExceptionEvent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static List<Interfaces.StockQuote> getStockQuoteList(Collection<Equity> equities) {
        List ret = new ArrayList<>();
        if (equities != null)
            for (Equity equity : equities) {
                ret.add(new FusionStockQuote(equity));
            }
        return ret;
    }

    @Override
    public String getUserData(String userDataKey) {
        synchronized (TAG) {
            return userData.get(userDataKey);
        }
    }

    @Override
    public void setGoogleAccount(GoogleSignInAccount account) {
        this.account = account;

        if (account != null) {
            sharedPrefStore.setEmail(account.getEmail());
            // User is authorized.
        } else {
            sharedPrefStore.clear();
        }
    }

    private OptionFusion getEndpoints() {

        if (optionFusionApi == null) {

            // Create API handler
            OptionFusion.Builder builder = new OptionFusion.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), getHttpRequestInitializer())
                    .setApplicationName(context.getString(R.string.app_name))
                    .setRootUrl(ROOT_URL)
                    .setGoogleClientRequestInitializer(getGoogleClientRequestInitializer());

            optionFusionApi = builder.build();
        }

        return optionFusionApi;
    }

    GoogleClientRequestInitializer getGoogleClientRequestInitializer() {
        return new GoogleClientRequestInitializer() {
            @Override
            public void initialize(AbstractGoogleClientRequest<?> request) throws IOException {
                request.setDisableGZipContent(true);
            }
        };
    }

    HttpRequestInitializer getHttpRequestInitializer() {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(HttpRequest request) throws IOException {
                RequestHandler handler = new RequestHandler();
                request.setInterceptor(handler);
                request.setUnsuccessfulResponseHandler(handler);
            }
        };
    }

    class RequestHandler implements HttpExecuteInterceptor, HttpUnsuccessfulResponseHandler {

        String token;

        public void intercept(HttpRequest request) throws IOException {
            if (account != null) {
                token = account.getIdToken();
                request.getHeaders().setAuthorization("Bearer " + token);
            }

            if (!TextUtils.isEmpty(sharedPrefStore.getSessionId())) {
                request.getHeaders().set("SessionId", sharedPrefStore.getSessionId());
            }
        }

        public boolean handleResponse(
                HttpRequest request, HttpResponse response, boolean supportsRetry) {
            return false;
        }
    }

    public static GoogleApiClient getGoogleApiClient(final FragmentActivity activity, int hostId) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(Constants.WEB_CLIENT_ID)
                .requestEmail()
                .requestId()
                .build();

        GoogleApiClient.Builder ret = new GoogleApiClient.Builder(activity)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .enableAutoManage(activity, hostId, new GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                        Log.e(TAG, "Connection Failed" + connectionResult);
                        Toast.makeText(activity, R.string.connection_failed, Toast.LENGTH_SHORT);
                    }
                });

        return ret.build();
    }
}
