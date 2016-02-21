package com.optionfusion.model.provider.backend;

import com.google.gson.Gson;
import com.optionfusion.backend.protobuf.OptionChainProto;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.Spread;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.module.OptionFusionApplication;

import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class FusionOptionChain implements Interfaces.OptionChain {


    private final OptionChainProto.OptionChain protoChain;
    List<Double> strikePrices;
    List<FusionOptionDate> optionDates = new ArrayList<>();

    public FusionOptionChain(OptionChainProto.OptionChain protoChain) {
        this.protoChain = protoChain;
        for (OptionChainProto.OptionDateChain dateChain : protoChain.getOptionDatesList()) {
            optionDates.add(new FusionOptionDate(dateChain));
        }
    }

    @Override
    public Interfaces.StockQuote getUnderlyingStockQuote() {
        return null;
    }

    @Override
    public List<? extends Interfaces.OptionDate> getChainsByDate() {
        return null;
    }

    @Override
    public List<DateTime> getExpirationDates() {
        return Collections.EMPTY_LIST;
    }

    @Override
    public List<Double> getStrikePrices() {
        if (strikePrices == null) {

            Set<Double> strikes = new HashSet<>();
            OptionChainProto.OptionDateChain firstDate = protoChain.getOptionDates(0);
            OptionChainProto.OptionDateChain lastDate = protoChain.getOptionDates(protoChain.getOptionDatesCount() - 1);

            double lo = firstDate.getOptions(0).getStrike();
            double hi = firstDate.getOptions(firstDate.getOptionsCount() - 1).getStrike();
            lo = Math.min(lo, lastDate.getOptions(0).getStrike());
            hi = Math.max(hi, lastDate.getOptions(lastDate.getOptionsCount() - 1).getStrike());

            strikePrices = com.optionfusion.util.Util.getStrikeTicks(lo, hi);
        }
        return strikePrices;
    }

    @Override
    public List<? extends Interfaces.OptionQuote> getOptionCalls() {
        return null;
    }

    @Override
    public List<? extends Interfaces.OptionQuote> getOptionPuts() {
        return null;
    }

    @Override
    public List<Spread> getAllSpreads(FilterSet filterSet) {
        return null;
    }

    @Override
    public String toJson(Gson gson) {
        return null;
    }

    @Override
    public OptionFusionApplication.Provider getProvider() {
        return null;
    }

    @Override
    public long getLastUpdatedTimestamp() {
        return 0;
    }

    @Override
    public boolean succeeded() {
        return false;
    }

    @Override
    public String getError() {
        return null;
    }
}
