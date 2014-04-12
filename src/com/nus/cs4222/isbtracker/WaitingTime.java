package com.nus.cs4222.isbtracker;

import java.util.Date;

public class WaitingTime {
	Date time;
	int waitTime; //minutes
	BusStop busstop;

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

	public int getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}

}
