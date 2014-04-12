package com.nus.cs4222.isbtracker;

import java.util.Date;

public class TripSegment {
	private BusStop start;
	private Date startTime;
	private BusStop end;
	private Date endTime;
	
	public TripSegment() {
		//details would be set later
	}
	
	public TripSegment(BusStop st, Date stTime, BusStop en, Date enTime) {
		//for SQL usage
		setStart(st);
		setStartTime(stTime);
		setEnd(en);
		setEndTime(enTime);
	}
	
	public BusStop getStart() {
		return start;
	}
	
	public void setStart(BusStop start) {
		this.start = start;
	}
	
	public Date getStartTime() {
		return startTime;
	}
	
	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}
	
	public BusStop getEnd() {
		return end;
	}
	
	public void setEnd(BusStop end) {
		this.end = end;
	}
	
	public Date getEndTime() {
		return endTime;
	}
	
	public void setEndTime(Date endTime) {
		this.endTime = endTime;
	}
}
