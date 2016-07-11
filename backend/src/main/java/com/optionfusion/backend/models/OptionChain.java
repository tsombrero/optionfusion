package com.optionfusion.backend.models;

import com.google.api.server.spi.config.AnnotationBoolean;
import com.google.api.server.spi.config.ApiResourceProperty;
import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.annotation.Serialize;
import com.optionfusion.common.protobuf.OptionChainProto;

import java.util.Date;

@Entity
@Cache
public class OptionChain {

    @Id
    long timestamp;

    @Parent
    @ApiResourceProperty(ignored = AnnotationBoolean.TRUE)
    Key<Equity> equity;

    String symbol;

    @Serialize(zip = true)
    Blob chainData;

    public OptionChain() {
    }

    public OptionChain(OptionChainProto.OptionChain protoChain) {
        setEquity(protoChain.getSymbol());
        timestamp = protoChain.getTimestamp();
        chainData = new Blob(protoChain.toByteArray());
    }

    public String getSymbol() {
        return symbol;
    }

    public Date getQuote_timestamp() {
        return new Date(timestamp);
    }

    public Blob getChainData() {
        return chainData;
    }

    public void setEquity(String ticker) {
        this.symbol = ticker;
        equity = Key.create(Equity.class, ticker);
    }
}
