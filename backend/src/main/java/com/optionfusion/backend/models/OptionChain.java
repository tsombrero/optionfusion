package com.optionfusion.backend.models;

import com.google.appengine.api.datastore.Blob;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.optionfusion.backend.protobuf.OptionChainProto;

import java.util.Date;

@Entity
public class OptionChain {

    @Id
    Long id;

    @Index
    String symbol;

    @Index
    Date timestamp;

    Blob data;

    public OptionChain() {
    }

    public OptionChain(OptionChainProto.OptionChain protoChain) {
        timestamp = new Date(protoChain.getTimestamp());
        symbol = protoChain.getStockquote().getSymbol();
        data = new Blob(protoChain.toByteArray());
    }
}
