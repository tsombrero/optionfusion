package com.optionfusion.backend.models;

import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;

@Entity
public class StockQuote {
    @Id long timestamp;

    @Parent
    @Load
    Ref<Equity> equity;

    long volume;
    double bid, ask, last;
    double change;
    double changePercent;
}
