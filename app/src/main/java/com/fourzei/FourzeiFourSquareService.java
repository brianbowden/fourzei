package com.fourzei;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;

public class FourzeiFourSquareService extends Service implements
        GooglePlayServicesClient.ConnectionCallbacks,
        GooglePlayServicesClient.OnConnectionFailedListener {

    private static final String TAG = "FourzeiFourSquareService";
    private final Context CONTEXT = this;

    private Binder mBinder = new LocalBinder();
    private LocationClient mLocationClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        mLocationClient = new LocationClient(this, this, this);
        mLocationClient.connect();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mLocationClient != null) {
            mLocationClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Location l = mLocationClient.getLastLocation();
        Utils.setData(CONTEXT, FourzeiArtService.STORE_LL, String.format("%s,%s", l.getLatitude(), l.getLongitude()));
        Utils.setDataFlt(CONTEXT, FourzeiArtService.STORE_ALT, (float)l.getAltitude());
        Utils.setDataFlt(CONTEXT, FourzeiArtService.STORE_ACC, l.getAccuracy());
        stopSelf();
    }

    @Override
    public void onDisconnected() {
        Log.d(TAG, "Disconnected!");
        stopSelf();
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.e(TAG, "Connection failed!");
        stopSelf();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    public class LocalBinder extends Binder {
        FourzeiFourSquareService getService() {
            return FourzeiFourSquareService.this;
        }
    }
}
