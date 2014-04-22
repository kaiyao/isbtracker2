package com.nus.cs4222.isbtracker;

import android.annotation.SuppressLint;
import java.util.Date;
import java.util.Locale;

public class WaitingTime {
	Date time;
	double waitTime; //minutes
	String day;
	BusStop busStop;

	public WaitingTime(BusStop bs, String d, Date t, double waitTime2) {
		time = t;
		day = d;
		waitTime = waitTime2;
		busStop = bs;
	}
	
	public WaitingTime(BusStop bs, int wt) {
		waitTime = wt;
		busStop = bs;
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

	
	@SuppressLint("DefaultLocale")
	public String getLine(){
		return String.format(Locale.US, "%d,%f,%d", time, waitTime, busstop.getId());
	}
	
	public WaitingTime(String line){
		String[] lineParts = line.split(",");
		if (lineParts.length >= 3){
			time = new Date(Long.valueOf(lineParts[0]));
			waitTime = Long.valueOf(lineParts[1]);
			busstop = BusStops.getInstance().getStopById(Integer.valueOf(lineParts[2]));
		}
	}


}
