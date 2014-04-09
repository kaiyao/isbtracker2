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
	private boolean continousLocationEnabled = false;
	
	private BusStops busStops;
	
	private State stateWhenPreviousCheck;
	private Time timeEnteredCurrentState;
	
	private LocationHelper locationHelper;
	
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
	
	public void checkStateChange(){
				
		if (stateWhenPreviousCheck != currentState) {
			timeEnteredCurrentState = getCurrentTime();
		}
		
		if (currentState == State.Elsewhere) {			
			
			// Check current position
			if (lastDetectedType == DetectedType.Activity) {
				if (isMoving(lastActivityDetected.getMostProbableActivity().getType())){
					locationHelper.getCurrentLocation();
				}				
			}else{
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				// if position is near bus stop, change state
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT) {
					currentState = State.PossiblyWaitingForBus;					
				}
			}
			
		}else if (currentState == State.PossiblyWaitingForBus) {

			// Check current position
			// Set GPS to continuous poll
			if (!continousLocationEnabled) {
				locationHelper.getContinuousLocation();
				continousLocationEnabled = true;
			}
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				// position is near bus stop and time more than one minute
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT && 
						Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000) {
					currentState = State.WaitingForBus;
				}
				
				// position no longer near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT){ // Might have problem what if user runs after the bus?
					currentState = State.Elsewhere;
					locationHelper.stopContinousLocation();
					continousLocationEnabled = false;
				}
			}
		}else if (currentState == State.WaitingForBus) {
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
			
				// position is not near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT) {
					currentState = State.PossiblyOnBus;
				}
			}
		}else if (currentState == State.PossiblyOnBus) {
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			// accelerometer indicates vehicle movement
			for (DetectedActivity activity : activities){
				if (activity.getType() == DetectedActivity.IN_VEHICLE && activity.getConfidence() > 50) {
					currentState = State.OnBus;
				}
			}
		}else if (currentState == State.OnBus) {
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			//accelerometer indicates walking movement
			for (DetectedActivity activity : activities){
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
	
	
	
}
