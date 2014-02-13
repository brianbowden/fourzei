package com.fourzei;


import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.Date;

public class FourzeiArtService extends RemoteMuzeiArtSource {

    public static final String EXTRA_STATE = "STATE";
    public static final String EXTRA_TITLE = "TITLE";
    public static final String EXTRA_BYLINE = "BYLINE";
    public static final String EXTRA_URI = "URI";
    public static final String EXTRA_TOKEN = "TOKEN";
    public static final String EXTRA_VENUE_ID = "VID";

    private static final String SOURCE_NAME = "FourzeiArtSource";
    private static final String TAG = "FourzeiArtSource";
    private static final int NEW_VENUE_THRESHOLD = 300000;
    private static final int TEAM_ID = 11;
    private static final int ROTATE_TIME_MILLIS = 1 * 60 * 60 * 1000; // rotate every hour
    private static final int POLL_TIME_MILLIS = 1000; // poll once a second

    public final Context CONTEXT = this;

    private boolean mGoToNextVenue;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public FourzeiArtService() {
        super(SOURCE_NAME);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);
    }

    @Override
    public void onHandleIntent(Intent intent) {
        super.onHandleIntent(intent);
    }

    @Override
    public void onTryUpdate(final int reason) throws RetryException {

        String state = Utils.getData(CONTEXT, EXTRA_STATE);

        if (state.equals("pending")) {



        } else if (state.equals("complete")) {
            publishArtwork(new Artwork.Builder()
                    .title(Utils.getData(CONTEXT, EXTRA_TITLE))
                    .byline(Utils.getData(CONTEXT, EXTRA_BYLINE))
                    .imageUri(Uri.parse(Utils.getData(CONTEXT, EXTRA_URI)))
                    .token(Utils.getData(CONTEXT, EXTRA_TOKEN))
                    .viewIntent(new Intent("http://foursquare.com/venue/" + Utils.getData(CONTEXT, EXTRA_VENUE_ID)))
                    .build());
            Utils.setData(CONTEXT, EXTRA_STATE, "published");
            scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);

        } else {

            if (reason == UPDATE_REASON_USER_NEXT) {
                long lastRequest = Utils.getLastRequest(CONTEXT);
                if (lastRequest > -1 && new Date().getTime() - lastRequest < NEW_VENUE_THRESHOLD) {
                    mGoToNextVenue = true;
                }

                Utils.setLastRequest(CONTEXT, new Date().getTime());
            }

            startService(new Intent(CONTEXT, FourzeiFourSquareService.class));

        }
    }
}
