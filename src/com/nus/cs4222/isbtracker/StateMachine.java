package com.nus.cs4222.isbtracker;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class StateMachine {
	
	public static final int ACTIVTY_DETECTION_INTERVAL_LONG = 20000;
	public static final int ACTIVTY_DETECTION_INTERVAL_SHORT = 2000;
	
	public static final int VEHICLE_MIN_SPEED_KPH = 20;
	public static final int VEHICLE_MIN_SPEED_MPS = VEHICLE_MIN_SPEED_KPH * 1000 / 3600;
	
	public enum State {
	    Elsewhere, PossiblyWaitingForBus, WaitingForBus, PossiblyOnBus, OnBus
	}
	
	public enum DetectedType {
		Location, Activity
	}
	
	private static StateMachine theOne;
	private State currentState;
	
	private FragmentActivity mActivity;
	
	private ActivityRecognitionResult lastActivityDetected;
	private Location lastLocationChangeDetected;
	private DetectedType lastDetectedType;
	private boolean continuousLocationEnabled = false;
	private int lastUsedActivityDetectionInterval = 0;
	
	private BusStops busStops;
	
	private State stateWhenPreviousCheck;
	private Time timeEnteredCurrentState;
	
	private ActivityRecognitionHelper activityRecognition;
	private LocationHelper locationHelper;
	
	private StateMachineListener mListener;
	
	private final float DISTANCE_LIMIT = 50.0f;
	
	private StateMachine(FragmentActivity context){
		mActivity = context;
		
		currentState = State.Elsewhere;
		stateWhenPreviousCheck = State.Elsewhere;
		timeEnteredCurrentState = getCurrentTime();
		
		busStops = new BusStops();
		locationHelper = new LocationHelper(mActivity);
	}
	
	public static StateMachine getInstance(FragmentActivity context){
		if (theOne == null){
			theOne = new StateMachine(context);
		}
		return theOne;
	}
	
	public static StateMachine getReadyInstance(){
		return theOne;
	}
	
	public void activityDetected(ActivityRecognitionResult result){
		lastActivityDetected = result;
		lastDetectedType = DetectedType.Activity;
		checkStateChange();
	}
	
	public void locationChanged(Location location){
		lastLocationChangeDetected = location;
		lastDetectedType = DetectedType.Location;
		checkStateChange();
	}
	
	private Time getCurrentTime(){
		Time t = new Time();
		t.setToNow();
		return t;
	}
	
	private boolean isMoving(int type) {
        switch (type) {
            // These types mean that the user is probably not moving
            case DetectedActivity.STILL :
            case DetectedActivity.TILTING :
            case DetectedActivity.UNKNOWN :
                return false;
            default:
                return true;
        }
    }
	
	private int confidenceForActivity(ActivityRecognitionResult lastActivityDetected, int type) {
		List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
		for (DetectedActivity activity : activities){
			mListener.onLogMessage("Activity is "+getActivityNameFromType(activity.getType()) + " " + activity.getConfidence());
			if (activity.getType() == type) {
				return activity.getConfidence();
			}
		}
		return 0;
    }
	
	/**
     * Map detected activity types to strings
     *
     * @param activityType The detected activity type
     * @return A user-readable name for the type
     */
    private String getActivityNameFromType(int activityType) {
        switch(activityType) {
            case DetectedActivity.IN_VEHICLE:
                return "in_vehicle";
            case DetectedActivity.ON_BICYCLE:
                return "on_bicycle";
            case DetectedActivity.ON_FOOT:
                return "on_foot";
            case DetectedActivity.STILL:
                return "still";
            case DetectedActivity.UNKNOWN:
                return "unknown";
            case DetectedActivity.TILTING:
                return "tilting";
        }
        return "unknown";
    }
	
	public void checkStateChange(){
				
		if (stateWhenPreviousCheck != currentState) {
			timeEnteredCurrentState = getCurrentTime();
			mListener.onStateMachineChanged();
		}
		
		if (currentState == State.Elsewhere) {	
			
			mListener.onLogMessage("Current State is elsewhere");
			
			if (continuousLocationEnabled) {
				locationHelper.stopContinousLocation();
				continuousLocationEnabled = false;
			}
			
			if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_LONG){
				lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_LONG;
				activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_LONG);				
			}
			
			// Check current position
			if (lastDetectedType == DetectedType.Activity) {
				mListener.onLogMessage("Detected activity");
				mListener.onLogMessage("Most probable activity is "+getActivityNameFromType(lastActivityDetected.getMostProbableActivity().getType()) + " " + lastActivityDetected.getActivityConfidence(lastActivityDetected.getMostProbableActivity().getType()));
				if (isMoving(lastActivityDetected.getMostProbableActivity().getType())){
					mListener.onLogMessage("Getting Location");
					locationHelper.getCurrentLocation();
				}				
			}else{
				mListener.onLogMessage("Location obtained");
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// if position is near bus stop, change state
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT) {
					currentState = State.PossiblyWaitingForBus;					
				}
			}
			
		}else if (currentState == State.PossiblyWaitingForBus) {
			
			mListener.onLogMessage("Current State is possibly waiting for bus");

			// Check current position
			// Set GPS to continuous poll
			mListener.onLogMessage("Checking if continous poll mode");
			if (!continuousLocationEnabled) {
				mListener.onLogMessage("Continous location not enabled, enabling...");
				locationHelper.getContinuousLocation();
				continuousLocationEnabled = true;
			}
			
			if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_SHORT){
				lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_SHORT;
				activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_SHORT);				
			}
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// position is near bus stop and time more than one minute
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT && 
						Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000) {
					mListener.onLogMessage("position is near bus stop and time more than one minute");
					currentState = State.WaitingForBus;
				}else
				// vehicle speed is fast or movement detected
				if (confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > 50 || lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS)  {
					mListener.onLogMessage("vehicle motion or speed detected");
					currentState = State.PossiblyOnBus;
				} else				
				// position no longer near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT){ // Might have problem what if user runs after the bus?
					mListener.onLogMessage("position no longer near bus stop");
					currentState = State.Elsewhere;
				}
			}
		}else if (currentState == State.WaitingForBus) {
			
			mListener.onLogMessage("Current State is waiting for bus");
			
			if (!continuousLocationEnabled) {
				mListener.onLogMessage("Continous location not enabled, enabling...");
				locationHelper.getContinuousLocation();
				continuousLocationEnabled = true;
			}
			
			if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_SHORT){
				lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_SHORT;
				activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_SHORT);				
			}
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
			
				// vehicle speed is fast or movement detected
				if (confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > 50 || lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS)  {
					mListener.onLogMessage("vehicle motion or speed detected");
					currentState = State.PossiblyOnBus;
				}else
				// position is not near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT) {
					currentState = State.PossiblyOnBus;
				}
			}
		}else if (currentState == State.PossiblyOnBus) {
			
			mListener.onLogMessage("Current State is possibly on bus");
			
			if (!continuousLocationEnabled) {
				mListener.onLogMessage("Continous location not enabled, enabling...");
				locationHelper.getContinuousLocation();
				continuousLocationEnabled = true;
			}
			
			if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_SHORT){
				lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_SHORT;
				activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_SHORT);				
			}
			
			// accelerometer indicates vehicle movement
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > 50 || lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS)  {
				mListener.onLogMessage("vehicle motion or speed detected");
				currentState = State.OnBus;
			}
			
			// emergency bail out
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > 80)  {
				mListener.onLogMessage("Walking detected! Emergency bail out!");
				currentState = State.Elsewhere;
			}
		}else if (currentState == State.OnBus) {
			
			mListener.onLogMessage("Current State is on bus");
			
			if (!continuousLocationEnabled) {
				mListener.onLogMessage("Continous location not enabled, enabling...");
				locationHelper.getContinuousLocation();
				continuousLocationEnabled = true;
			}
			
			if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_SHORT){
				lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_SHORT;
				activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_SHORT);				
			}
			
			//accelerometer indicates walking movement
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > 80)  {
				mListener.onLogMessage("Walking detected. State changing to elsewhere.");
				currentState = State.Elsewhere;
			}
		}
		
		if (stateWhenPreviousCheck != currentState) {
			stateWhenPreviousCheck = currentState;
			checkStateChange();
		}
	}

	public State getCurrentState() {
		return currentState;
	}

	public StateMachineListener getListener() {
		return mListener;
	}

	public void setListener(StateMachineListener mListener) {
		this.mListener = mListener;
	}
	
	public void startTracking() {
        activityRecognition = new ActivityRecognitionHelper(mActivity);
		activityRecognition.startUpdates(20000);
		mListener.onLogMessage("Start Tracking");
	}
	
	public void stopTracking() {
		mListener.onLogMessage("Stop Tracking");
		if (activityRecognition != null) {
			activityRecognition.stopUpdates();
		}
		if (locationHelper != null) {
			locationHelper.stopContinousLocation();
		}
	}
	
	
}
