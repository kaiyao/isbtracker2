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
	
	public enum State {
	    Elsewhere, PossiblyWaitingForBus, WaitingForBus, PossiblyOnBus, OnBus
	}
	
	public enum DetectedType {
		Location, Activity
	}
	
	private static StateMachine theOne;
	private State currentState;
	
	private Context mContext;
	
	private ActivityRecognitionResult lastActivityDetected;
	private Location lastLocationChangeDetected;
	private DetectedType lastDetectedType;
	private boolean continuousLocationEnabled = false;
	
	private BusStops busStops;
	
	private State stateWhenPreviousCheck;
	private Time timeEnteredCurrentState;
	
	private LocationHelper locationHelper;
	
	private StateMachineListener mListener;
	
	private final float DISTANCE_LIMIT = 50.0f;
	
	private StateMachine(Context context){
		mContext = context;
		
		currentState = State.Elsewhere;
		stateWhenPreviousCheck = State.Elsewhere;
		timeEnteredCurrentState = getCurrentTime();
		
		busStops = new BusStops(mContext);
		locationHelper = new LocationHelper((FragmentActivity) mContext);
	}
	
	public static StateMachine getInstance(Context context){
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
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// position is near bus stop and time more than one minute
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT && 
						Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000) {
					mListener.onLogMessage("position is near bus stop and time more than one minute");
					currentState = State.WaitingForBus;
				}
				
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
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
			
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
			
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			// accelerometer indicates vehicle movement
			for (DetectedActivity activity : activities){
				mListener.onLogMessage("Activity is "+getActivityNameFromType(activity.getType()) + " " + activity.getConfidence());
				if (activity.getType() == DetectedActivity.IN_VEHICLE && activity.getConfidence() > 50) {
					currentState = State.OnBus;
				}
				
				// emergency bail out
				if (activity.getType() == DetectedActivity.ON_FOOT && activity.getConfidence() > 80) {
					mListener.onLogMessage("Walking detected! Emergency bail out!");
					currentState = State.Elsewhere;
				}
			}
		}else if (currentState == State.OnBus) {
			
			mListener.onLogMessage("Current State is on bus");
			
			if (!continuousLocationEnabled) {
				mListener.onLogMessage("Continous location not enabled, enabling...");
				locationHelper.getContinuousLocation();
				continuousLocationEnabled = true;
			}
			
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			//accelerometer indicates walking movement
			for (DetectedActivity activity : activities){
				mListener.onLogMessage("Activity is "+getActivityNameFromType(activity.getType()) + " " + activity.getConfidence());
				if (activity.getType() == DetectedActivity.ON_FOOT && activity.getConfidence() > 80) {
					currentState = State.Elsewhere;
				}
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
	
	
	
}
