package com.optionfusion.backend.models;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;
import com.optionfusion.backend.protobuf.OptionChainProto;

import java.util.Date;

@Entity
public class OptionChain {

    @Id
    Long id;

    @Index
    String symbol;

    @Index
    Date quote_timestamp;

    @Serialize(zip = true)
    Blob chainData;

    public OptionChain() {
    }

    public OptionChain(OptionChainProto.OptionChain protoChain) {
        quote_timestamp = new Date(protoChain.getTimestamp());
        symbol = protoChain.getStockquote().getSymbol();
        chainData = new Blob(protoChain.toByteArray());
    }

    public String getSymbol() {
        return symbol;
    }

    public Date getQuote_timestamp() {
        return quote_timestamp;
    }

    public Blob getChainData() {
        return chainData;
    }

    //
//    public OptionChainProto.OptionChain getProtoBufChain() throws InvalidProtocolBufferException {
//        return OptionChainProto.OptionChain.parseFrom(chainData.getBytes());
//    }
}
