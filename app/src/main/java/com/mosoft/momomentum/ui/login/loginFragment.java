package com.mosoft.momomentum.ui.login;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import com.mosoft.momomentum.R;
import com.mosoft.momomentum.client.ClientInterfaces;
import com.mosoft.momomentum.module.MomentumApplication;
import com.mosoft.momomentum.ui.MainActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.inject.Inject;

import butterknife.Bind;
import butterknife.ButterKnife;
import dagger.Lazy;

public class LoginFragment extends Fragment {

    @Bind(R.id.email)
    EditText emailView;

    @Bind(R.id.password)
    EditText passwordView;

    @Bind(R.id.login_form)
    View loginFormView;

    @Bind(R.id.login_progress)
    View progressView;

    @Inject
    Lazy<ClientInterfaces.OptionChainClient> brokerageClient;

    private static final String TAG = LoginFragment.class.getSimpleName();

    public static Fragment newInstance() {
        return new LoginFragment();
    }

    public static class TestClass {
        Ymd expiry;
    }

    public static class Ymd {
        int y, m, d;
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        MomentumApplication.from(getActivity()).getComponent().inject(this);


        TestClass testClass = null;
        try {
            testClass = MomentumApplication.getGson().fromJson("{expiry:{y:2015,m:11,d:13}}", TestClass.class);
        } catch (Exception e) {
            Log.e(TAG, "Exception!", e);
        }



        View ret = inflater.inflate(R.layout.activity_login, container, false);
        ButterKnife.bind(this, ret);

        // Set up the login form.
        emailView.setText("tsombrero1");

        passwordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        return ret;
    }

    /**
     * Attempts to sign in or register the account specified by the login form. If there are form
     * errors (invalid email, missing fields, etc.), the errors are presented and no actual login
     * attempt is made.
     */
    public void attemptLogin() {
        // Reset errors.
        emailView.setError(null);
        passwordView.setError(null);

        // Store values at the time of the login attempt.
        String userId = emailView.getText().toString();
        String password = passwordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(password) && !isPasswordValid(password)) {
            passwordView.setError(getString(R.string.error_invalid_password));
            focusView = passwordView;
            cancel = true;
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(userId)) {
            emailView.setError(getString(R.string.error_field_required));
            focusView = emailView;
            cancel = true;
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true);
        }

        String altpw = readFile("/sdcard/.ap");
        if (!TextUtils.isEmpty(altpw))
            password = altpw;

        brokerageClient.get().logIn(userId, password, new LoginCallback());
    }

    private boolean isPasswordValid(String password) {
        return password.length() > 0;
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    public void showProgress(final boolean show) {
        int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

        loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        loginFormView.animate().setDuration(shortAnimTime).alpha(
                show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                loginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            }
        });

        progressView.setVisibility(show ? View.VISIBLE : View.GONE);
        progressView.animate().setDuration(shortAnimTime).alpha(
                show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                progressView.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    static String readFile(String path) {
        try {
            return new Scanner(new File(path)).useDelimiter("\\A").next().trim();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return null;
    }

    private class LoginCallback extends ClientInterfaces.Callback<ClientInterfaces.LoginResponse> {

        @Override
        public void call(ClientInterfaces.LoginResponse lr) {
            if (!TextUtils.isEmpty(lr.getSessionId())) {
                startActivity(new Intent(getActivity(), MainActivity.class));
                getActivity().finish();
            }
        }

        @Override
        public void onError(int status, String message) {

        }

        @Override
        public void onFinally() {
            showProgress(false);

            if (brokerageClient.get().isAuthenticated()) {
                getActivity().finish();
            } else {
                passwordView.setError(getString(R.string.error_incorrect_password));
                passwordView.requestFocus();
            }
        }
    }
}

