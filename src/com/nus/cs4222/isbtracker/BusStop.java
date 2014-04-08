package com.nus.cs4222.isbtracker;

import android.location.Location;

public class BusStop {
	
	private String mName;
	private Location mLocation;
	
	public BusStop(String name, Location location){
		mName = name;
		mLocation = location;
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
