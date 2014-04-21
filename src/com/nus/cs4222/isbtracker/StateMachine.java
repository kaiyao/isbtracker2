package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.location.Location;
import android.text.format.Time;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.nus.cs4222.isbtracker.route.BusRoutes;

import java.util.LinkedList;
import java.util.List;

public class StateMachine {
	
	public static final int ACTIVTY_DETECTION_INTERVAL_LONG = 20000;
	public static final int ACTIVTY_DETECTION_INTERVAL_SHORT = 2000;
	
	public static final int VEHICLE_MIN_SPEED_KPH = 20;
	public static final int VEHICLE_MIN_SPEED_MPS = VEHICLE_MIN_SPEED_KPH * 1000 / 3600;
	
	public static boolean SPEED_SIMULATION_MODE = true;
	
	public enum State {
	    Elsewhere, PossiblyWaitingForBus, WaitingForBus, PossiblyOnBus, OnBus
	}
	
	public enum DetectedType {
		Location, Activity
	}
	
	private State currentState;
	
	private Context mContext;
	
	private ActivityRecognitionResult lastActivityDetected;
	private Location lastLocationChangeDetected;
	private DetectedType lastDetectedType;
	private boolean continuousLocationEnabled;
	private int lastUsedActivityDetectionInterval;
	
	private BusStops busStops;
	private BusRoutes busRoutes;
	
	private LinkedList<StateChange> stateChangeList;
	
	private ActivityRecognitionHelper activityRecognition;
	private LocationHelper locationHelper;
	
	private StateMachineListener mListener;

	private boolean mIsTracking;
	
	private final float DISTANCE_LIMIT = 50.0f;	
	
	public StateMachine(Context context){
		mContext = context;

		busStops = new BusStops();
		busRoutes = new BusRoutes();

		mIsTracking = false;
		
		stateChangeList = new LinkedList<StateChange>();
	}

	public void activityDetected(ActivityRecognitionResult result){
		lastActivityDetected = result;
		lastDetectedType = DetectedType.Activity;
		checkStateChange();
	}
	
	public void locationChanged(Location location){
		
		mListener.onLogMessage("Location: " + location.getLatitude() + ","+location.getLongitude() + " "+location.getSpeed());
		
		if (SPEED_SIMULATION_MODE && location.getSpeed() == 0 && lastLocationChangeDetected != null) {
			
			float deltaDistance = location.distanceTo(lastLocationChangeDetected);
			long deltaTime = location.getTime() - lastLocationChangeDetected.getTime();
			
			// Speed is in meters per second
			if (deltaTime > 0) {
				float speed = deltaDistance / (deltaTime / 1000);
				
				// Prevent ridiculous "infinite" speed
				if (speed < 15) {
					location.setSpeed(speed);
				
					mListener.onLogMessage("Location simulated speed: " + speed);
				}
			}
			
			
		}
		
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
		if (lastActivityDetected != null) {
			List<DetectedActivity> activities = lastActivityDetected.getProbableActivities();
			for (DetectedActivity activity : activities){
				mListener.onLogMessage("Activity is "+getActivityNameFromType(activity.getType()) + " " + activity.getConfidence());
				
				if (activity.getType() == type) {
					return activity.getConfidence();
					
				}
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
				
		State previousState = null;
		if (stateChangeList != null && !stateChangeList.isEmpty()) {
			previousState = stateChangeList.getFirst().getState();
		}
		if (previousState != currentState) {
			mListener.onStateMachineChanged(currentState);
			
			// We add new state change to the head of the list
			StateChange stateChange = new StateChange(currentState, getCurrentTime());
			stateChangeList.addFirst(stateChange);
			
			// If list is too long we remove from the tail
			while (stateChangeList.size() > 100) {
				stateChangeList.removeLast();
			}
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
			
			// Check time spent waiting for bus at bus stop
			for (StateChange stateChange : stateChangeList) {
				if (stateChange.getState() == State.Elsewhere) {
					break;
				}
				
				long waitingTime = 0;
				if (stateChange.getState() == State.PossiblyWaitingForBus){
					waitingTime = 30000;
				}else if (stateChange.getState() == State.WaitingForBus){
					waitingTime = getCurrentTime().toMillis(false) - stateChange.getTimeEnteredState().toMillis(false) + 30000;
				}
				
				if (waitingTime > 0) {
					// Process waiting time here
				}
			}
			
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// position is near bus stop and time more than one minute
				Time timeEnteredCurrentState = stateChangeList.getLast().getTimeEnteredState();
				if (nearestStop.getDistanceFromLocation(currentPosition) <= 20 && 
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
				if (nearestStop.getDistanceFromLocation(currentPosition) > 50){ // Might have problem what if user runs after the bus?
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
				// position no longer near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > 50){ // Might have problem what if user runs after the bus?
					mListener.onLogMessage("position no longer near bus stop");
					currentState = State.Elsewhere;
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
			Time timeEnteredCurrentState = stateChangeList.getLast().getTimeEnteredState();
			if ((confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > 50 || 
					lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS) &&
					Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000)  {
				mListener.onLogMessage("vehicle motion or speed detected");
				currentState = State.OnBus;
			}
			
			// emergency bail out
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > 80)  {
				mListener.onLogMessage("Walking detected. Bailing out. State changing to elsewhere.");
				currentState = State.Elsewhere;
			}else if (busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > 20)  {
				mListener.onLogMessage("Too far from road ("+busRoutes.getDistanceFromRoutes(lastLocationChangeDetected)+"m). State changing to elsewhere.");
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
			}else if (busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > 20)  {
				mListener.onLogMessage("Too far from road ("+busRoutes.getDistanceFromRoutes(lastLocationChangeDetected)+"m). State changing to elsewhere.");
				currentState = State.Elsewhere;
			}
		}
		
		
		/*
		// Check if state has been changed above
		if (stateChangeList != null && !stateChangeList.isEmpty()) {
			previousState = stateChangeList.getFirst().getState();
		}
		if (previousState != currentState) {
			// If state has been changed, we process the current state
			checkStateChange();
		}
		*/
		
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
		mListener.onLogMessage("Start Tracking");
		mIsTracking = true;

		if (activityRecognition == null) {
			activityRecognition = new ActivityRecognitionHelper(mContext);
		}
		activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_LONG);
		if (locationHelper == null) {
			locationHelper = new LocationHelper(mContext);
		}

		// Reset state machine to initial state
		currentState = State.Elsewhere;
		lastUsedActivityDetectionInterval = 0;
		continuousLocationEnabled = false;

		mListener.onStateMachineChanged(currentState);
	}

	public void stopTracking() {
		mListener.onLogMessage("Stop Tracking");
		mIsTracking = false;

		if (activityRecognition != null) {
			activityRecognition.stopUpdates();
		}
		if (locationHelper != null) {
			locationHelper.stopContinousLocation();
		}
	}
	
	public boolean isTracking() {
		return mIsTracking;
		}
}
