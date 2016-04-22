package com.optionfusion.client;

import android.app.Fragment;
import android.support.v4.app.FragmentActivity;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.FusionUser;
import com.optionfusion.model.provider.Interfaces;
import com.optionfusion.model.provider.Interfaces.OptionChain;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.List;


public class ClientInterfaces {

    public abstract static class Callback<T> {
        public abstract void call(T type);

        public abstract void onError(int status, String message);

        public void onFinally() {
        }
    }

    public interface LoginResponse {
        String getSessionId();

        String getUserId();

        String getLoginTime();
    }

    public interface OptionChainClient {
        Interfaces.OptionChain getOptionChain(String symbol);
    }

    public interface BrokerageClient {
        void logIn(String userId, String password, Callback<LoginResponse> callback);

        void getAccounts(Callback<List<? extends Interfaces.Account>> callback);

        boolean isAuthenticated();
    }

    public interface StockQuoteClient {
        Interfaces.StockQuote getStockQuote(String symbol, Callback<Interfaces.StockQuote> callback);
        List<Interfaces.StockQuote> getStockQuotes(Collection<String> symbols, Callback<List<Interfaces.StockQuote>> callback);
    }

    public static class SymbolLookupResult {

        public SymbolLookupResult(String symbol, String description) {
            this.symbol = symbol;
            this.description = description;
        }

        public SymbolLookupResult(Equity equity) {
            this.symbol = equity.getSymbol();
            this.description = equity.getDescription();
        }

        String symbol;
        String description;

        public String getDescription() {
            return description;
        }

        public String getSymbol() {
            return symbol;
        }
    }

    public interface SymbolLookupClient {
        List<SymbolLookupResult> getSymbolsMatching(String query);
    }

    public interface PriceHistoryClient {
        void getPriceHistory(String symbol, Date start, Callback<Interfaces.StockPriceHistory> callback);
    }

    public interface AccountClient {
        void setGoogleAccount(GoogleSignInAccount account);
        GoogleSignInResult trySilentSignIn(GoogleApiClient googleApiClient);

        FusionUser getAccountUser();
        List<Interfaces.StockQuote> setWatchlist(Collection<String> symbols) throws IOException;
        List<Interfaces.StockQuote> getWatchlist() throws IOException;

        void setUserData(String key, String value) throws IOException;
        String getUserData(String key);
    }
}
