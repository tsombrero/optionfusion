package com.mosoft.momomentum.model.provider.amtd;

import com.google.gson.Gson;
import com.mosoft.momomentum.model.provider.Interfaces;

import org.simpleframework.xml.Default;
import org.simpleframework.xml.DefaultType;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.core.Commit;

import java.util.List;

/*

<?xml version="1.0"?>
<amtd>
  <result>OK</result>
  <quote-list>
    <error/>
    <quote>
      <error/>
      <symbol>T</symbol>
      <description>AT&amp;T INC COM</description>
      <bid>32.73</bid>
      <ask>33.58</ask>
      <bid-ask-size>0X100</bid-ask-size>
      <last>33.16</last>
      <last-trade-size>1216300</last-trade-size>
      <last-trade-date>2015-11-06 16:00:14 EST</last-trade-date>
      <open>33.22</open>
      <high>33.27</high>
      <low>32.86</low>
      <close>33.16</close>
      <volume>21494479</volume>
      <year-high>36.45</year-high>
      <year-low>30.97</year-low>
      <real-time>true</real-time>
      <exchange>NYSE</exchange>
      <asset-type>E</asset-type>
      <change>0.00</change>
      <change-percent>0.00%</change-percent>
    </quote>
  </quote-list>
</amtd>

*/
public class AmeritradeStockQuote extends AmtdResponseBase implements Interfaces.StockQuote {

    @Element(name = "quote-list", required = false)
    private Data data;

    @Root
    @Default(value = DefaultType.FIELD, required = false)
    private static class Data {
        String error;

        @ElementList(inline = true, required = false)
        List<QuoteData> quoteList;
    }

    @Root(name = "quote")
    @Default(value = DefaultType.FIELD, required = false)
    static class QuoteData {
        private String error;
        private String symbol;
        private String description;
        private double bid, ask, last, open, high, low, close, change;
    }

    QuoteData quote;

    @Commit
    public void build() {
        quote = data.quoteList.get(0);
        data = null;
    }

    @Override
    public String getSymbol() {
        return quote.symbol;
    }

    @Override
    public String getDescription() {
        return quote.description;
    }

    @Override
    public double getBid() {
        return quote.bid;
    }

    @Override
    public double getAsk() {
        return quote.ask;
    }

    @Override
    public double getLast() {
        return quote.last;
    }

    @Override
    public double getOpen() {
        return quote.open;
    }

    @Override
    public double getClose() {
        return quote.close;
    }

    @Override
    public String toJson(Gson gson) {
        return gson.toJson(this);
    }
}
