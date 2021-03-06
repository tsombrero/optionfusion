package com.optionfusion.model.provider.backend;

import com.google.gson.Gson;
import com.optionfusion.common.protobuf.OptionChainProto;
import com.optionfusion.model.FilterSet;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.VerticalSpread;

import org.joda.time.DateTime;
import org.joda.time.Days;

import java.util.Collections;
import java.util.List;

public class FusionOptionDate implements Interfaces.OptionDate {


    private final OptionChainProto.OptionDateChain dateChain;
    private int daysToExpiration;
    private DateTime exp;

    public FusionOptionDate(OptionChainProto.OptionDateChain dateChain) {
        this.dateChain = dateChain;
    }

    @Override
    public int getDaysToExpiration() {
        if (daysToExpiration == 0) {
            daysToExpiration = Days.daysBetween(DateTime.now().toLocalDate(), exp.toLocalDate()).getDays();
        }
        return daysToExpiration;
    }

    @Override
    public List<VerticalSpread> getAllSpreads(FilterSet filterSet) {
        if (!filterSet.pass(this))
            return Collections.EMPTY_LIST;


        return Collections.EMPTY_LIST;
    }

    @Override
    public DateTime getExpirationDate() {
        if (exp == null)
            exp = new DateTime(dateChain.getExpiration());

        return exp;
    }

    @Override
    public String toJson(Gson gson) {
        return null;
    }
}
