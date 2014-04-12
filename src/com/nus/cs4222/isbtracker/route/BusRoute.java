package com.nus.cs4222.isbtracker.route;

import java.util.List;

import android.location.Location;

public class BusRoute {
	
	private String mRouteName;
	private List<Location> mRoutePoints;
	
	public BusRoute(String name, List<Location> routePoints) {
		mRouteName = name;
		mRoutePoints = routePoints;
	}
	
	public float getDistanceFromRoute(Location point) {
		double minDistance = Float.POSITIVE_INFINITY;
		//Location mP1 = null, mP2 = null;
		
		Location p1 = null;
		for(Location p2 : mRoutePoints) {
			if (p1 != null) {				
				
				PointToLine p2l = new PointToLine(p1, p2, point);
				//Log.v("", "("+p1.getLatitude()+","+p1.getLongitude()+") ("+p2.getLatitude()+","+p2.getLongitude()+") "+p2l);
				if (minDistance > p2l.getDistance() && p2l.getDistanceAlongLine() >= 0 && p2l.getDistanceAlongLine() <= p1.distanceTo(p2)) {
					minDistance = p2l.getDistance();
					//mP1 = p1;
					//mP2 = p2;
				}
			}
			p1 = p2;
		}
		
		//Log.v("Min", "("+mP1.getLatitude()+","+mP1.getLongitude()+") ("+mP2.getLatitude()+","+mP2.getLongitude()+") ("+point.getLatitude()+","+point.getLongitude()+") "+minDistance);
		return (float) minDistance;
	}
	
	
	

	public String getRouteName() {
		return mRouteName;
	}

	public List<Location> getRoutePoints() {
		return mRoutePoints;
	}

}
