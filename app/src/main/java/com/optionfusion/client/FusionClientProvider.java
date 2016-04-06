package com.optionfusion.client;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.cache.StockQuoteProvider;
import com.optionfusion.db.DbHelper;
import com.optionfusion.util.Constants;
import com.optionfusion.util.SharedPrefStore;

import javax.inject.Inject;

public class FusionClientProvider extends ClientProvider implements ClientProvider.SymbolLookupClientProvider, ClientProvider.OptionChainClientProvider, ClientProvider.AccountClientProvider, ClientProvider.StockQuoteClientProvider {

    FusionClient client;
    Context context;
    private final SharedPrefStore sharedPrefStore;

    private static final String TAG = "FusionClientProvider";

    public FusionClientProvider(Context context, SharedPrefStore sharedPrefStore) {
        this.context = context;
        this.sharedPrefStore = sharedPrefStore;
    }

    @Override
    public ClientInterfaces.SymbolLookupClient getSymbolLookupClient() {
        return getClient();
    }

    @Override
    public ClientInterfaces.OptionChainClient getOptionChainClient() {
        return getClient();
    }

    @Override
    public ClientInterfaces.AccountClient getAccountClient() {
        return getClient();
    }

    @Override
    public ClientInterfaces.StockQuoteClient getStockQuoteClient() {
        return getClient();
    }

    private FusionClient getClient() {
        if (client == null)
            client = new FusionClient(context);
        return client;
    }
}
