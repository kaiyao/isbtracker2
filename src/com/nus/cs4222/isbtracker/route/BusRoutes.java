package com.nus.cs4222.isbtracker.route;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import com.nus.cs4222.isbtracker.ApplicationContext;

public class BusRoutes {
	
	public Map<String, BusRoute> routes;
	
	public BusRoutes(){
		routes = new HashMap<String, BusRoute>();
		readRoutesFromFile();
	}
	
	private void readRoutesFromFile(){	
		
		try {
			Context context = ApplicationContext.get();
			
			InputStreamReader is = new InputStreamReader(context.getAssets().open("NUS Bus Routes.csv"));
			BufferedReader reader = new BufferedReader(is);
			String line;
			while ((line = reader.readLine()) != null) {
				
				List<Location> route = new ArrayList<Location>();
				
				String[] lineParts = line.split(" ");
				String name = lineParts[0].trim();
				Log.v("",name);
				
				String[] lineParts2 = Arrays.copyOfRange(lineParts, 1, lineParts.length);
				for (String pointStr : lineParts2) {
					//Log.v("", pointStr);
					String[] pointParts = pointStr.split(",");
					double mLongitude = Double.valueOf(pointParts[0]);
					double mLatitude = Double.valueOf(pointParts[1]);
					
					Location l = new Location("");
					l.setLatitude(mLatitude);
					l.setLongitude(mLongitude);
					route.add(l);
				}	
				
				routes.put(name, new BusRoute(name, route));				
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}
	
	public Set<String> getRouteNames(){
		return routes.keySet();
	}
	
	public BusRoute getRouteWithName(String name){
		return routes.get(name);
	}
	
	public float getDistanceFromRoutes(Location point) {
		float minDistance = Float.POSITIVE_INFINITY;
		
		Set<Entry<String, BusRoute>> entrySet = routes.entrySet();
		for (Map.Entry<String, BusRoute> entry : entrySet) {
			BusRoute route = entry.getValue();
			float distance = route.getDistanceFromRoute(point);
			if (distance < minDistance) {
				minDistance = distance;
			}
		}
	    
	    return minDistance;
	}

}
