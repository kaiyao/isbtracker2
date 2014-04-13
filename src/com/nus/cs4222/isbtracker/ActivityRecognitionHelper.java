package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.ActivityRecognitionClient;
import android.app.Activity;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

public class ActivityRecognitionHelper implements ConnectionCallbacks, OnConnectionFailedListener {

    public static final String ACTIVITY_RECOGNITION_EXTRA_SUBJECT = "ActivityRecognition";
    public static final String ACTIVITY_RECOGNITION_RESULT = "com.nus.cs4222.isbtracker.ActivityRecognitionHelper.result";

    private Context mContext;
	
    // Constants that define the activity detection interval
    public static final int MILLISECONDS_PER_SECOND = 1000;
    public static final int DETECTION_INTERVAL_SECONDS = 20;
    public static final int DETECTION_INTERVAL_MILLISECONDS =
            MILLISECONDS_PER_SECOND * DETECTION_INTERVAL_SECONDS;
    
    private int mDetectionInterval = DETECTION_INTERVAL_MILLISECONDS;
    
    // Store the PendingIntent used to send activity recognition events back to the app
    private PendingIntent mActivityRecognitionPendingIntent;
    // Store the current activity recognition client
    private ActivityRecognitionClient mActivityRecognitionClient;
    // Flag that indicates if a request is underway. (prevent race conditions)
    private boolean mInProgress;
    
    public enum REQUEST_TYPE {START, STOP}
    private REQUEST_TYPE mRequestType;
    
    public ActivityRecognitionHelper (Context context){
    	
    	mContext = context;

    	/*
         * Instantiate a new activity recognition client. Since the
         * parent Activity implements the connection listener and
         * connection failure listener, the constructor uses "this"
         * to specify the values of those parameters.
         */
        mActivityRecognitionClient =
                new ActivityRecognitionClient(mContext, this, this);
        /*
         * Create the PendingIntent that Location Services uses
         * to send activity recognition updates back to this app.
         */
        Intent intent = new Intent(
                mContext, ActivityRecognitionIntentService.class);
        /*
         * Return a PendingIntent that starts the IntentService.
         */
        mActivityRecognitionPendingIntent =
                PendingIntent.getService(mContext, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        
        // Set request in progress state flag to false
        mInProgress = false;
    }

    private boolean servicesConnected() {
    	// Check that Google Play services is available
    	int resultCode =
    			GooglePlayServicesUtil.
    			isGooglePlayServicesAvailable(mContext);
    	// If Google Play services is available
    	if (ConnectionResult.SUCCESS == resultCode) {
    		// In debug mode, log the status
    		Log.d("Activity Recognition",
    				"Google Play services is available.");
    		// Continue
    		return true;
    		// Google Play services was not available for some reason
    	} else {
            /*Intent intent = new Intent(ScannerService.GET_PLAY_SERVICES);
            intent.setAction("getPlayServices");
            intent.putExtra("resultCode", resultCode);
            LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);*/

    		return false;
    	}
    }

	@Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Turn off the request flag
        mInProgress = false;
        /*
         * If the error has a resolution, start a Google Play services
         * activity to resolve it.
         */
        /*if (connectionResult.hasResolution()) {
            try {
                connectionResult.startResolutionForResult(
                        mContext,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
            } catch (SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        // If no resolution is available, display an error dialog
        } else {
            // Get the error code
            int errorCode = connectionResult.getErrorCode();
            // Get the error dialog from Google Play services
            Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
                    errorCode,
                    mContext,
                    CONNECTION_FAILURE_RESOLUTION_REQUEST);
            // If Google Play services can provide an error dialog
            if (errorDialog != null) {
                // Create a new DialogFragment for the error dialog
                ErrorDialogFragment errorFragment =
                        new ErrorDialogFragment();
                // Set the dialog in the DialogFragment
                errorFragment.setDialog(errorDialog);
                // Show the error dialog in the DialogFragment
                errorFragment.show(
                        mContext.getSupportFragmentManager(),
                        "Activity Recognition");
            }
        }*/
    }

	@Override
	public void onConnected(Bundle dataBundle) {
		switch (mRequestType) {
		case START :
			/*
			 * Request activity recognition updates using the
			 * preset detection interval and PendingIntent.
			 * This call is synchronous.
			 */
			mActivityRecognitionClient.requestActivityUpdates(
					mDetectionInterval,
					mActivityRecognitionPendingIntent);
			break;
		case STOP :
			mActivityRecognitionClient.removeActivityUpdates(
					mActivityRecognitionPendingIntent);
			break;
		}
		
        /*
         * Since the preceding call is synchronous, turn off the
         * in progress flag and disconnect the client
         */
        mInProgress = false;
        mActivityRecognitionClient.disconnect();
    }

	@Override
	public void onDisconnected() {
        // Turn off the request flag
        mInProgress = false;
        // Delete the client
        mActivityRecognitionClient = null;
    }
	
	/**
     * Respond by requesting activity recognition updates.
     * Note that startUpdates can be called repeatedly with a different detection interval
     * to change the detection interval
     */
    public void startUpdates(int detectionIntervalMilliseconds) {
    	
    	mDetectionInterval = detectionIntervalMilliseconds;
    	
    	mRequestType = REQUEST_TYPE.START;
    	
    	// Check for Google Play services

        if (!servicesConnected()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
        //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }
    


    /**
     * Respond by canceling updates.
     */
    public void stopUpdates() {
    	mRequestType = REQUEST_TYPE.STOP;
    	if (!servicesConnected()) {
            return;
        }
        // If a request is not already underway
        if (!mInProgress) {
            // Indicate that a request is in progress
            mInProgress = true;
            // Request a connection to Location Services
            mActivityRecognitionClient.connect();
        //
        } else {
            /*
             * A request is already underway. You can handle
             * this situation by disconnecting the client,
             * re-setting the flag, and then re-trying the
             * request.
             */
        }
    }
}
