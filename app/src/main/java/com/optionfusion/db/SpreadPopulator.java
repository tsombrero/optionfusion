package com.optionfusion.db;

import android.text.TextUtils;
import android.util.Log;

import com.optionfusion.BuildConfig;
import com.optionfusion.model.provider.VerticalSpread;

import org.sqlite.database.sqlite.SQLiteDatabase;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

public class SpreadPopulator {

    // Class for thge most insane sql query I've written in my entire life

    private static final String TAG = "SpreadPopulator";

    public static void updateSpreads(String symbol, SQLiteDatabase db) {
        ArrayList<String> colNames = new ArrayList<>();
        ArrayList<String> colValues = new ArrayList<>();

        symbol = symbol.replace("'", ".");
        symbol = symbol.replace(" ", ".");

        for (Schema.VerticalSpreads col : Schema.VerticalSpreads.values()) {
            colNames.add(col.name());
            colValues.add(appendCalculatedColumnValue(col));
        }

        StringBuilder sb = new StringBuilder("INSERT OR REPLACE INTO " + Schema.VerticalSpreads.TABLE_NAME)
                .append(" (")
                .append(TextUtils.join(",", colNames))
                .append(") SELECT ")
                .append(TextUtils.join(",", colValues))
                .append(" FROM Options buy, Options sell where buy.option_type = sell.option_type " +
                        " and buy.underlying_symbol = sell.underlying_symbol " +
                        " and buy.underlying_symbol = '" + symbol + "' " +
                        " and buy.symbol != sell.symbol " +
                        " and buy.expiration == sell.expiration " +
                        " and max_gain_percent >= 0.02 " +
                        " and max_gain_annualized >= 0.02 " +
                        " and abs(net_ask) > 0.05 and abs(net_bid) > 0.05 " +
                        " and max_gain_annualized < 1000000 " +
                        " and min(buy.ask, sell.bid) / max(buy.ask, sell.bid) > 0.1" +
                        ";");

        Log.d(TAG, "Inserting spreads:");
        db.execSQL(sb.toString());
        Log.d(TAG, "Done inserting spreads");

        if (BuildConfig.DEBUG) {
            try {
                FileOutputStream fos = new FileOutputStream(new File("/sdcard", "giantQuery.sql"));
                fos.write(sb.toString().getBytes());
                fos.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // very poor excuse for a template language, quick and dirty.
    private enum Replacements {
        normal_max_value("abs(buy.strike - sell.strike)"),
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
        WEIGHT_LOWRISK(String.valueOf(VerticalSpread.WEIGHT_LOWRISK));

        String replacementText;

        Replacements(String replacementText) {
            this.replacementText = replacementText;
        }
    }

    // calculate columns that generate the obscene sql query
    private static String appendCalculatedColumnValue(Schema.VerticalSpreads spreadColumn) {
        String sql = "0";
        switch (spreadColumn) {
            case UNDERLYING_SYMBOL:
                sql = "buy." + Schema.Options.UNDERLYING_SYMBOL;
                break;
            case UNDERLYING_PRICE:
                sql = "buy." + Schema.Options.UNDERLYING_PRICE;
                break;
            case BUY_SYMBOL:
                sql = "buy." + Schema.Options.SYMBOL;
                break;
            case SELL_SYMBOL:
                sql = "sell." + Schema.Options.SYMBOL;
                break;
            case BUY_STRIKE:
                sql = "buy." + Schema.Options.STRIKE;
                break;
            case SELL_STRIKE:
                sql = "sell." + Schema.Options.STRIKE;
                break;
            case IS_BULLISH:
                sql = "CASE when buy.strike < sell.strike THEN 1 ELSE 0 END";
                break;
            case SPREAD_TYPE:
                sql = "CASE " +
                        "when isBullCall THEN " +
                        VerticalSpread.SpreadType.BULL_CALL.ordinal() +
                        " when isBearPut THEN " +
                        VerticalSpread.SpreadType.BEAR_PUT.ordinal() +
                        " when isBearCall THEN " +
                        VerticalSpread.SpreadType.BEAR_CALL.ordinal() +
                        " ELSE " +
                        VerticalSpread.SpreadType.BULL_PUT.ordinal() +
                        " END";
                break;
            case PRICE_AT_MAX_GAIN:
                sql = "sell." + Schema.Options.STRIKE;
                break;
            case PRICE_AT_MAX_LOSS:
                sql = "buy." + Schema.Options.STRIKE;
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
                sql = "buy." + Schema.Options.EXPIRATION;
                break;
            case DAYS_TO_EXPIRATION:
                sql = "buy." + Schema.Options.DAYS_TO_EXPIRATION;
                break;
            case MAX_GAIN_ABSOLUTE:
                sql = "CASE  " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " normal_max_value - net_ask " +
                        "ELSE " +
                        " net_ask * -1 " +
                        "END";
                break;
            case MAX_GAIN_PERCENT:
                sql = "CASE " +
                        "WHEN isBullCall OR isBearPut THEN " +
                        " (normal_max_value - net_ask) / net_ask " +
                        "ELSE " +
                        // max_return_absolute / capital_at_risk
                        " (net_ask * -1) / (normal_max_value + net_ask) " +
                        "END";
                break;
//            case MAX_GAIN_MONTHLY:
//                // periodic_roi(principal, final, days held, days in period)
//                sql = "CASE " +
//                        "WHEN isBullCall OR isBearPut THEN " +
//                        " periodic_roi(net_ask, normal_max_value, buy.days_to_expiration, 30) " +
//                        "ELSE " +
//                        // principal: capital_at_risk
//                        // final: normal_max_value
//                        " periodic_roi(normal_max_value + net_ask, normal_max_value, buy.days_to_expiration, 30) " +
//                        "END";
//                break;
            case MAX_GAIN_ANNUALIZED:
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
                sql = " normal_max_value ";
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
            case RISK_AVERSION_SCORE:
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
            case BUFFER_TO_MAX_GAIN:
                sql = "CASE " +
                        "when buy.strike > sell.strike THEN " +
                        " sell.strike - buy.underlying_price " +
                        "ELSE " +
                        " buy.underlying_price - sell.strike " +
                        "END ";
                break;
            case BUFFER_TO_MAX_GAIN_PERCENT:
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
                sql = "buy." + Schema.Options.TIMESTAMP_QUOTE;
                break;
            case TIMESTAMP_FETCH:
                sql = "buy." + Schema.Options.TIMESTAMP_FETCH;
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
}
