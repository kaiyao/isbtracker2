package com.nus.cs4222.isbtracker;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class TrackerService extends Service {
    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Created service");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Started service");
        startLocationSampling();
        return Service.START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed");
        stopLocationSampling();
    }

    /** Helper method that starts Location sampling. */
    public void startLocationSampling() {
    }

    /**
     * Helper method that stops Location sampling.
     */
    public void stopLocationSampling() {
    }

    public class TrackerServiceBinder extends Binder {
        TrackerService getService() {
            return TrackerService.this;
        }
    }

    private final IBinder mBinder = new TrackerServiceBinder();

    /** DDMS Log Tag. */
    private static final String TAG = "TrackerService";
}
