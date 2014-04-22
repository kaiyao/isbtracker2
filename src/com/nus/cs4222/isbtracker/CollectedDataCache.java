package com.nus.cs4222.isbtracker;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.Context;
import android.text.SpannedString;
import android.text.format.Time;

public class CollectedDataCache {
	
	private static CollectedDataCache theOne;
	private Context mContext;
	
	private CollectedDataCache(){
		mContext = ApplicationContext.get();
	}
	
	public static CollectedDataCache getInstance(){
		if (theOne == null){
			theOne = new CollectedDataCache();
		}
		return theOne;
	}
	
	// Prevent simultaneous thread access to logger
	public synchronized void logWaitingTime(BusStop bs, Float waitingTime) {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
		
		File f = new File(mContext.getFilesDir(), "WaitingTimeLog.csv");
		try {
			PrintWriter w = new PrintWriter(new FileWriter(f, true));
			w.println(String.format("%s,%f,%d", sdf.format(new Date()), waitingTime, bs.getId()));
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public synchronized void uploadWaitingTime(){
		ServerSideComms comm = new ServerSideComms();
		
		try {
			File f = new File(mContext.getFilesDir(), "WaitingTimeLog.csv");
			BufferedReader reader = new BufferedReader(new FileReader(f));		
		
			String line;
			while ((line = reader.readLine()) != null) {
			    String[] lineParts = line.split(",");
			    comm.pushData(Integer.valueOf(lineParts[2]), lineParts[0], Double.valueOf(lineParts[1]));
			}
			
			reader.close();
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	

}
