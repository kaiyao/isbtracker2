package com.nus.cs4222.isbtracker;

import android.text.format.Time;

public class TripSegmentMini {
	
	private Time time;
	private BusStop busStop;
	
	public TripSegmentMini(BusStop busStop, Time time) {
		this.time = time;
		this.busStop = busStop;
	}

	public Time getTime() {
		return time;
	}

	public BusStop getBusStop() {
		return busStop;
	}	

}
