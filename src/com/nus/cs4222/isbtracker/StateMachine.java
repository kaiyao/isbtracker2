package com.nus.cs4222.isbtracker;

public class StateMachine {
	
	public enum State {
	    Elsewhere, PossiblyWaitingForBus, WaitingForBus, PossiblyOnBus, OnBus
	}
	
	private static StateMachine theOne;
	private State currentState;
	
	private StateMachine(){
		currentState = State.Elsewhere;
	}
	
	public static StateMachine getInstance(){
		if (theOne == null){
			theOne = new StateMachine();
		}
		return theOne;
	}
	
	public void activityDetected(){
		
	}
	
	public void locationChanged(){
		
	}
	
	public void checkStateChange(){
		
		if (currentState == State.Elsewhere) {
			// Check current position
			if (true/* position is near bus stop */) {
				currentState = State.PossiblyWaitingForBus;
			}
		}else if (currentState == State.PossiblyWaitingForBus) {
			// Check current position
			// Set GPS to continuous poll
			
			if (true/* position is near bus stop and time more than one minute*/) {
				currentState = State.WaitingForBus;
			}
			
			if (true /*position no longer near bus stop */){ // Might have problem what if user runs after the bus?
				currentState = State.Elsewhere;
			}
		}else if (currentState == State.WaitingForBus) {
			if (true/* position is not near bus stop*/) {
				currentState = State.PossiblyOnBus;
			}
		}else if (currentState == State.PossiblyOnBus) {
			if (true/* accelerometer indicates vehicle movement */) {
				currentState = State.OnBus;
			}
		}else if (currentState == State.OnBus) {
			if (true/* accelerometer indicates walking movement */) {
				currentState = State.Elsewhere;
			}
		}
		
		
	}
	
	
	
}
