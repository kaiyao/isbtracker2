package com.nus.cs4222.isbtracker;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;

public class ScannerService extends Service {

    public static final String GET_PLAY_SERVICES = "get-play-services";
    public static final String UPDATE_TOPIC = "update-event";

    private static final String LOGTAG = ScannerService.class.getSimpleName();
	private static final int ONGOING_NOTIFICATION_ID = 1;

    private final IBinder mBinder = new ScannerBinder();
    private boolean mIsBound;
    private StateMachine mStateMachine;
    private StateMachineListener mStateMachineListener;
    private BroadcastReceiver mMessageReceiver;    

    public class ScannerBinder extends Binder {
        ScannerService getService() {
            return ScannerService.this;
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(LOGTAG, "onStartCommand");
        
        // Create a notification builder that's compatible with platforms >= version 4
        NotificationCompat.Builder builder =
                new NotificationCompat.Builder(getApplicationContext());

        // Set the title, text, and icon
        builder.setContentTitle(getString(R.string.app_name))
               .setContentText("App is running")
               .setSmallIcon(R.drawable.ic_notification)

               // Get the Intent that starts the Location settings panel
               //.setContentIntent(intent)
               ;
        
        startForeground(ONGOING_NOTIFICATION_ID, builder.build());
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

    public void startTracking() {
        // XXX: Eventually we should just keep one StateMachine
        mStateMachine = new StateMachine(this);
        mStateMachine.setListener(mStateMachineListener);
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

	public StateMachineListener getStateMachineListener() {
		return mStateMachineListener;
	}

	public void setStateMachineListener(StateMachineListener mStateMachineListener) {
		this.mStateMachineListener = mStateMachineListener;
		
		if (mStateMachine != null) {
			mStateMachine.setListener(mStateMachineListener);
		}
	}
}
