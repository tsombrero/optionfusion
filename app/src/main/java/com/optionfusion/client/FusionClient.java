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
import com.optionfusion.model.Spread;
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
        Log.i(TAG, "TACO Starting Transaction");
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
        Log.i(TAG, "TACO Transaction Committed");
    }

    private void updateSpreads(SQLiteDatabase db) {
        ArrayList<String> colNames = new ArrayList<>();
        ArrayList<String> colValues = new ArrayList<>();

        for (Schema.VerticalSpreads col : Schema.VerticalSpreads.values()) {
            colNames.add(col.name());
            colValues.add(appendClculatedColumnValue(col));
        }

        StringBuilder sb = new StringBuilder("INSERT OR REPLACE INTO " + Schema.VerticalSpreads.getTableName())
                .append(" (")
                .append(TextUtils.join(",", colNames))
                .append(") SELECT ")
                .append(TextUtils.join(",", colValues))
                .append(" FROM Options buy, Options sell where buy.option_type = sell.option_type " +
                        " and buy.symbol_underlying = sell.symbol_underlying " +
                        " and buy.symbol != sell.symbol " +
                        " and buy.expiration == sell.expiration " +
                        " and max_return_absolute >= 0.05 " +
                        " and BUY_TO_SELL_PRICE_RATIO > 0.1;");

        Log.i(TAG, "TACO Inserting spreads:");
        db.execSQL(sb.toString());
        Log.i(TAG, "TACO Done inserting spreads");
    }

    // calculate columns that generate the obscene sql query
    private String appendClculatedColumnValue(Schema.VerticalSpreads spreadColumn) {
        String sql = null;
        switch (spreadColumn) {

            case BUY_SYMBOL:
                sql = "buy.SYMBOL";
                break;
            case SELL_SYMBOL:
                sql = "sell.SYMBOL";
                break;
            case BUY_STRIKE:
                sql = "buy." + Options.STRIKE;
                break;
            case SELL_STRIKE:
                sql = "sell." + Options.STRIKE;
                break;
            case IS_BULLISH:
                sql = "CASE when buy.strike < sell.strike THEN 1 ELSE 0 END";
                break;
            case BUY_TO_SELL_PRICE_RATIO:
                sql = "CASE " +
                        "WHEN buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        " sell.bid / buy.ask " +
                        "WHEN buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        " sell.bid / buy.ask " +
                        "WHEN buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        " buy.ask / sell.bid " +
                        "ELSE" +
                        " buy.ask / sell.bid " +
                        "END";
                break;
            case IS_CREDIT:
                sql = "CASE " +
                        "when (buy.strike < sell.strike and buy.option_type == 'C') THEN 0 " +
                        "when (buy.strike > sell.strike and buy.option_type == 'P') THEN 0 " +
                        "ELSE 1 " +
                        "END";
                break;
            case NET_ASK:
                sql = "buy.ask - sell.bid";
            case NET_BID:
                sql = "buy.bid - sell.ask";
                break;
            case EXPIRATION:
                sql = "buy." + Options.EXPIRATION;
                break;
            case DAYS_TO_EXPIRATION:
                sql = "buy." + Options.DAYS_TO_EXPIRATION;
                break;
            case MAX_RETURN_ABSOLUTE:
                sql = "CASE  " +
                        "WHEN buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "sell.bid - buy.ask " +
                        "WHEN buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "sell.strike - buy.strike - buy.ask + sell.bid " +
                        "WHEN buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "buy.strike - sell.strike - buy.ask + sell.bid " +
                        "ELSE " +
                        "sell.bid - buy.ask " +
                        "END";
                break;
            case MAX_RETURN_PERCENT:
                sql = "CASE " +
                        "WHEN buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "(sell.strike - buy.strike) / (buy.ask - sell.bid) " +
                        "WHEN buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "(sell.strike - buy.strike) / (buy.ask - sell.bid) " +
                        "ELSE 1 " +
                        "END ";
                break;
            case MAX_RETURN_DAILY:
                sql = "CASE " +
                        "WHEN buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "(sell.strike - buy.strike - buy.ask + sell.bid) / sell.days_to_expiration " +
                        "WHEN buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "(buy.strike - sell.strike - buy.ask + sell.bid) / sell.days_to_expiration " +
                        "WHEN buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "(sell.bid - buy.ask) / sell.days_to_expiration " +
                        "ELSE " +
                        "(sell.bid - buy.ask) / sell.days_to_expiration " +
                        "END";
                break;
            case MAX_VALUE_AT_EXPIRATION:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "sell.strike - buy.strike " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "buy.strike - sell.strike " +
                        "ELSE 0 " +
                        "END";
                break;
            case CAPITAL_AT_RISK:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "buy.ask - sell.bid " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "buy.ask - sell.bid " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "(buy.strike - sell.strike) - (sell.bid - buy.ask) " +
                        "ELSE " +
                        "(sell.strike - buy.strike) - (sell.bid - buy.ask) " +
                        "END";
                break;
            case CAPITAL_AT_RISK_PERCENT:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "((buy.strike - sell.strike) - (sell.bid - buy.ask)) / (buy.ask - sell.bid) " +
                        "when buy.option_type == 'P' AND buy.strike < sell.strike THEN " +
                        "((sell.strike - buy.strike) - (buy.ask - sell.bid)) / (buy.ask - sell.bid) " +
                        "ELSE 1 " +
                        "END";
                break;
            case PRICE_AT_BREAK_EVEN:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "buy.strike + buy.ask - sell.bid " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "buy.strike - buy.ask + sell.bid " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "sell.strike + sell.bid - buy.ask " +
                        "ELSE " +
                        "sell.strike - sell.bid + buy.ask " +
                        "END";
            case WEIGHTED_RISK:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        "min(.5, ((sell.strike - buy.strike - buy.ask + sell.bid) / sell.days_to_expiration) * 72) + ($WEIGHT_LOWRISK * (buy.underlying_price - buy.strike + buy.ask - sell.bid) / sell.underlying_price) " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        "min(.5, ((buy.strike - sell.strike - buy.ask + sell.bid) / sell.days_to_expiration) * 72) + ($WEIGHT_LOWRISK * (buy.strike - buy.ask + sell.bid - buy.underlying_price) / sell.underlying_price) " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        "min(.5, ((sell.bid - buy.ask) / sell.days_to_expiration) * 72) + ($WEIGHT_LOWRISK * (buy.strike - buy.ask + sell.bid - buy.underlying_price) / sell.underlying_price) " +
                        "ELSE " +
                        "min(.5, ((sell.bid - buy.ask) / sell.days_to_expiration) * 72) + ($WEIGHT_LOWRISK * (buy.underlying_price - sell.strike + sell.bid - buy.ask) / sell.underlying_price) " +
                        "END"
                .replaceAll("$WEIGHT_LOWRISK", String.valueOf(Spread.WEIGHT_LOWRISK));
                break;
            case BUFFER_TO_MAX_PROFIT:
                sql = "CASE " +
                        "when buy.strike > sell.strike THEN " +
                        "sell.strike - buy.underlying_price " +
                        "ELSE " +
                        "buy.underlying_price - sell.strike " +
                        "END ";
                break;
            case BUFFER_TO_MAX_PROFIT_PERCENT:
                sql = "CASE " +
                        "when buy.strike > sell.strike THEN " +
                        "(sell.strike - buy.underlying_price) / buy.underlying_price " +
                        "ELSE " +
                        "(buy.underlying_price - sell.strike) / buy.underlying_price " +
                        "END";
                break;
            case BUFFER_TO_BREAK_EVEN:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        " buy.underlying_price - (buy.strike + buy.ask - sell.bid) " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        " (buy.strike - buy.ask + sell.bid) - buy.underlying_price " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        " (sell.strike + sell.bid - buy.ask) - sell.underlying_price " +
                        "ELSE " +
                        " buy.underlying_price - (sell.strike - sell.bid + buy.ask)  " +
                        "END ";
                break;
            case BUFFER_TO_BREAK_EVEN_PERCENT:
                sql = "CASE " +
                        "when buy.option_type == 'C' AND buy.strike < sell.strike THEN " +
                        " (buy.underlying_price - (buy.strike + buy.ask - sell.bid)) / buy.underlying_price " +
                        "when buy.option_type == 'P' AND buy.strike > sell.strike THEN " +
                        " ((buy.strike - buy.ask + sell.bid) - buy.underlying_price) / buy.underlying_price " +
                        "when buy.option_type == 'C' AND buy.strike > sell.strike THEN " +
                        " ((sell.strike + sell.bid - buy.ask) - sell.underlying_price) / buy.underlying_price " +
                        "ELSE " +
                        " (buy.underlying_price - (sell.strike - sell.bid + buy.ask)) / buy.underlying_price " +
                        "END";
                break;
            case TIMESTAMP_QUOTE:
                sql = "buy." + Options.TIMESTAMP_QUOTE;
                break;
            case TIMESTAMP_FETCH:
                sql = "buy." + Options.TIMESTAMP_FETCH;
                break;
        }

        return sql + " AS " + spreadColumn;
    }

    private void writeDateChainToDb(SQLiteDatabase db, OptionChainProto.StockQuote stockQuote, OptionChainProto.OptionDateChain dateChain) {
        long now = System.currentTimeMillis();

        for (OptionChainProto.OptionQuote optionQuote : dateChain.getOptionsList()) {
            Schema.ContentValueBuilder cv = new Schema.ContentValueBuilder();
            cv.put(Options.SYMBOL, Options.getKey(stockQuote.getSymbol(), dateChain.getExpiration(), optionQuote.getOptionType(), optionQuote.getStrike()))
                    .put(Options.SYMBOL_UNDERLYING, stockQuote.getSymbol())
                    .put(Options.UNDERLYING_PRICE, stockQuote.getClose())
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
