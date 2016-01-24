package com.optionfusion.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class GoogClientProvider extends ClientProvider implements ClientProvider.OptionChainClientProvider, ClientProvider.SymbolLookupClientProvider {

    ClientInterfaces.StockQuoteClient stockQuoteClient;

    GoogClient optionChainClient;

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(Double.class, new TypeAdapter<Double>() {
                @Override
                public void write(JsonWriter out, Double value) throws IOException {
                    out.value(value);
                }

                @Override
                public Double read(JsonReader in) throws IOException {
                    try {
                        return Double.parseDouble(in.nextString());
                    } catch (Exception e) {
                        return Double.MIN_VALUE;
                    }
                }
            })
            .create();


    public GoogClientProvider(ClientInterfaces.StockQuoteClient stockQuoteClient) {
        this.stockQuoteClient = stockQuoteClient;
    }

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient() {
        return getDefaultClient();
    }

    private GoogClient getDefaultClient() {
        if (optionChainClient == null) {
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.interceptors().add(new LoggingInterceptor());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("https://www.google.com/finance/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();

            optionChainClient = new GoogClient(retrofit.create(GoogClient.RestInterface.class), stockQuoteClient);
        }
        return optionChainClient;
    }

    public ClientInterfaces.SymbolLookupClient getSymbolLookupClient() {
        return getDefaultClient();
    }
}
