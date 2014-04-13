package com.nus.cs4222.isbtracker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.location.ActivityRecognitionResult;

public class ScannerService extends Service {

    public static final String GET_PLAY_SERVICES = "get-play-services";
    public static final String UPDATE_TOPIC = "update-event";

    private static final String LOGTAG = ScannerService.class.getSimpleName();

    private final IBinder mBinder = new ScannerBinder();
    private boolean mIsBound;
    private StateMachine mStateMachine;
    private BroadcastReceiver mMessageReceiver;

    public class ScannerBinder extends Binder {
        ScannerService getService() {
            return ScannerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "onStartCommand");
        return Service.START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOGTAG, "onCreate");

        mMessageReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(LOGTAG, "Got a update request");
                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);

                if (ActivityRecognitionHelper.ACTIVITY_RECOGNITION_EXTRA_SUBJECT.equals(subject)) {
                    ActivityRecognitionResult result = intent.getParcelableExtra(
                            ActivityRecognitionHelper.ACTIVITY_RECOGNITION_RESULT);
                    mStateMachine.activityDetected(result);
                } else if (LocationHelper.LOCATION_HELPER_EXTRA_SUBJECT.equals(subject)) {
                    Location location = intent.getParcelableExtra(
                            LocationHelper.LOCATION_HELPER_LOCATION);
                    mStateMachine.locationChanged(location);
                }
            }
        };

        LocalBroadcastManager.getInstance(this).registerReceiver(mMessageReceiver,
                new IntentFilter(UPDATE_TOPIC));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(LOGTAG, "onDestroy");
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mMessageReceiver);
    }

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;
        Log.d(LOGTAG, "onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mIsBound = true;
        Log.d(LOGTAG, "onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;
        Log.d(LOGTAG, "onUnbind");
        if (mStateMachine != null && !mStateMachine.isTracking()) {
            stopSelf();
        }
        // Allow rebinding by returning true
        return true;
    }

    public void startTracking(StateMachineListener listener) {
        // XXX: Eventually we should just keep one StateMachine
        mStateMachine = new StateMachine(this);
        mStateMachine.setListener(listener);
        mStateMachine.startTracking();
    }

    public void stopTracking() {
        if (mStateMachine != null && mStateMachine.isTracking()) {
            mStateMachine.stopTracking();
            mStateMachine = null;
            if (!mIsBound) {
                stopSelf();
            }
        }
    }
}
