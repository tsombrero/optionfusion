package com.optionfusion.ui.login;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.optionfusion.R;
import com.optionfusion.module.OptionFusionApplication;
import com.optionfusion.ui.MainActivity;
import com.optionfusion.util.Constants;

import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import butterknife.ButterKnife;


public class GoogleLoginFragment extends Fragment {

    /**
     * Activity that allows the user to select the account they want to use to sign
     * in. The class also implements integration with Google Play Services and
     * Google Accounts.
     */

    private static final String TAG = "GoogleLoginFragment";

    /**
     * Name of the key for the shared preferences to access the current signed in account.
     */
    private static final String ACCOUNT_NAME_SETTING_NAME = "accountName";

    /**
     * Constant for startActivityForResult flow.
     */
    private static final int REQUEST_ACCOUNT_PICKER = 1;

    /**
     * Constant for startActivityForResult flow.
     */
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 2;

    /**
     * Google Account credentials manager.
     */
    private static GoogleAccountCredential credential;

    /**
     * @return the google account credential manager.
     */
    public static GoogleAccountCredential getCredential() {
        return credential;
    }

    /**
     * Called to sign out the user, so user can later on select a different account.
     *
     * @param activity activity that initiated the sign out.
     */
    static void onSignOut(final Activity activity) {
        SharedPreferences settings = activity
                .getSharedPreferences("MobileAssistant", 0);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_NAME_SETTING_NAME, "");

        editor.apply();
        credential.setSelectedAccountName("");

        Intent intent = new Intent(activity, MainActivity.class);
        activity.startActivity(intent);
    }

    /**
     * Initializes the activity content and then navigates to the MainActivity if the user is
     * already signed in or if the app is configured to not require the sign in. Otherwise it
     * initiates starting the UI for the account selection and a check for Google Play Services
     * being up to date.
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        OptionFusionApplication.from(getActivity()).getComponent().inject(this);

        View ret = inflater.inflate(R.layout.google_login, container, false);
        ButterKnife.bind(this, ret);


        if (!checkPlayServices()) {
            // Google Play Services are required, so don't proceed until they
            // are installed.
            return null;
        }

        if (isSignedIn()) {
            startMainActivity();
        } else {
            startActivityForResult(credential.newChooseAccountIntent(),
                    REQUEST_ACCOUNT_PICKER);
        }

        return ret;
    }

    /**
     * Handles the results from activities launched to select an account and to install Google Play
     * Services.
     */
    @Override
    public final void onActivityResult(final int requestCode,
                                          final int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode != Activity.RESULT_OK) {
                    checkPlayServices();
                }
                break;
            case REQUEST_ACCOUNT_PICKER:
            default:
                if (data != null && data.getExtras() != null) {
                    String accountName = data.getExtras()
                            .getString(AccountManager.KEY_ACCOUNT_NAME);
                    if (accountName != null) {
                        onSignedIn(accountName);
                        return;
                    }
                }
                // Signing in is required so display the dialog again
                startActivityForResult(credential.newChooseAccountIntent(),
                        REQUEST_ACCOUNT_PICKER);
                break;
        }
    }

    /**
     * Retrieves the previously used account name from the application preferences and checks if
     * the credential object can be set to this account.
     *
     * @return a boolean indicating if the user is signed in or not
     */
    private boolean isSignedIn() {
        credential = GoogleAccountCredential.usingAudience(getActivity(),
                Constants.AUDIENCE_ANDROID_CLIENT_ID);
        SharedPreferences settings = getActivity().getSharedPreferences("MobileAssistant", 0);
        String accountName = settings
                .getString(ACCOUNT_NAME_SETTING_NAME, null);
        credential.setSelectedAccountName(accountName);
        return credential.getSelectedAccount() != null;
    }

    /**
     * Called when the user selected an account. The account name is stored in the application
     * preferences and set in the credential object.
     *
     * @param accountName the account that the user selected.
     */
    private void onSignedIn(final String accountName) {
        SharedPreferences settings = getActivity().getSharedPreferences("MobileAssistant", 0);

        credential.setSelectedAccountName(accountName);

        SharedPreferences.Editor editor = settings.edit();
        editor.putString(ACCOUNT_NAME_SETTING_NAME, accountName);
        editor.apply();

        startMainActivity();
    }

    /**
     * Registers the device with GCM if necessary, and then navigates to the MainActivity.
     */
    private void startMainActivity() {
        Intent intent = new Intent(getActivity(), MainActivity.class);
        startActivity(intent);
    }

    @Override
    public final void onResume() {
        super.onResume();
        if (Constants.SIGN_IN_REQUIRED) {
            // As per GooglePlayServices documentation, an application needs to
            // check from within onResume if Google Play Services is available.
            checkPlayServices();
        }
    }

    /**
     * Checks if Google Play Services are installed and if not it initializes opening the dialog to
     * allow user to install Google Play Services.
     *
     * @return a boolean indicating if the Google Play Services are available.
     */
    private boolean checkPlayServices() {
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getActivity());
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GoogleApiAvailability.getInstance().isUserResolvableError(resultCode)) {
                GoogleApiAvailability.getInstance()
                        .getErrorDialog(getActivity(), resultCode, REQUEST_GOOGLE_PLAY_SERVICES)
                        .show();
            } else {
                Log.i(TAG, "This device is not supported.");
                getActivity().finish();
            }
            return false;
        }
        return true;
    }
}

