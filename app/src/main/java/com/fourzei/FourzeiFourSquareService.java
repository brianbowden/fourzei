package com.fourzei;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class FourzeiFourSquareService extends Service {

    private static final String TAG = "FourzeiFourSquareService";
    private final Context CONTEXT = this;

    private Binder mBinder = new LocalBinder();
    private Handler mHandler = new Handler();
    private Runnable mTimeoutTask;

    private LocationManager mLocationManager;
    private LocationListener mLocationListener;

    @Override
    public void onCreate() {
        super.onCreate();

        // Start checking location once every half hour (inexact)
        setAlarm(this);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location l) {
                long time = Utils.getDataLong(CONTEXT, FourzeiArtService.STORE_TIME);
                float acc = Utils.getDataFlt(CONTEXT, FourzeiArtService.STORE_ACC);

                if (l != null && l.getLatitude() != 0 && l.getLongitude() != 0
                        && (l.getTime() - time > FourzeiArtService.LOCATION_UPDATE_TIME_THRESHOLD || acc < l.getAccuracy())) {

                    Utils.setData(CONTEXT, FourzeiArtService.STORE_LL, String.format("%s,%s", l.getLatitude(), l.getLongitude()));
                    Utils.setDataFlt(CONTEXT, FourzeiArtService.STORE_ALT, (float) l.getAltitude());
                    Utils.setDataFlt(CONTEXT, FourzeiArtService.STORE_ACC, l.getAccuracy());
                    Utils.setDataLong(CONTEXT, FourzeiArtService.STORE_TIME, l.getTime());
                    Log.d(TAG, "LOCATION HAS BEEN PULLED AND STORED FOR FUTURE CALLS");
                } else {
                    Log.d(TAG, "LOCATION HAS BEEN PULLED AND REJECTED");
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        for (String provider : mLocationManager.getProviders(true)) {
            mLocationManager.requestLocationUpdates(provider, 5, 0, mLocationListener);
        }

        mTimeoutTask = new Runnable() {
            @Override
            public void run() {
                // Kill the service after 5 seconds
                Log.e(TAG, "KILLING LOCATION CHECK");
                mLocationManager.removeUpdates(mLocationListener);
                stopSelf();
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "Received start id " + startId + ": " + intent);

        // Reset the timeout
        mHandler.removeCallbacks(mTimeoutTask);
        mHandler.postDelayed(mTimeoutTask, 6000);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "DESTROYING LOCATION CHECK SERVICE");
        if (mLocationManager != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
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

    public void setAlarm(Context ctx) {
        AlarmManager alarmMgr = (AlarmManager)ctx.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(ctx, FourzeiFourSquareService.class);
        PendingIntent pi = PendingIntent.getService(ctx, 0, intent, 0);
        alarmMgr.setInexactRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis(), AlarmManager.INTERVAL_HALF_HOUR, pi);
    }
}
