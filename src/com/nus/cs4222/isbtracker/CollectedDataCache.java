package com.nus.cs4222.isbtracker;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import android.content.Context;

public class CollectedDataCache {
	
	private CollectedDataCache theOne;
	private Context mContext;
	
	private CollectedDataCache(){
		mContext = ApplicationContext.get();
	}
	
	public CollectedDataCache getInstance(){
		if (theOne == null){
			theOne = new CollectedDataCache();
		}
		return theOne;
	}
	
	// Prevent simultaneous thread access to logger
	public synchronized void logWaitingTime(WaitingTime logItem) {
		File f = new File(mContext.getFilesDir(), "WaitingTimeLog.csv");
		try {
			PrintWriter w = new PrintWriter(new FileWriter(f, true));
			w.println(logItem.getLine());
			w.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	

}
