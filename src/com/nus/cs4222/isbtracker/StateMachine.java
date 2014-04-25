package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.location.Location;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.DetectedActivity;
import com.nus.cs4222.isbtracker.route.BusRoutes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

public class StateMachine {
	
	public static final int BUS_STOP_FAR_DISTANCE_LIMIT = 50;
	public static final int BUS_STOP_NEAR_DISTANCE_LIMIT = 20;
	public static final int ROUTE_DISTANCE_LIMIT = 20;
	
	public static final int ON_FOOT_CONFIDENCE = 80;
	public static final int IN_VEHICLE_CONFIDENCE = 60;
	
	public static final int ACTIVTY_DETECTION_INTERVAL_LONG = 20000;
	public static final int ACTIVTY_DETECTION_INTERVAL_SHORT = 2000;
	
	public static final int VEHICLE_MIN_SPEED_KPH = 20;
	public static final int VEHICLE_MIN_SPEED_MPS = VEHICLE_MIN_SPEED_KPH * 1000 / 3600;	
	
	public static boolean SPEED_SIMULATION_MODE = Common.SIMULATION_MODE;
	
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
	private LinkedList<TripSegmentMini> tripSegmentsList;
	
	private ActivityRecognitionHelper activityRecognition;
	private LocationHelper locationHelper;
	
	private Time lastWaitingTimeLogEnteredStateTime;
	
	private StateMachineListener mListener;

    private DtnComms dtnComms;

	private boolean mIsTracking;
	
	private final float DISTANCE_LIMIT = 50.0f;	
	
