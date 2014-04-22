package com.nus.cs4222.isbtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import android.util.SparseArray;

public class BusStops {
	
	private static BusStops theOne;	
	private List<BusStop> listOfStops;
	private SparseArray<BusStop> stopsById;
	
	private BusStops(){
		listOfStops = new ArrayList<BusStop>();
		stopsById = new SparseArray<BusStop>();
		readStopsFromFile();		
	}
	
	public static BusStops getInstance(){
		if (theOne == null){
			theOne = new BusStops();
		}
		return theOne;
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
			
			Context context = ApplicationContext.get();
			
			InputStreamReader is = new InputStreamReader(context.getAssets().open("NUS Bus Stops.csv"));
			BufferedReader reader = new BufferedReader(is);
			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineParts = line.split(",");
				double mLatitude = Double.valueOf(lineParts[0]);
				double mLongitude = Double.valueOf(lineParts[1]);
				String mName = lineParts[2];
				int mId = Integer.valueOf(lineParts[3]);
				List<Integer> nextStopIds = new ArrayList<Integer>();
				if (lineParts.length > 4){
					String nextStopIdString = lineParts[4];
					String[] nextStopIdStrings = nextStopIdString.split(" ");
					for (String nextStopId : nextStopIdStrings){
						nextStopIds.add(Integer.valueOf(nextStopId));
					}
				}
				
				Location l = new Location("");
				l.setLatitude(mLatitude);
				l.setLongitude(mLongitude);
				BusStop bs = new BusStop(mName, l, mId, nextStopIds);
				listOfStops.add(bs);				
				stopsById.put(bs.getId(), bs);
			}
			
			for (BusStop bs : listOfStops) {
				List<BusStop> nextStops = new ArrayList<BusStop>();
				for (Integer nextStopId : bs.getNextStopIds()){
					nextStops.add(stopsById.get(nextStopId));
				}
				bs.setNextStops(nextStops);
			}
			Log.v("isbtracker.BusStops", "Read bus stop file");
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	public List<BusStop> getListOfStops() {
		return listOfStops;
	}

	public BusStop getStopById(int key) {
		return stopsById.get(key);
	}

}
