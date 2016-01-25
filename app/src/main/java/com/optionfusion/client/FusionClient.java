package com.optionfusion.client;

import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.optionfusion.BuildConfig;
import com.optionfusion.R;
import com.optionfusion.com.backend.optionFusion.OptionFusion;
import com.optionfusion.com.backend.optionFusion.model.Symbol;
import com.optionfusion.com.backend.optionFusion.model.SymbolCollection;
import com.optionfusion.util.Constants;

import java.io.IOException;

public class FusionClient implements ClientInterfaces.SymbolLookupClient {

    OptionFusion optionFusionApi;

    private GoogleSignInAccount account;

    Context context;

    private static final boolean SIGN_IN_REQUIRED = true;
    private static final String ROOT_URL = BuildConfig.ROOT_URL;

    public FusionClient(Context context, GoogleSignInAccount acct) {
        this.context = context;
        this.account = acct;
    }

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
    GoogleAccountCredential getRequestInitializer() {
        if (SIGN_IN_REQUIRED) {
            GoogleAccountCredential ret = GoogleAccountCredential.usingAudience(context,
                    Constants.AUDIENCE_ANDROID_CLIENT_ID);

            ret.setSelectedAccountName(account.getEmail());
            return ret;
        } else {
            return null;
//            return new HttpRequestInitializer() {
//                @Override
//                public void initialize(final HttpRequest arg0) {
//                }
//            };
        }
    }


    public GoogleSignInAccount getAccount() {
        return account;
    }

    public void setAccount(GoogleSignInAccount account) {
        this.account = account;
    }
}
