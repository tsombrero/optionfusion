package com.mosoft.optionfusion.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.mosoft.optionfusion.model.provider.yhoo.YhooStockQuote;
import com.squareup.okhttp.OkHttpClient;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit.GsonConverterFactory;
import retrofit.Retrofit;

public class YhooClientClientProvider extends ClientProvider implements ClientProvider.StockQuoteClientProvider {

    private ClientInterfaces.StockQuoteClient client;

    // Needed bacause quotes may come back as a single object or as a json array. We want a list in both cases.
    private static class QuoteDataTypeAdapter implements JsonDeserializer<List<YhooStockQuote.QuoteData>> {
        @Override
        public List<YhooStockQuote.QuoteData> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) {
            List<YhooStockQuote.QuoteData> vals = new ArrayList<>();
            if (json.isJsonArray()) {
                for (JsonElement e : json.getAsJsonArray()) {
                    vals.add((YhooStockQuote.QuoteData) ctx.deserialize(e, YhooStockQuote.QuoteData.class));
                }
            } else if (json.isJsonObject()) {
                vals.add((YhooStockQuote.QuoteData) ctx.deserialize(json, YhooStockQuote.QuoteData.class));
            } else {
                throw new RuntimeException("Unexpected JSON type: " + json.getClass());
            }
            return vals;
        }
    }

    private static class DoubleTypeAdapter implements JsonDeserializer<Double> {

        @Override
        public Double deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return json.getAsDouble();
            } catch (Throwable t) {
                return Double.MIN_VALUE;
            }
        }
    }

    private static final Type quoteDataListType = new TypeToken<List<YhooStockQuote.QuoteData>>() {}.getType();

    Gson gson = new GsonBuilder()
            .registerTypeAdapter(Double.class, new DoubleTypeAdapter())
            .registerTypeAdapter(quoteDataListType, new QuoteDataTypeAdapter())
            .create();


    @Override
    public ClientInterfaces.StockQuoteClient getStockQuoteClient() {
        if (client == null) {
            OkHttpClient okHttpClient = new OkHttpClient();
            okHttpClient.interceptors().add(new LoggingInterceptor());

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl("http://query.yahooapis.com/v1/public/")
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .client(okHttpClient)
                    .build();

            client = new YhooClient(retrofit.create(YhooClient.RestInterface.class));
        }

        return client;
    }
}
