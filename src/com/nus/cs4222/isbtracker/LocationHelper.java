package com.nus.cs4222.isbtracker;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.IntentSender;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.view.View;

import com.nus.cs4222.isbtracker.R;

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
	
    private Context mContext;
    private FragmentActivity mActivity;
	
	// Location stuff
    private LocationClient mLocationClient;
    // A request to connect to Location Services
    private LocationRequest mLocationRequest;
    
    boolean mUpdatesRequested = false;
    boolean mSingleUpdateRequested = false;
    int mSingleUpdateResponseCount = 0;
    
    
	
    /**
     * Report location updates to the UI.
     *
     * @param location The updated location.
     */
    @Override
    public void onLocationChanged(Location location) {

    	/*
        // Report to the UI that the location was updated
        mConnectionStatus.setText(R.string.location_updated);

        // In the UI, set the latitude and longitude to the value received
        mLatLng.setText(LocationUtils.getLatLng(this, location));
        */
    	
    	if (mSingleUpdateRequested) {
    		Log.d("CurrentLocation", "Getting results");    		
    		Log.i("LocationChanged", LocationUtils.getLatLng(mContext, location));
    		
    		LogFile.getInstance(mContext).log(
            		LocationUtils.getLatLng(mContext, location)
                );
    	
    		mSingleUpdateResponseCount++;
    		
    		// We wait for two counts in case first one is a cached response
    		if (mSingleUpdateResponseCount >= 2) {
    			mSingleUpdateRequested = false;
    			mLocationClient.removeLocationUpdates(this);
    		}
    	}else{ // periodic updates
    		Log.d("CurrentLocation", "Getting results");    		
    		Log.i("LocationChanged", LocationUtils.getLatLng(mContext, location));
    		
    		LogFile.getInstance(mContext).log(
            		LocationUtils.getLatLng(mContext, location)
                );
    	}
    	
    }
	
	// Location stuff    
    public void getLastLocation(View v) {

        // If Google Play Services is available
        if (servicesConnected()) {

            // Get the current location
            Location currentLocation = mLocationClient.getLastLocation();

            // Display the current location in the UI
            //mLatLng.setText(LocationUtils.getLatLng(this, currentLocation));
            LogFile.getInstance(mContext).log(
            		LocationUtils.getLatLng(mContext, currentLocation)
                );
            
            Log.d("ISBTracker", "Current Location: "+LocationUtils.getLatLng(mContext, currentLocation));
        }
    }
    
    // Location stuff    
    public void getCurrentLocation(View v) {

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
    
    public void getContinuousLocation(View v) {

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
	
	public void setup(FragmentActivity activity) {
		mContext = activity;
		mActivity = activity;
		
		mLocationClient = new LocationClient(mContext, this, this);
		mLocationClient.connect();
	}
	
	private boolean servicesConnected() {

        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(mContext);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {
            // In debug mode, log the status
            Log.d(LocationUtils.APPTAG, mContext.getString(R.string.play_services_available));

            // Continue
            return true;
        // Google Play services was not available for some reason
        } else {
            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, mActivity, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(mActivity.getSupportFragmentManager(), LocationUtils.APPTAG);
            }
            return false;
        }
    }
	
	/**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }

	@Override
	public void onConnectionFailed(ConnectionResult connectionResult) {
		/*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {

                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(
                        mActivity,
                        LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

                /*
                * Thrown if Google Play services canceled the original
                * PendingIntent
                */

            } catch (IntentSender.SendIntentException e) {

                // Log the error
                e.printStackTrace();
            }
        } else {

            // If no resolution is available, display a dialog to the user with the error.
            showErrorDialog(connectionResult.getErrorCode());
        }
		
	}

	@Override
	public void onConnected(Bundle arg0) {
		if (mUpdatesRequested) {
            startPeriodicUpdates();
        }
		Log.i("Loc", "Connected");
		
	}

	@Override
	public void onDisconnected() {
		// TODO Auto-generated method stub
		
	}
	
	/**
     * In response to a request to start updates, send a request
     * to Location Services
     */
    private void startPeriodicUpdates() {

        mLocationClient.requestLocationUpdates(mLocationRequest, this);
        //mConnectionState.setText(R.string.location_requested);
    }

    /**
     * In response to a request to stop updates, send a request to
     * Location Services
     */
    private void stopPeriodicUpdates() {
        mLocationClient.removeLocationUpdates(this);
        //mConnectionState.setText(R.string.location_updates_stopped);
    }
	
	/**
     * Show a dialog returned by Google Play services for the
     * connection error code
     *
     * @param errorCode An error code returned from onConnectionFailed
     */
    private void showErrorDialog(int errorCode) {

        // Get the error dialog from Google Play services
        Dialog errorDialog = GooglePlayServicesUtil.getErrorDialog(
            errorCode,
            mActivity,
            LocationUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);

        // If Google Play services can provide an error dialog
        if (errorDialog != null) {

            // Create a new DialogFragment in which to show the error dialog
            ErrorDialogFragment errorFragment = new ErrorDialogFragment();

            // Set the dialog in the DialogFragment
            errorFragment.setDialog(errorDialog);

            // Show the error dialog in the DialogFragment
            errorFragment.show(mActivity.getSupportFragmentManager(), LocationUtils.APPTAG);
        }
    }

}
