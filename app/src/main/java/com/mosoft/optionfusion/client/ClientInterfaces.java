package com.mosoft.optionfusion.client;

import android.database.Cursor;
import android.database.CursorWrapper;
import android.database.MatrixCursor;

import com.mosoft.optionfusion.model.provider.Interfaces;
import com.mosoft.optionfusion.model.provider.Interfaces.OptionChain;

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
        void getOptionChain(String symbol, Callback<OptionChain> callback);
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

    public interface SymbolLookupClient {
        enum SuggestionColumns {
            _id, symbol, description;

            static String[] names = new String[]{_id.name(), symbol.name(), description.name()};

            public static String[] getNames() {
                return names;
            }
        }

        Cursor EMPTY_CURSOR = new CursorWrapper(new MatrixCursor(SuggestionColumns.getNames()));

        Cursor getSymbolsMatching(String query);
    }

    public interface PriceHistoryClient {
        void getPriceHistory(String symbol, Date start, Callback<Interfaces.StockPriceHistory> callback);
    }
}
