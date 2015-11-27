package com.mosoft.optionfusion.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class YhooClientClientProvider extends ClientProvider implements ClientProvider.StockQuoteClientProvider {

    ClientInterfaces.StockQuoteClient stockQuoteClient;

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
    public ClientInterfaces.StockQuoteClient getStockQuoteClient() {
        if (stockQuoteClient == null) {
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.interceptors().add(new LoggingInterceptor());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://query.yahooapis.com/v1/public/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();

            stockQuoteClient = new YhooClient(retrofit.create(YhooClient.RestInterface.class));
        }

        return stockQuoteClient;
    }
}
