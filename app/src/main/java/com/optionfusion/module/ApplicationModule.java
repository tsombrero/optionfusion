package com.optionfusion.module;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.Nullable;

import com.birbit.android.jobqueue.Job;
import com.birbit.android.jobqueue.JobManager;
import com.birbit.android.jobqueue.config.Configuration;
import com.birbit.android.jobqueue.di.DependencyInjector;
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
import com.optionfusion.jobqueue.GetStockQuotesJob;

import net.danlew.android.joda.JodaTimeAndroid;

import org.greenrobot.eventbus.EventBus;
import org.joda.time.DateTime;

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
    ClientInterfaces.OptionChainClient provideOptionChainClient(Context context, AmeritradeClientProvider ameritradeClientProvider, FusionClientProvider fusionClientProvider) {
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
    ClientInterfaces.StockQuoteClient provideStockQuoteClient(Context context, YhooClientClientProvider yhooClientProvider, FusionClientProvider fusionClientProvider) {
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
    ClientInterfaces.SymbolLookupClient provideSymbolLookupClient(Context context, FusionClientProvider fusionClientProvider) {
        switch (OptionFusionApplication.from(context).getBackendProvider()) {
            case AMERITRADE:
                //TODO
            default:
                return fusionClientProvider.getSymbolLookupClient();
        }
    }

    @Provides
    ClientInterfaces.AccountClient provideAccountClient(FusionClientProvider fusionClientProvider) {
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
                .registerTypeAdapter(DateTime.class, new DateTimeSerializer())
                .registerTypeAdapter(DateTime.class, new DateTimeDeserializer())
                .create();
    }

    private class DateTimeSerializer implements JsonSerializer<DateTime> {
        public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.getMillis());
        }
    }

    private class DateTimeDeserializer implements JsonDeserializer<DateTime> {
        public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            return new DateTime(json.getAsJsonPrimitive().getAsLong());
        }
    }

    @Provides
    @Singleton
    SharedPreferences provideSharedPreferences(Context context) {
        return context.getSharedPreferences("default", Context.MODE_PRIVATE);
    }

    @Provides
    @Singleton
    StockQuoteProvider provideStockQuoteProvider(Context context, ClientInterfaces.StockQuoteClient stockQuoteClient, EventBus bus, JobManager jobManager) {
        return new StockQuoteProvider(context, stockQuoteClient, bus, jobManager);
    }

    @Provides
    @Singleton
    DbHelper provideDbHelper(Context context) {
        String path = context.getDatabasePath(Schema.DB_NAME).getPath();
        return new DbHelper(context, path, null, Schema.SCHEMA_VERSION);
    }

    @Provides
    @Singleton
    EventBus provideEventBus() {
        return new EventBus();
    }

    @Provides
    @Singleton
    JobManager provideJobManager(final Context context) {
        return new JobManager(new Configuration.Builder(context).injector(new DependencyInjector() {
            @Override
            public void inject(Job job) {
                OptionFusionApplication.from(context).getComponent().inject(job);
            }
        })
        .build());
    }

}
