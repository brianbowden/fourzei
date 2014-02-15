package com.fourzei;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
    public static final String STORE_TIME = "TIME";
    public static final String STORE_CURRENT_PHOTO = "SCP";

    public static final long LOCATION_UPDATE_TIME_THRESHOLD = 60000;

    private static final String SOURCE_NAME = "FourzeiArtSource";
    private static final String TAG = "FourzeiArtSource";
    private static final int ROTATE_TIME_MILLIS = 1 * 60 * 60 * 1000; // rotate every hour

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

                List<CompactVenue> venues = new ArrayList<CompactVenue>();
                for (CompactVenue cv : venueResult.getResult().getVenues()) {
                    venues.add(cv);
                }

                ArrayList<Photo> photos = new ArrayList<Photo>();
                int tries = 0;

                while (photos.size() == 0 && tries < 5 && venues.size() > 0) {
                    tries++;

                    int venueIndex = Utils.getRandomIndex(venues.size());

                    CompactVenue venue = venues.get(venueIndex);
                    Result<PhotoGroup> photoGroupResult = fsApi.venuesPhotos(venue.getId(), "venue", 50, 0);

                    if (photoGroupResult != null && photoGroupResult.getMeta().getCode() == 200
                            && photoGroupResult.getResult().getItems() != null) {

                        for(Photo p : photoGroupResult.getResult().getItems()) {
                            photos.add(p);
                        }

                        if (photos.size() > 0) {

                            while (photos.size() > 0) {

                                Photo photo = photos.get(Utils.getRandomIndex(photos.size()));

                                if (photo != null && photo.getUrl() != null && photo.getId() != null &&
                                        !photo.getId().equals(Utils.getData(CONTEXT, STORE_CURRENT_PHOTO))) {

                                    Log.e(TAG, "Try #" + tries + ": Found a photo for " + venue.getName());

                                    Utils.setData(CONTEXT, STORE_CURRENT_PHOTO, photo.getId());

                                    publishArtwork(new Artwork.Builder()
                                            .title(venue.getName())
                                            .byline(venue.getLocation().getAddress())
                                            .imageUri(Uri.parse(photo.getUrl()))
                                            .token(photo.getId())
                                            .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://foursquare.com/venue/" + venue.getId())))
                                            .build());

                                    return;
                                } else {
                                    Log.e(TAG, "Try #" + tries + ": There was a photo for " + venue.getName() + ", " +
                                            "but it's corrupted or already displayed");
                                }

                                photos.remove(photo);
                            }

                        } else {
                            Log.e(TAG, "Try #" + tries + ": No photos for " + venue.getName());
                        }
                    } else {
                        Log.e(TAG, "Try #" + tries + ": No photo group result for " + venue.getName());
                    }

                    venues.remove(venue);
                }
            }

            Log.e(TAG, "No photos available for your location");
            Artwork current = getCurrentArtwork();
            publishArtwork(new Artwork.Builder()
                    .title("")
                    .byline("No new photos yet!")
                    .imageUri(current.getImageUri())
                    .token(current.getToken())
                    .viewIntent(current.getViewIntent())
                    .build());

        } catch (FoursquareApiException e) {
            Log.e(TAG, "Unable to explore Foursquare venues for cool photos. :(");
        }

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}
