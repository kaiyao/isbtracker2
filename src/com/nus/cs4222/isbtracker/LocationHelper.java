package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

public class LocationHelper implements
LocationListener,
GooglePlayServicesClient.ConnectionCallbacks,
GooglePlayServicesClient.OnConnectionFailedListener {

    public static final String LOCATION_HELPER_EXTRA_SUBJECT = "LocationHelper";
    public static final String LOCATION_HELPER_LOCATION = "com.nus.cs4222.isbtracker.LocationHelper.location";

    private Context mContext;
	
    // Define an object that holds accuracy and frequency parameters
    LocationRequest mLocationRequest;
    
    LocationClient mLocationClient;
    boolean mUpdatesRequested;
    
    boolean mSingleUpdateRequested = false;
    int mSingleUpdateResponseCount = 0;
    
    private boolean servicesConnected() {
    	// Check that Google Play services is available
    	int resultCode =
    			GooglePlayServicesUtil.
    			isGooglePlayServicesAvailable(mContext);
    	// If Google Play services is available
    	if (ConnectionResult.SUCCESS == resultCode) {
    		// In debug mode, log the status
    		Log.d("Location",
    				"Google Play services is available.");
    		// Continue
    		return true;
    		// Google Play services was not available for some reason
    	} else {
    		return false;
    	}
    }
    
    /*
     * Called by Location Services when the request to connect the
     * client finishes successfully. At this point, you can
     * request the current location or start periodic updates
     */
    @Override
    public void onConnected(Bundle dataBundle) {
        // Display the connection status
        Log.v("LocationHelper", "Connected to Google Play");
    }
    
    /*
     * Called by Location Services if the connection to the
     * location client drops because of an error.
     */
    @Override
    public void onDisconnected() {
        // Display the connection status
    	Log.v("LocationHelper", "Disconnected Google Play");
    }
    
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        /*if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        mContext,
                        CONNECTION_FAILURE_RESOLUTION_REQUEST);
                *//*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                *//*
            } catch (IntentSender.SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }
        } else {
            *//*
             * If no resolution is available, display a dialog to the
             * user with the error.
             *//*
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
                        "Location");
            }
        }*/
    }
    
    // Define the callback method that receives location updates
    // @param Location http://developer.android.com/reference/android/location/Location.html
    @Override
    public void onLocationChanged(Location location) {

        /*
        // Report to the UI that the location was updated
        mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        mLatLng.setText(LocationUtils.getLatLng(this, location));
        */

        reportNewLocation(location);

    	// note continuous updates should take priority over single updates
    	if (mUpdatesRequested){ // periodic updates
    		mSingleUpdateRequested = false;
            Log.d("CurrentLocation", "Getting results");            
            Log.i("LocationChanged", LocationUtils.getLatLng(mContext, location));
        }
    	else if (mSingleUpdateRequested) {
            Log.d("CurrentLocation", "Getting results");            
            Log.i("LocationChanged", LocationUtils.getLatLng(mContext, location));
        
            mSingleUpdateResponseCount++;
            
            // We wait for two counts in case first one is a cached response
            if (mSingleUpdateResponseCount >= 2) {
                mSingleUpdateRequested = false;
                mLocationClient.removeLocationUpdates(this);
            }
        }
        
        

    }
    
    public LocationHelper(Context context) {
        mContext = context;
        
        /*
         * Create a new location client, using the enclosing class to
         * handle callbacks.
         */
        mLocationClient = new LocationClient(mContext, this, this);
        mLocationClient.connect();
        
        // Start with updates turned off
        mUpdatesRequested = false;
    }

    
	// Location stuff    
    public void getLastLocation() {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Display the current location in the UI
            
            Log.d("ISBTracker", "Current Location: "+LocationUtils.getLatLng(mContext, currentLocation));
        }
    }
    
    // We need to do this so that the location request
 	// is not made on the IntentService thread (when activity response is received)...
 	// which is killed after the processing
 	public Handler getLocationHandler = new Handler(){

 		@Override
 		public void handleMessage(Message msg) {
 			
 			if (msg.what == 1) {
 				getCurrentLocationInHandler();
 			}else if (msg.what == 2) {
 				getContinuousLocationInHandler();
 			}
 		}
 		
 	};
 	
 	public void getCurrentLocation() {
 		getLocationHandler.sendEmptyMessage(1);
 	}
    
 	private void getCurrentLocationInHandler() {

        // If Google Play Services is available
        if (servicesConnected()) {
        	Log.d("CurrentLocation", "Creating location request");
        	LocationRequest mLocationRequest = LocationRequest.create();

            /*
             * Set the update interval
             */
            mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

            // Use high accuracy
            mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);

            // Set the interval ceiling to one minute
            mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        	
            Log.d("CurrentLocation", "Sending request");
        	mSingleUpdateRequested = true;
        	mSingleUpdateResponseCount = 0;
        	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        	
        }
    }
 	
 	public void getContinuousLocation() {
 		getLocationHandler.sendEmptyMessage(2);
 	}
    
    private void getContinuousLocationInHandler() {

        // If Google Play Services is available
        if (servicesConnected()) {
        	Log.d("CurrentLocation", "Creating location request");
        	LocationRequest mLocationRequest = LocationRequest.create();

            /*
             * Set the update interval
             */
            mLocationRequest.setInterval(LocationUtils.UPDATE_INTERVAL_IN_MILLISECONDS);

            // Use high accuracy
            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

            // Set the interval ceiling to one minute
            mLocationRequest.setFastestInterval(LocationUtils.FAST_INTERVAL_CEILING_IN_MILLISECONDS);
        	
            Log.d("CurrentLocation", "Sending request");
        	mUpdatesRequested = true;
        	mLocationClient.requestLocationUpdates(mLocationRequest, this);
        	
        }
    }
    
    public void stopContinousLocation(){
    	mUpdatesRequested = false;
    	mLocationClient.removeLocationUpdates(this);
    }

    private void reportNewLocation(Location location) {
        Intent intent = new Intent(ScannerService.UPDATE_TOPIC);
        intent.putExtra(Intent.EXTRA_SUBJECT, LOCATION_HELPER_EXTRA_SUBJECT);
        intent.putExtra(LOCATION_HELPER_LOCATION, location);
        LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);
    }
}
