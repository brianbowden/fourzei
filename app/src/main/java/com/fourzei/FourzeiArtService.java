package com.fourzei;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.Photo;
import fi.foyt.foursquare.api.entities.PhotoGroup;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;

public class FourzeiArtService extends RemoteMuzeiArtSource {

    public static final String STORE_LL = "LL";
    public static final String STORE_ALT = "ALT";
    public static final String STORE_ACC = "ACC";

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

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        Location lkl = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER); // initial course location

        // Kick off the initial location request
        startService(new Intent(CONTEXT, FourzeiFourSquareService.class));

        FoursquareApi fsApi = new FoursquareApi(Config.FS_CLIENT_ID, Config.FS_CLIENT_SECRET, Config.FS_REDIRECT_URI);
        String latLong = Utils.getData(CONTEXT, STORE_LL);

        if (latLong == null && lkl != null) {
            latLong = String.format("%s,%s", lkl.getLatitude(), lkl.getLongitude());
        }

        String near = latLong == null ? "New York City, NY" : null; // initial default if nothing else works
        double alt = Utils.getDataFlt(CONTEXT, STORE_ALT);
        double acc = Utils.getDataFlt(CONTEXT, STORE_ACC);

        try {
            Result<VenuesSearchResult> venueResult = fsApi.venuesSearch(latLong, acc > -1 ? acc : null,
                    alt > -1 ? alt : null, acc > -1 ? acc : null, null, 10, "browse", null, null, null, null, 700, near);

            if (venueResult != null && venueResult.getMeta().getCode() == 200 && venueResult.getResult().getVenues() != null
                    && venueResult.getResult().getVenues().length > 0) {

                int venueIndex = Utils.getRandomIndex(venueResult.getResult().getVenues().length);

                CompactVenue venue = venueResult.getResult().getVenues()[venueIndex];;
                Result<PhotoGroup> photoGroupResult = fsApi.venuesPhotos(venue.getId(), "venue", 50, 0);

                if (photoGroupResult != null && photoGroupResult.getMeta().getCode() == 200
                        && photoGroupResult.getResult().getItems() != null) {

                    Photo[] photos = photoGroupResult.getResult().getItems();

                    if (photos.length > 0) {
                        Photo photo = photos[Utils.getRandomIndex(photos.length)];

                        publishArtwork(new Artwork.Builder()
                            .title(venue.getName())
                            .byline(venue.getLocation().getAddress())
                            .imageUri(Uri.parse(photo.getUrl()))
                            .token(photo.getId())
                            .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://foursquare.com/venue/" + venue.getId())))
                            .build());
                    } else {
                        Log.e(TAG, "No photos for " + venue.getName());
                    }
                }
            }

        } catch (FoursquareApiException e) {
            Log.e(TAG, "Unable to explore Foursquare venues for cool photos. :(");
        }

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}
