package com.nus.cs4222.isbtracker;

import android.text.format.Time;

public class StateChange {
	
	private StateMachine.State state;
	private Time timeEnteredState;
	private BusStop busStopForState;
	
	public StateChange(StateMachine.State state, Time timeEnteredState){
		this.state = state;
		this.timeEnteredState = timeEnteredState;
	}
	
	public StateMachine.State getState() {
		return state;
	}
	
	public Time getTimeEnteredState() {
		return timeEnteredState;
	}

	public BusStop getBusStopForState() {
		return busStopForState;
	}

	public void setBusStopForState(BusStop busStopForState) {
		this.busStopForState = busStopForState;
	}
	
	
}
