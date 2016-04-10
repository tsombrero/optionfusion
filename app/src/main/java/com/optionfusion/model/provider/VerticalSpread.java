package com.optionfusion.model.provider;

import android.os.Parcelable;

import org.joda.time.DateTime;

public interface VerticalSpread extends Parcelable {

    // Weight used in sorting by risk; higher number means lower-risk trades are more preferred
    double WEIGHT_LOWRISK = 35D;


    SpreadType getSpreadType();

    double getAsk();

    double getBid();

    double getMaxReturn();

    double getMaxValueAtExpiration();

    double getPrice_BreakEven();

    double getMaxPercentProfitAtExpiration();

    // how much $ the price can change before the max profit limit
    double getPriceChange_MaxProfit();

    // how much % the price can drop before cutting into profit
    double getPercentChange_MaxProfit();

    DateTime getExpiresDate();

    double getMaxReturnAnnualized();

    double getBreakEvenDepth();

    int getDaysToExpiration();

    boolean isInTheMoney_BreakEven();

    boolean isInTheMoney_MaxReturn();

    double getPrice_MaxReturn();

    double getPrice_MaxLoss();

    boolean isCall();

    String getUnderlyingSymbol();

    boolean isBullSpread();

    boolean isBearSpread();

    double getWeightedValue();

    String getDescriptionNoExp();

    String getDescription();

    double getBuyStrike();

    double getSellStrike();

    String getSellSymbol();

    String getBuySymbol();

    Interfaces.OptionQuote getBuy();

    Interfaces.OptionQuote getSell();

    Double getCapitalAtRisk();

    enum SpreadType {
        BULL_CALL,
        BEAR_CALL,
        BULL_PUT,
        BEAR_PUT;

        @Override
        public String toString() {
            return name().replace('_', ' ');
        }
    }
}
