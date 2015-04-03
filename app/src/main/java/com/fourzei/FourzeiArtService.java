package com.fourzei;


import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import java.util.ArrayList;
import java.util.List;

import retrofit.RetrofitError;

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

    private FoursquareApi.FoursquareService mVenueApi;

    public final Context CONTEXT = this;

    public FourzeiArtService() {
        super(SOURCE_NAME);
        mVenueApi = new FoursquareApi().getService();
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

        String latLong = Utils.getData(CONTEXT, STORE_LL);

        if (latLong == null && lkl != null) {
            latLong = String.format("%s,%s", lkl.getLatitude(), lkl.getLongitude());
        }

        String near = latLong == null ? "New York City, NY" : null; // initial default if nothing else works

        try {
            FoursquareApi.VenuesMetaResponse venuesResponse = mVenueApi.search(near, latLong);

            if (venuesResponse != null && venuesResponse.getResponse() != null
                    && venuesResponse.getResponse().getVenues() != null &&
                    venuesResponse.getResponse().getVenues().size() > 0) {

                List<FoursquareApi.Venue> venues = venuesResponse.getResponse().getVenues();

                List<FoursquareApi.Photo> photos = new ArrayList<>();
                int tries = 0;

                while (photos.size() == 0 && tries < 10 && venues.size() > 0) {
                    tries++;

                    int venueIndex = Utils.getRandomIndex(venues.size());

                    FoursquareApi.Venue venue = venues.get(venueIndex);
                    FoursquareApi.VenuesMetaResponse photosResponse = mVenueApi.getPhotos(venue.getId());

                    if (photosResponse != null && photosResponse.getResponse() != null &&
                            photosResponse.getResponse().getPhotos() != null &&
                            photosResponse.getResponse().getPhotos().getItems() != null) {

                        photos = photosResponse.getResponse().getPhotos().getItems();

                        if (photos.size() > 0) {

                            while (photos.size() > 0) {

                                FoursquareApi.Photo photo = photos.get(Utils.getRandomIndex(photos.size()));

                                if (photo != null && photo.getPrefix() != null && photo.getSuffix() != null &&
                                        photo.getId() != null &&
                                        !photo.getId().equals(Utils.getData(CONTEXT, STORE_CURRENT_PHOTO))) {

                                    Log.d(TAG, "Try #" + tries + ": Found a photo for " + venue.getName());
                                    Utils.setData(CONTEXT, STORE_CURRENT_PHOTO, photo.getId());

                                    publishArtwork(new Artwork.Builder()
                                            .title(venue.getName())
                                            .byline(venue.getLocation().getAddress())
                                            .imageUri(Uri.parse(photo.getOriginalUrl()))
                                            .token(photo.getId())
                                            .viewIntent(new Intent(Intent.ACTION_VIEW, Uri.parse("http://foursquare.com/venue/" + venue.getId())))
                                            .build());

                                    scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
                                    return;
                                } else {
                                    Log.d(TAG, "Try #" + tries + ": There was a photo for " + venue.getName() + ", " +
                                            "but it's corrupted or already displayed");
                                }

                                photos.remove(photo);
                            }

                        } else {
                            Log.d(TAG, "Try #" + tries + ": No photos for " + venue.getName());
                            venues.remove(venueIndex);
                        }
                    } else {
                        Log.d(TAG, "Try #" + tries + ": No photo group result for " + venue.getName());
                    }

                    venues.remove(venue);
                }
            }

            Log.e(TAG, "No photos available for your location");

            try {
                publishArtwork(new Artwork.Builder()
                        .title("Sorry!")
                        .byline("No new photos were found")
                        .imageUri(Uri.parse("http://i.imgur.com/c0Pd3nk.jpg"))
                        .build());
            } catch (Exception e) {
                Log.e(TAG, "Unable to show default artwork");
            }

        } catch (RetrofitError e) {
          Log.e(TAG, "HTTP Error while trying to get cool photos. :( URL: " + e.getUrl() + " Response: " + e.getResponse().toString());

        } catch (Exception e) {
            Log.e(TAG, "Unable to explore Foursquare venues for cool photos. :(");
        }

        scheduleUpdate(System.currentTimeMillis() + ROTATE_TIME_MILLIS);
    }
}
