package com.optionfusion.client;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.http.HttpExecuteInterceptor;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpUnsuccessfulResponseHandler;
import com.optionfusion.BuildConfig;
import com.optionfusion.R;
import com.optionfusion.com.backend.optionFusion.OptionFusion;
import com.optionfusion.com.backend.optionFusion.model.Equity;
import com.optionfusion.com.backend.optionFusion.model.EquityCollection;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class FusionClient implements ClientInterfaces.SymbolLookupClient {

    OptionFusion optionFusionApi;

    private GoogleSignInAccount account;

    Context context;

    private static final String ROOT_URL = BuildConfig.ROOT_URL;

    public FusionClient(Context context, GoogleSignInAccount acct) {
        this.context = context;
        this.account = acct;
    }

    @Override
    public List<ClientInterfaces.SymbolLookupResult> getSymbolsMatching(String query) {
        try {
            EquityCollection matches = getEndpoints().symbolLookup().getMatching(query).execute();

            if (matches == null)
                return Collections.EMPTY_LIST;

            MatrixCursor ret = new MatrixCursor(SuggestionColumns.getNames());

            for (Equity match : matches.getItems()) {
                ret.newRow()
                        .add(SuggestionColumns._id.name(), 0)
                        .add(SuggestionColumns.symbol.name(), match.getTicker())
                        .add(SuggestionColumns.description.name(), match.getDescription());
            }
            return ret;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR;
    }

    /**
     * *
     *
     * @return ShoppingAssistant endpoints to the GAE backend.
     */
    private OptionFusion getEndpoints() {

        if (optionFusionApi == null) {

            // Create API handler
            OptionFusion.Builder builder = new OptionFusion.Builder(
                    AndroidHttp.newCompatibleTransport(),
                    new AndroidJsonFactory(), getRequestInitializer())
                    .setApplicationName(context.getString(R.string.app_name))
                    .setRootUrl(ROOT_URL);

            optionFusionApi = builder.build();
        }

        return optionFusionApi;
    }

    /**
     * Returns appropriate HttpRequestInitializer depending whether the application is configured to
     * require users to be signed in or not.
     *
     * @return an appropriate HttpRequestInitializer.
     */
    HttpRequestInitializer getRequestInitializer() {
        return new HttpRequestInitializer() {
            @Override
            public void initialize(final HttpRequest request) {
                if (account != null) {
                    RequestHandler handler = new RequestHandler();
                    request.setInterceptor(handler);
                    request.setUnsuccessfulResponseHandler(handler);
                }
            }
        };
    }

    // Code borrowed from GoogleAccountCredential

    class RequestHandler implements HttpExecuteInterceptor, HttpUnsuccessfulResponseHandler {

        /**
         * Whether we've received a 401 error code indicating the token is invalid.
         */
        boolean received401;
        String token;


        public void intercept(HttpRequest request) throws IOException {
            token = account.getIdToken();
            request.getHeaders().setAuthorization("Bearer " + token);
        }

        public boolean handleResponse(
                HttpRequest request, HttpResponse response, boolean supportsRetry) {
            if (response.getStatusCode() == 401 && !received401) {
                received401 = true;
                try {
                    GoogleAuthUtil.clearToken(context, token);
                } catch (GoogleAuthException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            return false;
        }

//        /** Sleeper. */
//        private Sleeper sleeper = Sleeper.DEFAULT;
//
//        /**
//         * Back-off policy which is used when an I/O exception is thrown inside {@link #getToken} or
//         * {@code null} for none.
//         */
//        private BackOff backOff = new ExponentialBackOff();
//        public String getToken() throws IOException, GoogleAuthException {
//            while (true) {
//                try {
//                    return GoogleAuthUtil.getToken(context, account, Constants.EMAIL_SCOPE);
//                } catch (IOException e) {
//                    // network or server error, so retry using back-off policy
//                    try {
//                        if (backOff == null || !BackOffUtils.next(sleeper, backOff)) {
//                            throw e;
//                        }
//                    } catch (InterruptedException e2) {
//                        // ignore
//                    }
//                }
//            }
//        }
    }

    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
}
