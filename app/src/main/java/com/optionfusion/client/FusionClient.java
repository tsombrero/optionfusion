package com.optionfusion.client;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.util.Log;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.optionfusion.BuildConfig;
import com.optionfusion.R;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.com.backend.optionFusion.OptionFusion;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.EquityCollection;
import com.optionfusion.com.backend.optionFusion.model.OptionChain;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;
import com.optionfusion.db.Schema.Options;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.backend.FusionOptionChain;
import com.optionfusion.module.OptionFusionApplication;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

public class FusionClient implements ClientInterfaces.SymbolLookupClient, ClientInterfaces.OptionChainClient {

    OptionFusion optionFusionApi;

    private GoogleSignInAccount account;

    Context context;

    private static final String ROOT_URL = BuildConfig.ROOT_URL;

    private static final String TAG = "FusionClient";

    @Inject
    DbHelper dbHelper;

    public FusionClient(Context context, GoogleSignInAccount acct) {
        this.context = context;
        this.account = acct;
        OptionFusionApplication.from(context).getComponent().inject(this);
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
    public void getOptionChain(final String symbol, final ClientInterfaces.Callback<Interfaces.OptionChain> callback) {
        new AsyncTask<Void, Void, FusionOptionChain>() {
            @Override
            protected FusionOptionChain doInBackground(Void... params) {
                try {
                    OptionChain chain = getEndpoints().optionDataApi().getEodChain(symbol).execute();
                    OptionChainProto.OptionChain protoChain = OptionChainProto.OptionChain.parseFrom(chain.decodeChainData());
                    writeChainToDb(protoChain);

                    return new FusionOptionChain(protoChain);

                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute();
    }

    private void writeChainToDb(OptionChainProto.OptionChain protoChain) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        try {
            db.beginTransaction();

            for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
                writeDateChainToDb(db, protoChain.getStockquote(), dateChain);
            }

            updateSpreads(db);

            db.setTransactionSuccessful();
        } finally {
            if (db.inTransaction())
                db.endTransaction();
        }
    }

    private void updateSpreads(SQLiteDatabase db) {
        StringBuilder sb = new StringBuilder("INSERT OR REPLACE INTO " + Schema.VerticalSpreads.getTableName())
                .append(" (")
                .append(TextUtils.join(",", Schema.getColumnNames(Schema.VerticalSpreads.values())))
                .append(")");


    }

    // calculate column from
    // SELECT * FROM Options buy JOIN Options sell
    // WHERE buy.underlying_symbol = sell.underlying_symbol
    //  AND buy.option_type = sell.option_type
    //  AND buy.symbol != sell.symbol

    private String appendClculatedColumnValue(Schema.VerticalSpreads spreadColumn) {
        switch(spreadColumn) {

            case BUY_SIDE:
                return "buy.SYMBOL";
            case SELL_SIDE:
                return "sell.SYMBOL";
            case IS_BULLISH:
                return "";
            case IS_PUT_SPREAD:
                break;
            case NET_ASK:
                break;
            case NET_BID:
                break;
            case EXPIRATION:
                break;
            case MAX_RETURN_ABSOLUTE:
                break;
            case MAX_RETURN_PERCENT:
                break;
            case MAX_RETURN_ANNUALIZED:
                break;
            case MAX_VALUE_AT_EXPIRATION:
                break;
            case PRICE_AT_BREAK_EVEN:
                break;
            case WEIGHTED_RISK:
                break;
            case BUFFER_TO_MAX_PROFIT:
                break;
            case BUFFER_TO_MAX_PROFIT_PERCENT:
                break;
            case BUFFER_TO_BREAK_EVEN:
                break;
            case BUFFER_TO_BREAK_EVEN_PERCENT:
                break;
            case TIMESTAMP_QUOTE:
                break;
            case TIMESTAMP_FETCH:
                break;
        }
        return "";
    }

    private void writeDateChainToDb(SQLiteDatabase db, OptionChainProto.StockQuote stockQuote, OptionChainProto.OptionDateChain dateChain) {
        long now = System.currentTimeMillis();

        for (OptionChainProto.OptionQuote optionQuote : dateChain.getOptionsList()) {
            Schema.ContentValueBuilder cv = new Schema.ContentValueBuilder();
            cv.put(Options.SYMBOL, Options.getKey(stockQuote.getSymbol(), dateChain.getExpiration(), optionQuote.getOptionType(), optionQuote.getStrike()))
                    .put(Options.SYMBOL_UNDERLYING, stockQuote.getSymbol())
                    .put(Options.BID, optionQuote.getBid())
                    .put(Options.ASK, optionQuote.getAsk())
                    .put(Options.STRIKE, optionQuote.getStrike())
                    .put(Options.EXPIRATION, dateChain.getExpiration())
                    .put(Options.IV, optionQuote.getIv())
                    .put(Options.OPTION_TYPE, optionQuote.getOptionType().ordinal())
                    .put(Options.OPEN_INTEREST, optionQuote.getOpenInterest())
                    .put(Options.TIMESTAMP_FETCH, now)
                    .put(Options.TIMESTAMP_QUOTE, stockQuote.getTimestamp());

            db.insertWithOnConflict(Options.getTableName(), "", cv.build(), SQLiteDatabase.CONFLICT_REPLACE);
        }
        Log.v(TAG, "Wrote date chain " + dateChain.getExpiration());
    }


    private OptionFusion getEndpoints() {

        if (optionFusionApi == null) {

            // Create API handler
            OptionFusion.Builder builder = new OptionFusion.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), getRequestInitializer())
                    .setApplicationName(context.getString(R.string.app_name))
                    .setRootUrl(ROOT_URL);

            optionFusionApi = builder.build();
        }

        return optionFusionApi;
    }

    /**
     * Returns appropriate HttpRequestInitializer depending whether the application is configured to
     * require users to be signed in or not.
     *
     * @return an appropriate HttpRequestInitializer.
     */
    HttpRequestInitializer getRequestInitializer() {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(final HttpRequest request) {
                if (account != null) {
                    RequestHandler handler = new RequestHandler();
                    request.setInterceptor(handler);
                    request.setUnsuccessfulResponseHandler(handler);
                }
            }
        };
    }

    // Code borrowed from GoogleAccountCredential

    class RequestHandler implements HttpExecuteInterceptor, HttpUnsuccessfulResponseHandler {

        /**
         * Whether we've received a 401 error code indicating the token is invalid.
         */
        boolean received401;
        String token;

        public void intercept(HttpRequest request) throws IOException {
            token = account.getIdToken();
            request.getHeaders().setAuthorization("Bearer " + token);
        }

        public boolean handleResponse(
                HttpRequest request, HttpResponse response, boolean supportsRetry) {
            if (response.getStatusCode() == 401 && !received401) {
                received401 = true;
                try {
                    GoogleAuthUtil.clearToken(context, token);
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        }

    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
}
