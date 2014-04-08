package com.nus.cs4222.isbtracker;

import java.util.List;

import android.content.Context;
import android.location.Location;
import android.support.v4.app.FragmentActivity;
import android.text.format.Time;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;

public class StateMachine {
	
	public enum State {
	    Elsewhere, PossiblyWaitingForBus, WaitingForBus, PossiblyOnBus, OnBus
	}
	
	private static StateMachine theOne;
	private State currentState;
	
	private Context mContext;
	
	private ActivityRecognitionResult lastActivityDetected;
	private Location lastLocationChangeDetected;
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
		checkStateChange();
	}
	
	public void locationChanged(Location location){
		lastLocationChangeDetected = location;
		checkStateChange();
	}
	
	private Time getCurrentTime(){
		Time t = new Time();
		t.setToNow();
		return t;
	}
	
	public void checkStateChange(){
		
		if (stateWhenPreviousCheck != currentState) {
			timeEnteredCurrentState = getCurrentTime();
			stateWhenPreviousCheck = currentState;
		}		
		
		if (currentState == State.Elsewhere) {			
			
			// Check current position			
			locationHelper.getCurrentLocation();
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				// if position is near bus stop, change state
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT) {
					currentState = State.PossiblyWaitingForBus;
					checkStateChange();
				}
			}
		}else if (currentState == State.PossiblyWaitingForBus) {

			// Check current position
			// Set GPS to continuous poll
			locationHelper.getContinuousLocation();
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				/* position is near bus stop and time more than one minute*/
				if (nearestStop.getDistanceFromLocation(currentPosition) <= DISTANCE_LIMIT && 
						Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000) {
					currentState = State.WaitingForBus;
					checkStateChange();
				}
				
				/*position no longer near bus stop */
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT){ // Might have problem what if user runs after the bus?
					currentState = State.Elsewhere;
					locationHelper.stopContinousLocation();
					checkStateChange();
				}
			}
		}else if (currentState == State.WaitingForBus) {
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
			
				/* position is not near bus stop*/
				if (nearestStop.getDistanceFromLocation(currentPosition) > DISTANCE_LIMIT) {
					currentState = State.PossiblyOnBus;
					checkStateChange();
				}
			}
		}else if (currentState == State.PossiblyOnBus) {
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			/* accelerometer indicates vehicle movement */
			for (DetectedActivity activity : activities){
				if (activity.getType() == DetectedActivity.IN_VEHICLE && activity.getConfidence() > 50) {
					currentState = State.OnBus;
					checkStateChange();
				}
			}
		}else if (currentState == State.OnBus) {
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			
			/* accelerometer indicates walking movement */
			for (DetectedActivity activity : activities){
				if (activity.getType() == DetectedActivity.ON_FOOT && activity.getConfidence() > 80) {
					currentState = State.Elsewhere;
					checkStateChange();
				}
			}
		}
		
		
	}
	
	
	
}
