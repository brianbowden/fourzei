package com.fourzei;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

import fi.foyt.foursquare.api.FoursquareApi;
import fi.foyt.foursquare.api.FoursquareApiException;
import fi.foyt.foursquare.api.Result;
import fi.foyt.foursquare.api.entities.CompactVenue;
import fi.foyt.foursquare.api.entities.Photo;
import fi.foyt.foursquare.api.entities.PhotoGroup;
import fi.foyt.foursquare.api.entities.VenuesSearchResult;

public class FourzeiFourSquareService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = "FourzeiFourSquareService";
    private final Context CONTEXT = this;

    private Binder mBinder = new LocalBinder();
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private LocationClient mLocationClient;
    private Intent mIntent;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "pending");
        mIntent = intent;
        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if ("pending".equals(Utils.getData(CONTEXT, FourzeiArtService.EXTRA_STATE))) {
            Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "error");
        }
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        new FourSquareAsyncTask().execute(mLocationClient.getLastLocation());
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Disconnected!");
        Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "error");
        stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed!");
        Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "error");
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    private class FourSquareAsyncTask extends AsyncTask<Location, Void, Void> {

        @Override
        protected Void doInBackground(Location... locations) {
            if (locations == null || locations.length == 0) return null;

            Location l = locations[0];

            if (l != null) {
                FoursquareApi fsApi = new FoursquareApi(Config.FS_CLIENT_ID, Config.FS_CLIENT_SECRET, Config.FS_REDIRECT_URI);
                String latLong = String.format("%s,%s", l.getLatitude(), l.getLongitude());

                try {
                    Result<VenuesSearchResult> venueResult = fsApi.venuesSearch(latLong, (double) l.getAccuracy(),
                            l.getAltitude(), (double) l.getAccuracy(), null, 10, "browse", null, null, null, null, 1000, null);

                    if (venueResult != null && venueResult.getMeta().getCode() == 200 && venueResult.getResult().getVenues() != null
                            && venueResult.getResult().getVenues().length > 0) {

                        int venueIndex = Utils.getRandomIndex(venueResult.getResult().getVenues().length);

                        CompactVenue venue = null;
                        Result<PhotoGroup> photoGroupResult = null;

                        int tries = 0;

                        while (tries < 3 || (photoGroupResult.getResult() != null
                                && photoGroupResult.getResult().getItems().length == 0)) {
                            venue = venueResult.getResult().getVenues()[venueIndex];
                            photoGroupResult = fsApi.venuesPhotos(venue.getId(), "venue", 50, 0);
                            tries++;
                        }

                        if (photoGroupResult != null && photoGroupResult.getMeta().getCode() == 200
                                && photoGroupResult.getResult().getItems() != null) {

                            Photo[] photos = photoGroupResult.getResult().getItems();

                            if (photos.length > 0) {
                                Photo photo = photos[Utils.getRandomIndex(photos.length)];

                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "complete");
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_TITLE, venue.getName());
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_BYLINE, venue.getLocation().getAddress());
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_URI, photo.getUrl());
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_TOKEN, photo.getId());
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_VENUE_ID, venue.getId());
                            } else {
                                Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "empty");
                            }
                        }
                    }

                } catch (FoursquareApiException e) {
                    Utils.setData(CONTEXT, FourzeiArtService.EXTRA_STATE, "error");
                    Log.e(TAG, "Unable to explore Foursquare venues for cool photos. :(");
                }
            }

            return null;
        }

        @Override
        public void onPostExecute(Void result) {
            stopSelf();
        }
    }

    public class LocalBinder extends Binder {
        FourzeiFourSquareService getService() {
            return FourzeiFourSquareService.this;
        }
    }
}
