package com.nus.cs4222.isbtracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.text.format.Time;

public class DataSource {

	// Database fields
	private SQLiteDatabase database;
	private DatabaseHelper dbHelper;
	private HashMap<Integer, BusStop> busStops;
	
	private String[] allColumns_BusStop = { DatabaseHelper.KEY_BS_ID,
			DatabaseHelper.KEY_BS_LONG, DatabaseHelper.KEY_BS_LAT };
	private String[] allColumns_Trip = { DatabaseHelper.KEY_TRIP_START,
			DatabaseHelper.KEY_TRIP_STARTBS, DatabaseHelper.KEY_TRIP_END,
			DatabaseHelper.KEY_TRIP_ENDBS};
	private String[] allColumns_WaitingTime = { DatabaseHelper.KEY_WAIT_TIME,
			DatabaseHelper.KEY_BS_ID };

	public DataSource(Context context) {
		dbHelper = new DatabaseHelper(context);
		busStops = new HashMap<Integer, BusStop>();
	}

	public void open() throws SQLException {
		database = dbHelper.getWritableDatabase();
	}

	public void close() {
		dbHelper.close();
	}

	public BusStop createBusStop(int id, long lat, long longi) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.KEY_BS_ID, id);
		values.put(DatabaseHelper.KEY_BS_LAT, lat);
		values.put(DatabaseHelper.KEY_BS_LONG, longi);
		long insertId = database.insert(DatabaseHelper.TABLE_BUSSTOP, null,
				values);
		Cursor cursor = database.query(DatabaseHelper.TABLE_BUSSTOP,
				allColumns_BusStop, DatabaseHelper.KEY_BS_ID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		BusStop newBusStop = cursorToBusStop(cursor);
		cursor.close();
		return newBusStop;
	}
	
	public TripSegment createTripSegment(int startId, Time start, int endId, Time end) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.KEY_TRIP_START, start.format("%F %T"));
		values.put(DatabaseHelper.KEY_TRIP_STARTBS, startId);
		values.put(DatabaseHelper.KEY_TRIP_END, start.format("%F %T"));
		values.put(DatabaseHelper.KEY_TRIP_ENDBS, endId);
		long insertId = database.insert(DatabaseHelper.TABLE_TRIPS, null,
				values);
		Cursor cursor = database.query(DatabaseHelper.TABLE_TRIPS,
				allColumns_Trip, DatabaseHelper.KEY_ROWID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		TripSegment newTripSegment = cursorToTripSegment(cursor);
		cursor.close();
		return newTripSegment;
	}
	
	public WaitingTime createWaitingTime(int waitTime, int id) {
		ContentValues values = new ContentValues();
		values.put(DatabaseHelper.KEY_WAIT_TIME, waitTime);
		values.put(DatabaseHelper.KEY_BS_ID, id);
		long insertId = database.insert(DatabaseHelper.TABLE_BUSSTOPWAIT, null,
				values);
		Cursor cursor = database.query(DatabaseHelper.TABLE_BUSSTOPWAIT,
				allColumns_WaitingTime, DatabaseHelper.KEY_ROWID + " = " + insertId, null,
				null, null, null);
		cursor.moveToFirst();
		WaitingTime newWaitingTime = cursorToWaitingTime(cursor);
		cursor.close();
		return newWaitingTime;
	}

//	public void deleteComment(Comment comment) {
//		long id = comment.getId();
//		System.out.println("Comment deleted with id: " + id);
//		database.delete(DatabaseHelper.TABLE_COMMENTS, DatabaseHelper.COLUMN_ID
//				+ " = " + id, null);
//	}

	public List<BusStop> getAllBusStops() {
		List<BusStop> comments = new ArrayList<BusStop>();

		Cursor cursor = database.query(DatabaseHelper.TABLE_BUSSTOP,
				allColumns_BusStop, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			BusStop comment = cursorToBusStop(cursor);
			comments.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return comments;
	}
	
	public List<TripSegment> getAllTrips() {
		List<TripSegment> comments = new ArrayList<TripSegment>();

		Cursor cursor = database.query(DatabaseHelper.TABLE_TRIPS,
				allColumns_Trip, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			TripSegment comment = cursorToTripSegment(cursor);
			comments.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return comments;
	}
	
	public List<WaitingTime> getAllWaitingTime() {
		List<WaitingTime> comments = new ArrayList<WaitingTime>();

		Cursor cursor = database.query(DatabaseHelper.TABLE_BUSSTOPWAIT,
				allColumns_WaitingTime, null, null, null, null, null);
		cursor.moveToFirst();
		while (!cursor.isAfterLast()) {
			WaitingTime comment = cursorToWaitingTime(cursor);
			comments.add(comment);
			cursor.moveToNext();
		}
		// Make sure to close the cursor
		cursor.close();
		return comments;
	}
	
	private BusStop cursorToBusStop(Cursor cursor) {
		float lat = cursor.getFloat(0);
		float longi = cursor.getFloat(1);
		int id = cursor.getInt(2);
		BusStop busstop;
		if(!busStops.containsKey(id)) {
			busstop = new BusStop(id, lat, longi);
			busStops.put(id, busstop);
		} else {
			busstop = busStops.get(id);
		}
		return busstop;
	}
	
	private TripSegment cursorToTripSegment(Cursor cursor) {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String start = cursor.getString(0);
		int startBS = cursor.getInt(1);
		String end = cursor.getString(2); 
		int endBS = cursor.getInt(3);
		Date startTime = null, endTime = null;
		BusStop bsStart = busStops.get(startBS);
		BusStop bsEnd = busStops.get(endBS);
		try {
			startTime = df.parse(start);
			endTime = df.parse(end);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		TripSegment ts = new TripSegment(bsStart, startTime, bsEnd, endTime);
		return ts;
	}
	
	private WaitingTime cursorToWaitingTime(Cursor cursor) {
		int waitTime = cursor.getInt(0);
		int busstop = cursor.getInt(1);
		BusStop bs = busStops.get(busstop);
		WaitingTime wt = new WaitingTime(bs, waitTime);
		return wt;
	}

}