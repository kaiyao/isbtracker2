package com.nus.cs4222.isbtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.location.Location;

public class BusStops {
	
	Context mContext;
	List<BusStop> listOfStops;
	
	public BusStops(Context context){
		mContext = context;
		listOfStops = new ArrayList<BusStop>();
		readStopsFromFile();		
	}
	
	public BusStop getNearestStop(Location l){
		float minDistance = Float.POSITIVE_INFINITY;
		BusStop nearestStop = null;
		
		for (BusStop stop : listOfStops){
			float distance = stop.getDistanceFromLocation(l);
			if (distance < minDistance) {
				minDistance = distance;
				nearestStop = stop;
			}
		}
		
		return nearestStop;
	}
	
	private void readStopsFromFile(){
		try {
			InputStreamReader is = new InputStreamReader(mContext.getAssets().open("NUS Bus Stops.csv"));
			BufferedReader reader = new BufferedReader(is);
			reader.readLine();
			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineParts = line.split(",");
				double mLatitude = Double.valueOf(lineParts[0]);
				double mLongitude = Double.valueOf(lineParts[1]);
				String mName = lineParts[2];
				
				Location l = new Location("");
				l.setLatitude(mLatitude);
				l.setLongitude(mLongitude);
				BusStop bs = new BusStop(mName, l, 0);
				listOfStops.add(bs);
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

}
