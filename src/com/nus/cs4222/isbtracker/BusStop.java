package com.nus.cs4222.isbtracker;

import java.util.List;

import android.location.Location;

public class BusStop {
	
	private int mId;
	private String mName;
	private Location mLocation;
	private List<Integer> mNextStopIds;
	private List<BusStop> mNextStops;
	
	public BusStop(String name, Location location, int id, List<Integer> nextStopIds){
		mName = name;
		mLocation = location;
		mId = id;
		mNextStopIds = nextStopIds;
	}
	
	public BusStop(int id, float lat, float longi){
		mId = id;
		mLocation = new Location("");
		mLocation.setLatitude(lat);
		mLocation.setLongitude(longi);
	}
	
	public BusStop(String mName2, Location l) {
		// TODO Auto-generated constructor stub
	}


	public int getId() {
		return mId;
	}

	public String getName() {
		return mName;
	}

	public Location getLocation() {
		return mLocation;
	}

	// Returns distance in meters
	public float getDistanceFromLocation(Location l){
		return l.distanceTo(mLocation);
	}

	public List<Integer> getNextStopIds() {
		return mNextStopIds;
	}

	public List<BusStop> getNextStops() {
		return mNextStops;
	}

	public void setNextStops(List<BusStop> nextStops) {
		this.mNextStops = nextStops;
	}

}
