package com.optionfusion.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.optionfusion.cache.OptionChainProvider;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.client.AmeritradeClientProvider;
import com.optionfusion.client.ClientInterfaces;
import com.optionfusion.client.FusionClientProvider;
import com.optionfusion.client.GoogClientProvider;
import com.optionfusion.client.YhooClientClientProvider;
import com.optionfusion.db.DbHelper;
import com.optionfusion.db.Schema;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.LocalDate;

import java.lang.reflect.Type;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Providers for Dagger2. There are some naming challenges. For example:
 * <p/>
 * StockQuoteProvider is a class that provides stock quotes from an im-memory cache. It's a
 * singleton. This module contains the useful but unfortunate function provideStockQuoteProvider().
 * <p/>
 * There are multiple StockQuoteClient implementations, depending on how the user is authenticated.
 * StockQuoteClients fetch stock quote data from REST services. They are singletons.
 * <p/>
 * The StockQuoteClientProvider will return the correct StockQuoteClient as needed. This module
 * contains another useful but unfortunate function provideStockQuoteClientProvider().
 * <p/>
 * I think there's an intermediate layer here that can be factored out but let's worry about that
 * later.
 */

@Module
public class ApplicationModule {

    private final Application application;

    ApplicationModule(Application application) {
        this.application = application;
        JodaTimeAndroid.init(application);
    }

    @Provides
    @Singleton
    Application application() {
        return application;
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    ClientInterfaces.OptionChainClient provideOptionChainClient(Context context, AmeritradeClientProvider ameritradeClientProvider, FusionClientProvider fusionClientProvider, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        switch (OptionFusionApplication.from(context).getBackendProvider()) {
            case AMERITRADE:
                return ameritradeClientProvider.getOptionChainClient();
        }
        return fusionClientProvider.getOptionChainClient();
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    @Nullable
    ClientInterfaces.BrokerageClient provideBrokerageClient(Context context, AmeritradeClientProvider ameritradeClientProvider) {
        switch (OptionFusionApplication.from(context).getBackendProvider()) {
            case AMERITRADE:
                return ameritradeClientProvider.getBrokerageClient();
        }
        return null;
    }

    // Note this is not a singleton because it's an abstracted provider; the underlying client providers are singletons
    @Provides
    ClientInterfaces.StockQuoteClient provideStockQuoteClient(Context context, AmeritradeClientProvider ameritradeClientProvider, YhooClientClientProvider yhooClientProvider, FusionClientProvider fusionClientProvider) {
        switch (OptionFusionApplication.from(context).getBackendProvider()) {
            case AMERITRADE:
                //TODO
            case OPTION_FUSION_BACKEND:
                return fusionClientProvider.getStockQuoteClient();
            default:
                return yhooClientProvider.getStockQuoteClient();
        }
    }

    @Provides
    ClientInterfaces.SymbolLookupClient provideSymbolLookupClient(Context context, AmeritradeClientProvider ameritradeClientProvider, GoogClientProvider googClientProvider, FusionClientProvider fusionClientProvider) {
        switch (OptionFusionApplication.from(context).getBackendProvider()) {
            case AMERITRADE:
                //TODO
            default:
                return fusionClientProvider.getSymbolLookupClient();
        }
    }

    @Provides
    @Nullable
    ClientInterfaces.AccountClient provideAccountClient(Context context, FusionClientProvider fusionClientProvider) {
        return fusionClientProvider.getAccountClient();
    }


    @Provides
    @Singleton
    AmeritradeClientProvider provideAmeritradeClientProvider() {
        return new AmeritradeClientProvider();
    }

    @Provides
    @Singleton
    FusionClientProvider provideFusionClientProvider(Context context) {
        return new FusionClientProvider(context);
    }

    @Provides
    @Singleton
    GoogClientProvider provideGoogClientProvider(ClientInterfaces.StockQuoteClient stockQuoteClient) {
        return new GoogClientProvider(stockQuoteClient);
    }

    @Provides
    @Singleton
    YhooClientClientProvider provideYhooClientProvider() {
        return new YhooClientClientProvider();
    }

    @Provides
    @Singleton
    OptionChainProvider getOptionChainProvider(Context context, ClientInterfaces.OptionChainClient client) {
        return new OptionChainProvider(context, client);
    }

    @Provides
    @Singleton
    Context provideApplicationContext() {
        return application.getApplicationContext();
    }

    @Provides
    @Singleton
    Gson provideGson() {
        return new GsonBuilder()
                .registerTypeAdapter(LocalDate.class, new DateTimeSerializer())
                .registerTypeAdapter(LocalDate.class, new DateTimeDeserializer())
                .create();
    }

    private class DateTimeSerializer implements JsonSerializer<LocalDate> {
        public JsonElement serialize(LocalDate src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private class DateTimeDeserializer implements JsonDeserializer<LocalDate> {
        public LocalDate deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new LocalDate(json.getAsJsonPrimitive().getAsString());
        }
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Context context) {
        return context.getSharedPreferences("default", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    StockQuoteProvider provideStockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient) {
        return new StockQuoteProvider(context, stockQuoteClient);
    }

    @Provides
    @Singleton
    DbHelper provideDbHelper(Context context) {
        String path = context.getDatabasePath(Schema.DB_NAME).getPath();
        return new DbHelper(context, path, null, Schema.SCHEMA_VERSION);
    }

}
