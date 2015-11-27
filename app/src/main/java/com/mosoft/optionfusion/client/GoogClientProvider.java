package com.mosoft.optionfusion.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;

import javax.inject.Inject;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class GoogClientProvider extends ClientProvider implements ClientProvider.OptionChainClientProvider {

    @Inject
    ClientInterfaces.StockQuoteClient stockQuoteClient;

    ClientInterfaces.OptionChainClient optionChainClient;

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


    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient() {
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

    public ClientInterfaces.OptionChainClient getOptionChainClient(ClientInterfaces.StockQuoteClient stockQuoteClient) {
        getOptionChainClient();
        if (optionChainClient != null)
            ((GoogClient) optionChainClient).setStockQuoteClient(stockQuoteClient);

        return optionChainClient;
    }
}
