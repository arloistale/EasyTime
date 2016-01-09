package com.trinew.easytime.activities;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.digits.sdk.android.AuthCallback;
import com.digits.sdk.android.DigitsAuthButton;
import com.digits.sdk.android.DigitsException;
import com.digits.sdk.android.DigitsOAuthSigning;
import com.digits.sdk.android.DigitsSession;
import com.parse.FunctionCallback;
import com.parse.LogInCallback;
import com.parse.ParseCloud;
import com.parse.ParseException;
import com.parse.ParseUser;
import com.trinew.easytime.R;
import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;

import java.util.HashMap;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by jonathanlu on 9/18/15.
 */
public class LoginActivity extends AppCompatActivity {
    private final static String HEADER_SERVICE_PROVIDER = "X-Auth-Service-Provider";
    private final static String HEADER_CREDENTIALS_AUTHORIZATION = "X-Verify-Credentials-Authorization";

    private final static String KEY_DIGITS_ID = "digitsId";
    private final static String KEY_REQUEST_URL = "requestURL";
    private final static String KEY_AUTH_HEADER = "authHeader";
    private final static String KEY_PHONE_NUMBER = "phoneNumber";

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // init views
        progressBar = (ProgressBar) findViewById(R.id.loadingSpinner);

        if(ParseUser.getCurrentUser() != null) {
            onAuth();
        }

        DigitsAuthButton digitsButton = (DigitsAuthButton) findViewById(R.id.loginButton);
        digitsButton.setText("Login with mobile");
        digitsButton.setCallback(new AuthCallback() {
            @Override
            public void success(DigitsSession session, String phoneNumber) {
                TwitterAuthConfig authConfig = TwitterCore.getInstance().getAuthConfig();
                TwitterAuthToken authToken = (TwitterAuthToken) session.getAuthToken();
                DigitsOAuthSigning oauthSigning = new DigitsOAuthSigning(authConfig, authToken);

                Map<String, String> authHeaders = oauthSigning.getOAuthEchoHeadersForVerifyCredentials();

                long digitsId = session.getId();
                String url = authHeaders.get(HEADER_SERVICE_PROVIDER);
                String auth = authHeaders.get(HEADER_CREDENTIALS_AUTHORIZATION);

                Map<String, Object> params = new HashMap<>();
                params.put(KEY_DIGITS_ID, digitsId);
                params.put(KEY_REQUEST_URL, url);
                params.put(KEY_AUTH_HEADER, auth);
                params.put(KEY_PHONE_NUMBER, phoneNumber);

                // indicate we are logging in
                progressBar.setVisibility(View.VISIBLE);

                // first we get the cloud to send request to digits api to authenticate the session and get a token back
                ParseCloud.callFunctionInBackground("loginWithDigits", params, new FunctionCallback<String>() {
                    @Override
                    public void done(String sessionToken, ParseException e) {
                        if (e != null || sessionToken == null) {
                            Toast.makeText(LoginActivity.this, "There was a problem authenticating you, please try again", Toast.LENGTH_LONG).show();
                            return;
                        }

                        // now that we have a session going we authenticate to get the local session and user back
                        ParseUser.becomeInBackground(sessionToken, new LogInCallback() {
                            @Override
                            public void done(ParseUser parseUser, ParseException e) {
                                progressBar.setVisibility(View.GONE);

                                if (e != null || parseUser == null) {
                                    Toast.makeText(LoginActivity.this, "There was a problem logging in, please try again", Toast.LENGTH_LONG).show();
                                    return;
                                }

                                onAuth();
                            }
                        });
                    }
                });
            }

            @Override
            public void failure(DigitsException exception) {
                Toast.makeText(LoginActivity.this, "There was a problem logging in, please try again", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void onAuth() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);

        finish();
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
}