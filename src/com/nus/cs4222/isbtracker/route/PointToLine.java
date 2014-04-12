package com.nus.cs4222.isbtracker.route;

import android.location.Location;

public class PointToLine {
	
	private Location p1, p2, p3;
	private double distance;
	private double distanceWithSign;
	private double distanceAlongLine;
	
	public PointToLine (Location p1, Location p2, Location p3){
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
		
		compute();
	}
	
	private void compute() {
		
		// Formula source: http://www.movable-type.co.uk/scripts/latlong.html (cross track distance)
		
		double d13 = p1.distanceTo(p3);
		double brng13 = deg2rad(p1.bearingTo(p3));
		double brng12 = deg2rad(p1.bearingTo(p2));
		double R = 6378100;
		
		double dXt = Math.asin(Math.sin(d13/R)*Math.sin(brng13-brng12)) * R;
		double dAt = Math.acos(Math.cos(d13/R)/Math.cos(dXt/R)) * R;
		
		distance =  Math.abs(dXt);
		distanceWithSign = dXt;
		distanceAlongLine = dAt;
	}
	
	public double getDistance() {
		return distance;
	}

	public double getDistanceWithSign() {
		return distanceWithSign;
	}

	public double getDistanceAlongLine() {
		return distanceAlongLine;
	}

	private double deg2rad(double deg) {
		return (deg * Math.PI / 180.0);
	}
}
