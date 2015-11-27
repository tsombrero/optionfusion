package com.mosoft.optionfusion.client;

import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.model.provider.Interfaces.OptionChain;

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
        void getOptionChain(String symbol, Callback<OptionChain> callback);

    }

    public interface BrokerageClient {
        void logIn(String userId, String password, Callback<LoginResponse> callback);

        void getAccounts(Callback<List<? extends Interfaces.Account>> callback);

        boolean isAuthenticated();
    }

    public interface StockQuoteClient {
        Interfaces.StockQuote getStockQuote(String symbol, Callback<Interfaces.StockQuote> callback);
    }
}
