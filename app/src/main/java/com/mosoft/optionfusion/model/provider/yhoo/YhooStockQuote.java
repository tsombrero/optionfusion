package com.mosoft.optionfusion.model.provider.yhoo;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.module.MomentumApplication;

/*

{
  "query": {
    "count": 1,
    "created": "2015-11-13T06:58:58Z",
    "lang": "en-US",
    "results": {
      "quote": {
        "symbol": "T",
        "Ask": "32.80",
        "AverageDailyVolume": "26993300",
        "Bid": "32.68",
        "AskRealtime": null,
        "BidRealtime": null,
        "BookValue": "19.80",
        "Change_PercentChange": "-0.23 - -0.70%",
        "Change": "-0.23",
        "Commission": null,
        "Currency": "USD",
        "ChangeRealtime": null,
        "AfterHoursChangeRealtime": null,
        "DividendShare": "1.88",
        "LastTradeDate": "11/12/2015",
        "TradeDate": null,
        "EarningsShare": "0.95",
        "ErrorIndicationreturnedforsymbolchangedinvalid": null,
        "EPSEstimateCurrentYear": "2.66",
        "EPSEstimateNextYear": "2.83",
        "EPSEstimateNextQuarter": "0.72",
        "DaysLow": "32.63",
        "DaysHigh": "32.\n86",
        "YearLow": "30.97",
        "YearHigh": "36.45",
        "HoldingsGainPercent": null,
        "AnnualizedGain": null,
        "HoldingsGain": null,
        "HoldingsGainPercentRealtime": null,
        "HoldingsGainRealtime": null,
        "MoreInfo": null,
        "OrderBookRealtime": null,
        "MarketCapitalization": "201.11B",
        "MarketCapRealtime": null,
        "EBITDA": "32.65B",
        "ChangeFromYearLow": "1.72",
        "PercentChangeFromYearLow": "+5.\n55%",
        "LastTradeRealtimeWithTime": null,
        "ChangePercentRealtime": null,
        "ChangeFromYearHigh": "-3.76",
        "PercebtChangeFromYearHigh": "-10.32%",
        "LastTradeWithTime": "4:00pm - <b>32.69</b>",
        "LastTradePriceOnly": "32.69",
        "HighLimit": null,
        "LowLimit": null,
        "DaysRange": "32.63 - 32.86",
        "DaysRangeRealtime": null,
        "FiftydayMovingAverage": "33.17",
        "TwoHundreddayMovingAve\nrage": "33.96",
        "ChangeFromTwoHundreddayMovingAverage": "-1.27",
        "PercentChangeFromTwoHundreddayMovingAverage": "-3.74%",
        "ChangeFromFiftydayMovingAverage": "-0.48",
        "PercentChangeFromFiftydayMovingAverage": "-1.45%",
        "Name": "AT&T Inc.",
        "Notes": null,
        "Open": "32.84",
        "PreviousClose": "32.92",
        "PricePaid": null,
        "ChangeinPercent": "-0.70%",
        "PriceSales": "1.46",
        "Pric\neBook": "1.66",
        "ExDividendDate": "10/7/2015",
        "PERatio": "34.37",
        "DividendPayDate": "11/2/2015",
        "PERatioRealtime": null,
        "PEGRatio": "2.12",
        "PriceEPSEstimateCurrentYear": "12.29",
        "PriceEPSEstimateNextYear": "11.55",
        "Symbol": "T",
        "SharesOwned": null,
        "ShortRatio": "4.40",
        "LastTradeTime": "4:00pm",
        "TickerTrend": null,
        "OneyrTargetPrice": "36.96",
        "Volume": "17408424",
        "HoldingsValue": null,
        "HoldingsValueRealtime": null,
        "YearRange": "30.97 - 36.45",
        "DaysValueChange": null,
        "DaysValueChangeRealtime": null,
        "StockExchange": "NYQ",
        "DividendYield": "5.91",
        "PercentChange": "-0.70%"
      }
    }
}

 */

public class YhooStockQuote implements Interfaces.StockQuote {

    private QueryData query;

    private static class QueryData {
        ResultsData results;
    }

    private static class ResultsData {
        QuoteData quote;
    }

    private static class QuoteData {
        String symbol;

        @SerializedName("Name")
        String name;

        @SerializedName("Bid")
        Double bid;

        @SerializedName("Ask")
        Double ask;

        @SerializedName("LastTradePriceOnly")
        Double last;

        @SerializedName("Open")
        Double open;

        @SerializedName("PreviousClose")
        Double close;
    }

    @Override
    public String getSymbol() {
        return query.results.quote.symbol;
    }

    @Override
    public String getDescription() {
        return query.results.quote.name;
    }

    @Override
    public double getBid() {
        return query.results.quote.bid;
    }

    @Override
    public double getAsk() {
        return query.results.quote.ask;
    }

    @Override
    public double getLast() {
        return query.results.quote.last;
    }

    @Override
    public double getOpen() {
        return query.results.quote.open;
    }

    @Override
    public double getClose() {
        return query.results.quote.close;
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    @Override
    public MomentumApplication.Provider getProvider() {
        return MomentumApplication.Provider.YAHOO;
    }
}
