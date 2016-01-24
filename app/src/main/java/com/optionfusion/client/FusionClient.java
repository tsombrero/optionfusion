package com.optionfusion.client;

import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.services.AbstractGoogleClientRequest;
import com.google.api.client.googleapis.services.GoogleClientRequestInitializer;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.optionfusion.BuildConfig;
import com.optionfusion.com.backend.optionFusion.OptionFusion;
import com.optionfusion.com.backend.optionFusion.model.Symbol;
import com.optionfusion.com.backend.optionFusion.model.SymbolCollection;

import java.io.IOException;

public class FusionClient implements ClientInterfaces.SymbolLookupClient {

    OptionFusion optionFusionApi;

    private static final boolean SIGN_IN_REQUIRED = true;
    private static final String ROOT_URL = BuildConfig.ROOT_URL;

    @Override
    public Cursor getSymbolsMatching(String query) {
        try {
            SymbolCollection matches = getEndpoints().symbolLookup().getMatching(query).execute();

            if (matches == null)
                return ClientInterfaces.SymbolLookupClient.EMPTY_CURSOR;

            MatrixCursor ret = new MatrixCursor(SuggestionColumns.getNames());

            for (Symbol match : matches.getItems()) {
                ret.newRow()
                        .add(SuggestionColumns._id.name(), 0)
                        .add(SuggestionColumns.symbol.name(), match.getSymbol())
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
                    .setRootUrl(ROOT_URL)
                    .setGoogleClientRequestInitializer(
                            new GoogleClientRequestInitializer() {
                                @Override
                                public void initialize(
                                        final AbstractGoogleClientRequest<?>
                                                abstractGoogleClientRequest)
                                        throws IOException {
                                    abstractGoogleClientRequest
                                            .setDisableGZipContent(true);
                                }
                            }
                    );

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
    static HttpRequestInitializer getRequestInitializer() {
        if (SIGN_IN_REQUIRED) {
            return null;
//            return SignInActivity.getCredential();
        } else {
            return new HttpRequestInitializer() {
                @Override
                public void initialize(final HttpRequest arg0) {
                }
            };
        }
    }
}
