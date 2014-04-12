package com.nus.cs4222.isbtracker;

import android.location.Location;

public class BusStop {
	
	private int mId;
	private String mName;
	private Location mLocation;
	
	public BusStop(String name, Location location, int id){
		mName = name;
		mLocation = location;
		mId = id;
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

}
