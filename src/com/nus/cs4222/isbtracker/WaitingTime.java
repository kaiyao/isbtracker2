package com.nus.cs4222.isbtracker;

import android.annotation.SuppressLint;
import java.util.Date;
import java.util.Locale;

public class WaitingTime {
	Date time;
	long waitTime; // milliseconds
	BusStop busStop;

	public WaitingTime(BusStop bs, long waitTime) {
		this.waitTime = waitTime;
		this.busStop = bs;
		this.time = new Date();
	}

	public Date getTime() {
		return time;
	}

	public void setTime(Date time) {
		this.time = time;
	}

	public long getWaitTime() {
		return waitTime;
	}

	public void setWaitTime(int waitTime) {
		this.waitTime = waitTime;
	}
	
	@SuppressLint("DefaultLocale")
	public String getLine(){
		return String.format(Locale.US, "%d,%f,%d", time, waitTime, busStop.getId());
	}
	
	public WaitingTime(String line){
		String[] lineParts = line.split(",");
		if (lineParts.length >= 3){
			time = new Date(Long.valueOf(lineParts[0]));
			waitTime = Long.valueOf(lineParts[1]);
			busStop = BusStops.getInstance().getStopById(Integer.valueOf(lineParts[2]));
		}
	}


}
