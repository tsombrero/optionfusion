package com.optionfusion.client;

import android.content.Context;
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
import com.optionfusion.util.Util;

import org.joda.time.DateTime;
import org.sqlite.database.sqlite.SQLiteDatabase;

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
        long now = System.currentTimeMillis();
        Log.i(TAG, "TACO Starting Transaction");
        try {
            db.beginTransaction();

            for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
                writeDateChainToDb(db, protoChain.getStockquote(), dateChain, now);
            }

            db.delete(Schema.VerticalSpreads.getTableName(),
                    Schema.VerticalSpreads.TIMESTAMP_QUOTE.name() + "=" + protoChain.getTimestamp(), null);

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
                        " and buy.underlying_symbol = sell.underlying_symbol " +
                        " and buy.symbol != sell.symbol " +
                        " and buy.expiration == sell.expiration " +
                        " and max_return_absolute >= 0.05 " +
                        " and BUY_TO_SELL_PRICE_RATIO > 0.1" +
                        " and sell.open_interest > 0" +
                        ";");

        Log.i(TAG, "TACO Inserting spreads:");
        db.execSQL(sb.toString());
        Log.i(TAG, "TACO Done inserting spreads");
    }

    // calculate columns that generate the obscene sql query
    private String appendClculatedColumnValue(Schema.VerticalSpreads spreadColumn) {
        String sql = "0";
        switch (spreadColumn) {
            case UNDERLYING_SUMBOL:
                sql = "buy." + Options.UNDERLYING_SYMBOL;
                break;
            case BUY_SYMBOL:
                sql = "buy." + Options.SYMBOL;
                break;
            case SELL_SYMBOL:
                sql = "sell." + Options.SYMBOL;
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
                        "WHEN isBullCall THEN " +
                        " sell.bid / buy.ask " +
                        "WHEN isBearPut THEN " +
                        " sell.bid / buy.ask " +
                        "WHEN isBearCall THEN " +
                        " buy.ask / sell.bid " +
                        "WHEN isBullPut THEN " +
                        " buy.ask / sell.bid " +
                        "END";
                break;
            case IS_CREDIT:
                sql = "CASE " +
                        "when isBullCall OR isBearPut THEN 0 " +
                        "ELSE 1 " +
                        "END";
                break;
            case NET_ASK:
                sql = "net_ask";
                break;
            case NET_BID:
                sql = "net_bid";
                break;
            case EXPIRATION:
                sql = "buy." + Options.EXPIRATION;
                break;
            case DAYS_TO_EXPIRATION:
                sql = "buy." + Options.DAYS_TO_EXPIRATION;
                break;
            case MAX_RETURN_ABSOLUTE:
                sql = "CASE  " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " normal_max_value - net_ask " +
                        "ELSE " +
                        " net_ask * -1 " +
                        "END";
                break;
            case MAX_RETURN_PERCENT:
                sql = "CASE " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " (normal_max_value - net_ask) / net_ask " +
                        "ELSE " +
                        // max_return_absolute / capital_at_risk
                        " (net_ask * -1) / (normal_max_value + net_ask) " +
                        "END";
                break;
            case MAX_RETURN_MONTHLY:
                // periodic_roi(principal, final, days held, days in period)
                sql = "CASE " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " periodic_roi(net_ask, normal_max_value, buy.days_to_expiration, 30) " +
                        "ELSE " +
                        // principal: capital_at_risk
                        // final: normal_max_value
                        " periodic_roi(normal_max_value + net_ask, normal_max_value, buy.days_to_expiration, 30) " +
                        "END";
                break;
            case MAX_RETURN_ANNUALIZED:
                // periodic_roi(principal, final, days held, days in period)
                sql = "CASE " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " periodic_roi(net_ask, normal_max_value, buy.days_to_expiration, 365) " +
                        "ELSE " +
                        // principal: max value - credit from trade
                        // final: credit from trade
                        " periodic_roi(normal_max_value + net_ask, normal_max_value, buy.days_to_expiration, 365) " +
                        "END";
                break;
            case MAX_VALUE_AT_EXPIRATION:
                sql = "CASE " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " normal_max_value " +
                        "ELSE " +
                        " credit_max_value" +
                        "END";
                break;
            case CAPITAL_AT_RISK:
                sql = "CASE " +
                        "when isBullCall OR isBearPut THEN " +
                        " net_ask " +
                        "ELSE " +
                        " normal_max_value + net_ask " +
                        "END";
                break;
            case CAPITAL_AT_RISK_PERCENT:
                sql = "CASE " +
                        "when isBearCall OR isBullPut THEN " +
                        " (normal_max_value + net_ask) / net_ask * -1 " +
                        "ELSE 1 " +
                        "END";
                break;
            case PRICE_AT_BREAK_EVEN:
                sql = "CASE " +
                        "when isBullCall THEN " +
                        " bullCall_breakEven " +
                        "when isBearPut THEN " +
                        " bearPut_breakEven " +
                        "when isBearCall THEN " +
                        " bearCall_breakEven " +
                        "ELSE " +
                        " bullPut_breakEven " +
                        "END";
                break;
            case WEIGHTED_RISK:
                sql = "CASE " +
                        "when isBullCall THEN " +
                        "min(.5, periodic_roi(net_ask, normal_max_value, buy.days_to_expiration, 365) / 5.0) + (WEIGHT_LOWRISK * (buy.underlying_price - bullCall_breakEven) / sell.underlying_price) " +
                        "when isBearPut THEN " +
                        "min(.5, periodic_roi(net_ask, normal_max_value, buy.days_to_expiration, 365) / 5.0) + (WEIGHT_LOWRISK * (bearPut_breakEven - buy.underlying_price) / sell.underlying_price) " +
                        "when isBearCall THEN " +
                        "min(.5, periodic_roi(normal_max_value + net_ask, normal_max_value, buy.days_to_expiration, 365) / 5.0) + (WEIGHT_LOWRISK * (bearCall_breakEven - sell.underlying_price) / sell.underlying_price) " +
                        "ELSE " +
                        "min(.5, periodic_roi(normal_max_value + net_ask, normal_max_value, buy.days_to_expiration, 365) / 5.0) + (WEIGHT_LOWRISK * (buy.underlying_price - bullPut_breakEven) / sell.underlying_price) " +
                        "END";
                break;
            case BUFFER_TO_MAX_PROFIT:
                sql = "CASE " +
                        "when buy.strike > sell.strike THEN " +
                        " sell.strike - buy.underlying_price " +
                        "ELSE " +
                        " buy.underlying_price - sell.strike " +
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
                        "when isBullCall THEN " +
                        " buy.underlying_price - bullCall_breakEven " +
                        "when isBearPut THEN " +
                        " bearPut_breakEven - buy.underlying_price " +
                        "when isBearCall THEN " +
                        " bearCall_breakEven - sell.underlying_price " +
                        "when isBullPut THEN " +
                        " buy.underlying_price - bullPut_breakEven  " +
                        "END ";
                break;
            case BUFFER_TO_BREAK_EVEN_PERCENT:
                sql = "CASE " +
                        "when isBullCall THEN " +
                        " (buy.underlying_price - bullCall_breakEven) / buy.underlying_price " +
                        "when isBearPut THEN " +
                        " (bearPut_breakEven - buy.underlying_price) / buy.underlying_price " +
                        "when isBearCall THEN " +
                        " (bearCall_breakEven - sell.underlying_price) / buy.underlying_price " +
                        "when isBullPut THEN " +
                        " (buy.underlying_price - bullPut_breakEven) / buy.underlying_price " +
                        "END";
                break;
            case TIMESTAMP_QUOTE:
                sql = "buy." + Options.TIMESTAMP_QUOTE;
                break;
            case TIMESTAMP_FETCH:
                sql = "buy." + Options.TIMESTAMP_FETCH;
                break;
        }

        boolean done;
        do {
            done = true;
            for (Replacements replacement : Replacements.values()) {
                if (sql.contains(replacement.name())) {
                    sql = sql.replaceAll(replacement.name(), "(" + replacement.replacementText + ")");
                    done = false;
                }
            }
        } while (!done);

        return sql + " AS " + spreadColumn;
    }

    // very poor excuse for a template language, quick and dirty.
    enum Replacements {
        normal_max_value("abs(buy.strike - sell.strike)"),
        credit_max_value("net_ask * -1"),
        bullCall_breakEven("buy.strike + net_ask"),
        bearPut_breakEven("buy.strike - net_ask"),
        bearCall_breakEven("sell.strike - net_ask"),
        bullPut_breakEven("sell.strike + net_ask"),
        isBullCall("buy.option_type == 'C' AND buy.strike < sell.strike"),
        isBearCall("buy.option_type == 'C' AND buy.strike > sell.strike"),
        isBearPut("buy.option_type == 'P' AND buy.strike > sell.strike"),
        isBullPut("buy.option_type == 'P' AND buy.strike < sell.strike"),
        net_ask("round(buy.ask - sell.bid, 4)"),
        net_bid("round(buy.bid - sell.ask, 4)"),
        WEIGHT_LOWRISK(String.valueOf(Spread.WEIGHT_LOWRISK));

        String replacementText;

        Replacements(String replacementText) {
            this.replacementText = replacementText;
        }
    }

    private void writeDateChainToDb(SQLiteDatabase db, OptionChainProto.StockQuote stockQuote, OptionChainProto.OptionDateChain dateChain, long now) {

        for (OptionChainProto.OptionQuote optionQuote : dateChain.getOptionsList()) {
            Schema.ContentValueBuilder cv = new Schema.ContentValueBuilder();
            cv.put(Options.SYMBOL, Options.getKey(stockQuote.getSymbol(), dateChain.getExpiration(), optionQuote.getOptionType(), optionQuote.getStrike()))
                    .put(Options.UNDERLYING_SYMBOL, stockQuote.getSymbol())
                    .put(Options.UNDERLYING_PRICE, stockQuote.getClose())
                    .put(Options.BID, optionQuote.getBid())
                    .put(Options.ASK, optionQuote.getAsk())
                    .put(Options.STRIKE, optionQuote.getStrike())
                    .put(Options.EXPIRATION, dateChain.getExpiration())
                    .put(Options.DAYS_TO_EXPIRATION, Math.max(1, Util.getDaysFromNow(new DateTime(dateChain.getExpiration()))))
                    .put(Options.IV, optionQuote.getIv())
                    .put(Options.OPTION_TYPE, optionQuote.getOptionType().name().substring(0,1))
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