	public StateMachine(Context context){
		mContext = context;

		busStops = BusStops.getInstance();
		busRoutes = new BusRoutes();

		mIsTracking = false;
		
		stateChangeList = new LinkedList<StateChange>();
		tripSegmentsList = new LinkedList<TripSegmentMini>();

		if (Common.ENABLE_DTN) {
			dtnComms = new DtnComms();
		}
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
			
			disableContinousLocationAndSetLongActivityDetectionInterval();
			
			// Reset waiting time logger
			lastWaitingTimeLogEnteredStateTime = null;
			
			// Check current position
			if (lastDetectedType == DetectedType.Activity) {
				mListener.onLogMessage("Detected activity");
				mListener.onLogMessage("Most probable activity is "+getActivityNameFromType(lastActivityDetected.getMostProbableActivity().getType()) + " " + lastActivityDetected.getActivityConfidence(lastActivityDetected.getMostProbableActivity().getType()));
				if (Common.SIMULATION_MODE) {
					locationHelper.getCurrentLocation();
				}else{				
					if (isMoving(lastActivityDetected.getMostProbableActivity().getType())){
						mListener.onLogMessage("Getting Location");
						locationHelper.getCurrentLocation();
					}
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
				
				// end of bus trip
				if (tripSegmentsList != null && !tripSegmentsList.isEmpty()) {
					
					// No need to log the last bus stop as it would have been recorded
					// in the on bus state
					/*
					BusStop lastStop = tripSegmentsList.getFirst().getBusStop();
					BusStop nearestExpectedStop = null;
					float nearestExpectedStopDistance = Float.POSITIVE_INFINITY;
					for (BusStop bs : lastStop.getNextStops()){
						if (bs.getDistanceFromLocation(currentPosition) < nearestExpectedStopDistance) {
							nearestExpectedStopDistance = bs.getDistanceFromLocation(currentPosition);
							nearestExpectedStop = bs;
						}
					}
					tripSegmentsList.addFirst(new TripSegmentMini(nearestExpectedStop, getCurrentTime()));
					*/
					
					// At this point record the trip segments list
					// And reset it in preparation for the next trip
					Log.v("isbtracker.StateMachine", "End of trip");
					CollectedDataCache.getInstance().logTrip(tripSegmentsList);
					tripSegmentsList = new LinkedList<TripSegmentMini>();
					
				}
			}
			
		}else if (currentState == State.PossiblyWaitingForBus) {
			
			mListener.onLogMessage("Current State is possibly waiting for bus");

			enableContinousGpsAndSetShortActivityDetectionInterval();
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				stateChangeList.getFirst().setBusStopForState(nearestStop);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// position is near bus stop and time more than one minute
				Time timeEnteredCurrentState = stateChangeList.getLast().getTimeEnteredState();
				if (nearestStop.getDistanceFromLocation(currentPosition) <= BUS_STOP_NEAR_DISTANCE_LIMIT && 
						Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000) {
					mListener.onLogMessage("position is near bus stop and time more than one minute");
					currentState = State.WaitingForBus;
				}else
				// vehicle speed is fast or movement detected
				if (confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > IN_VEHICLE_CONFIDENCE || lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS)  {
					mListener.onLogMessage("vehicle motion or speed detected");
					currentState = State.PossiblyOnBus;
				} else				
				// position no longer near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > BUS_STOP_FAR_DISTANCE_LIMIT && 
						(busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > ROUTE_DISTANCE_LIMIT ||
								confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > ON_FOOT_CONFIDENCE)
						){ // Might have problem what if user runs after the bus?
					mListener.onLogMessage("position no longer near bus stop");
					currentState = State.Elsewhere;
				}
			}
		}else if (currentState == State.WaitingForBus) {
			
			mListener.onLogMessage("Current State is waiting for bus");
			
			enableContinousGpsAndSetShortActivityDetectionInterval();
			if (dtnComms != null) dtnComms.start();
			
			if (lastLocationChangeDetected != null) {
				Location currentPosition = lastLocationChangeDetected;
				BusStop nearestStop = busStops.getNearestStop(currentPosition);
				
				stateChangeList.getFirst().setBusStopForState(nearestStop);
				
				mListener.onLogMessage("Nearest stop " + nearestStop.getName() + " distance " + nearestStop.getDistanceFromLocation(currentPosition));
				
				// vehicle speed is fast or movement detected
				if (confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > IN_VEHICLE_CONFIDENCE || lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS)  {
					mListener.onLogMessage("vehicle motion or speed detected");
					currentState = State.PossiblyOnBus;
				}else
				// position no longer near bus stop
				if (nearestStop.getDistanceFromLocation(currentPosition) > BUS_STOP_FAR_DISTANCE_LIMIT && 
						(busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > ROUTE_DISTANCE_LIMIT ||
								confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > ON_FOOT_CONFIDENCE)
						){ // Might have problem what if user runs after the bus?
					mListener.onLogMessage("position no longer near bus stop");
					currentState = State.Elsewhere;
				}
			}
		}else if (currentState == State.PossiblyOnBus) {
			
			mListener.onLogMessage("Current State is possibly on bus");
			
			enableContinousGpsAndSetShortActivityDetectionInterval();
			if (dtnComms != null) dtnComms.stop();
			
			// ********************************************
			// Check time spent waiting for bus at bus stop
			// ********************************************
			for (StateChange stateChange : stateChangeList) {
				if (stateChange.getState() == State.Elsewhere) {
					// If we encounter elsewhere when we look back at the history, it means
					// a bug, probably. Since the user cannot transition into the on bus states
					// from elsewhere
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
					
					// We check that it is not the same as the bus stop in the 
					// last logged time to prevent duplicates
					if (lastWaitingTimeLogEnteredStateTime != stateChange.getTimeEnteredState()) {
						lastWaitingTimeLogEnteredStateTime = stateChange.getTimeEnteredState();
						Log.d("isbtracker.StateMachine", "Waiting Time" + stateChange.getBusStopForState().getName() + " " + waitingTime);
						CollectedDataCache.getInstance().logWaitingTime(stateChange.getBusStopForState(), (float)waitingTime/60000);
					}
					
					// We break here as this means that we have already encountered a waiting at bus stop
					// when we check from the current state backwards
					// We don't want to further check even older waiting at bus stop states
					break;
				}
			}
			
			logTripSegment();
			
			// accelerometer indicates vehicle movement
			Time timeEnteredCurrentState = stateChangeList.getLast().getTimeEnteredState();
			if ((confidenceForActivity(lastActivityDetected, DetectedActivity.IN_VEHICLE) > IN_VEHICLE_CONFIDENCE || 
					lastLocationChangeDetected.getSpeed() > VEHICLE_MIN_SPEED_MPS) &&
					Math.abs(getCurrentTime().toMillis(false) - timeEnteredCurrentState.toMillis(false)) > 60000)  {
				mListener.onLogMessage("vehicle motion or speed detected");
				currentState = State.OnBus;
			}
			
			// emergency bail out
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > ON_FOOT_CONFIDENCE)  {
				mListener.onLogMessage("Walking detected. Bailing out. State changing to elsewhere.");
				currentState = State.Elsewhere;
			}else if (busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > ROUTE_DISTANCE_LIMIT)  {
				mListener.onLogMessage("Too far from road ("+busRoutes.getDistanceFromRoutes(lastLocationChangeDetected)+"m). State changing to elsewhere.");
				currentState = State.Elsewhere;
			}
		}else if (currentState == State.OnBus) {
			
			mListener.onLogMessage("Current State is on bus");
			
			enableContinousGpsAndSetShortActivityDetectionInterval();
			if (dtnComms != null) dtnComms.stop();
			
			logTripSegment();
			
			//accelerometer indicates walking movement
			if (confidenceForActivity(lastActivityDetected, DetectedActivity.ON_FOOT) > ON_FOOT_CONFIDENCE)  {
				mListener.onLogMessage("Walking detected. State changing to elsewhere.");
				currentState = State.Elsewhere;
			}else if (busRoutes.getDistanceFromRoutes(lastLocationChangeDetected) > ROUTE_DISTANCE_LIMIT)  {
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

	private void logTripSegment() {
		if (lastLocationChangeDetected != null) {
			Location currentPosition = lastLocationChangeDetected;
			
			if (tripSegmentsList == null){
				tripSegmentsList = new LinkedList<TripSegmentMini>();
			}
			
			Log.d("isbtracker.statemachine.logtripsegment", "Trip logger");
			if (tripSegmentsList.isEmpty()){
				Log.d("isbtracker.statemachine.logtripsegment", "Trip logger empty");
				// If it is the first stop, we try to look for the nearest stop first
				// Then we look back at the states to see if there's a waitingForBus state
				// those are more accurate as the bus may have moved considerably before the
				// on bus state is activated
				BusStop startingStop = busStops.getNearestStop(currentPosition);
				for (StateChange stateChange : stateChangeList) {
					if (stateChange.getState() == State.WaitingForBus){
						startingStop = stateChange.getBusStopForState();
						break;
					}else if (stateChange.getState() == State.PossiblyWaitingForBus){
						startingStop = stateChange.getBusStopForState();
						break;
					}
				}				
				tripSegmentsList.addFirst(new TripSegmentMini(startingStop, getCurrentTime()));
				Log.d("isbtracker.statemachine.logtripsegment", "First Stop: " + startingStop.getName());
			}else{
				
				Log.d("isbtracker.statemachine.logtripsegment", "Trip logger not empty");
				
				if (tripSegmentsList.size() == 1) {
					Log.d("isbtracker.statemachine.logtripsegment", "Trip logger size 1");
					// If this is the second stop in the trip, we try to correct for the first stop
					// in case the first stop was recorded wrongly
					BusStop lastStop = tripSegmentsList.getFirst().getBusStop();
					Set<BusStop> possibleFirstStops = new HashSet<BusStop>();
					for (BusStop bs : BusStops.getInstance().getListOfStops()){
						if (bs.getDistanceFromLocation(lastStop.getLocation()) < BUS_STOP_FAR_DISTANCE_LIMIT) {
							possibleFirstStops.add(bs);
						}
					}
					
					for (BusStop pbs : possibleFirstStops){
						Log.d("isbtracker.statemachine.logtripsegment", "Each Possible First Stop: " + pbs.getName());
						for (BusStop bs : pbs.getNextStops()){
							Log.d("isbtracker.statemachine.logtripsegment", "Each Possible Next Stop: " + bs.getName());
							if (bs.getDistanceFromLocation(currentPosition) < 30) {
								tripSegmentsList.getFirst().setBusStop(pbs);
								tripSegmentsList.addFirst(new TripSegmentMini(bs, getCurrentTime()));
								
								Log.d("isbtracker.statemachine.logtripsegment", "New First Stop: " + pbs.getName());
								Log.d("isbtracker.statemachine.logtripsegment", "Next Stop: " + bs.getName());
								break;
							}
						}
					}
					
					
				}else{
					Log.d("isbtracker.statemachine.logtripsegment", "Trip logger size >1");
					
					// Using the last stop, we "look out" for the next possible bus stop
					BusStop lastStop = tripSegmentsList.getFirst().getBusStop();
					
					for (BusStop bs : lastStop.getNextStops()){
						if (bs.getDistanceFromLocation(currentPosition) < 30) {
							tripSegmentsList.addFirst(new TripSegmentMini(bs, getCurrentTime()));
							Log.d("isbtracker.statemachine.logtripsegment", "Next Stop: " + bs.getName());
							break;
						}
					}
				}
			}
			
		}
	}

	private void disableContinousLocationAndSetLongActivityDetectionInterval() {
		if (continuousLocationEnabled) {
			locationHelper.stopContinousLocation();
			continuousLocationEnabled = false;
		}
		
		if (lastUsedActivityDetectionInterval != ACTIVTY_DETECTION_INTERVAL_LONG){
			lastUsedActivityDetectionInterval = ACTIVTY_DETECTION_INTERVAL_LONG;
			activityRecognition.startUpdates(ACTIVTY_DETECTION_INTERVAL_LONG);				
		}
	}

	private void enableContinousGpsAndSetShortActivityDetectionInterval() {
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
		dtnComms.start();
	}

	public void stopTracking() {
		mListener.onLogMessage("Stop Tracking");
		mIsTracking = false;

        dtnComms.stop();

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
