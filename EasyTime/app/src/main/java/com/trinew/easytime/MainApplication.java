package com.trinew.easytime;

import android.app.Application;

import com.crashlytics.android.Crashlytics;
import com.digits.sdk.android.Digits;
import com.parse.Parse;
import com.parse.ParseObject;
import com.trinew.easytime.models.ParseEmployer;
import com.trinew.easytime.models.ParseStamp;

import com.twitter.sdk.android.core.TwitterAuthConfig;
import com.twitter.sdk.android.core.TwitterCore;
import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by Jonathan on 8/1/2015.
 */
public class MainApplication extends Application {

    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.
    private static final String TWITTER_KEY = "y7sVPYbw54h7aVgFR4ctGg6Kw";
    private static final String TWITTER_SECRET = "dWUFSbsHODogBVynN3bUeSpqiHaHuu1Zg2rloSqzl7KOhSuRiV";


    // Note: Your consumer key and secret should be obfuscated in your source code before shipping.

    private final static String PARSE_APP_ID = "0zOguLdTdU3vgFMmtaJQNLyk1kUm18NowKwp3ECN";
    private final static String PARSE_CLIENT_KEY = "4coRGqECUCUTlNGYxlPtZsYIVwkVfJBthZ6awr2p";

    @Override
    public void onCreate() {
        super.onCreate();
        TwitterAuthConfig authConfig = new TwitterAuthConfig(TWITTER_KEY, TWITTER_SECRET);
        Fabric.with(this, new TwitterCore(authConfig), new Digits(), new Crashlytics());

        // Twitter Digits

        // Calligraphy
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Roboto-Regular.ttf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );

        // Parse
        ParseObject.registerSubclass(ParseStamp.class);
        ParseObject.registerSubclass(ParseEmployer.class);

        Parse.enableLocalDatastore(this);

        Parse.initialize(this, PARSE_APP_ID, PARSE_CLIENT_KEY);
    }
}