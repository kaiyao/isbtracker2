package com.nus.cs4222.isbtracker;

import java.util.Date;

public class WaitingTime {
	Date time;
	double waitTime; //minutes
	String day;
	BusStop busstop;

	public WaitingTime(BusStop bs, String d, Date t, double waitTime2) {
		time = t;
		day = d;
		waitTime = waitTime2;
		busstop = bs;
	}
	
	public WaitingTime(BusStop bs, int wt) {
		waitTime = wt;
		busstop = bs;
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public double getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

}
