package com.nus.cs4222.isbtracker;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormatSymbols;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.util.Log;

public class WaitingTimes {
	
	Context mContext;
	List<WaitingTime> listOfWaitingTime;
	BusStops busStops;
	
	public WaitingTimes() {
		listOfWaitingTime = new ArrayList<WaitingTime>();
		readTimesFromFile();
		busStops = BusStops.getInstance();
	
	}
	
	private void readTimesFromFile(){
		try {
			
			Context context = ApplicationContext.get();
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm:ss", Locale.US);
			InputStreamReader is = new InputStreamReader(context.openFileInput("Timings.csv"));
			BufferedReader reader = new BufferedReader(is);
			String line;
			while ((line = reader.readLine()) != null) {
				String[] lineParts = line.split(",");
				int busstopID = Integer.parseInt(lineParts[0]);
				BusStop bs = busStops.getStopById(busstopID);
				String day = lineParts[1];
				Date time = sdf.parse(lineParts[2]);
				double waitTime = Double.parseDouble(lineParts[3]);
				WaitingTime wt = new WaitingTime(bs, day, time, waitTime);
				listOfWaitingTime.add(wt);				
			}
			
			Log.v("isbtracker.BusStops", "Read bus stop file");
			
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public double getEstimatedTime(Date date, int bs) {
		double result = 0; 
		SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE", Locale.US); 
		SimpleDateFormat dfTimeOnly = new SimpleDateFormat("HH:mm:ss", Locale.US);
		for(WaitingTime wt : listOfWaitingTime) {
			String asWeek = dateFormat.format(date);
			if(asWeek.compareTo(wt.day) == 0) {
				String timeOnlyS = dfTimeOnly.format(date);
				try {
					Date timeOnly = dfTimeOnly.parse(timeOnlyS);
					if (Math.abs(timeOnly.getTime() - wt.time.getTime()) >= 15*60*1000) {
						return wt.waitTime;
					}
				} catch (ParseException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		return result;
	}
}
