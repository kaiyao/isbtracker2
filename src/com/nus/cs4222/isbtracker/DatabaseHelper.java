package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {

	private static final String DATABASE_NAME = "busstops.db";
	private static final int DATABASE_VERSION = 1;

	//table names
	public static final String TABLE_BUSSTOP = "busstops";
	public static final String TABLE_TRIPS = "trips";
	public static final String TABLE_BUSSTOPWAIT = "timewait";
	
	//common names for tables
	public static final String KEY_ROWID = "rowid";
	public static final String KEY_BS_ID = "id";
		
	//column names for busstop
	public static final String KEY_BS_LONG = "longitude";
	public static final String KEY_BS_LAT = "latitude";
	
	//column names for trips
	public static final String KEY_TRIP_START = "start_time";
	public static final String KEY_TRIP_STARTBS = "start_busstop";
	public static final String KEY_TRIP_END = "end_time";
	public static final String KEY_TRIP_ENDBS = "end_busstop";
	
	//column name for waiting time
	public static final String KEY_WAIT_TIME = "wait_time";
	
	// Database creation sql statement
	private static final String TABLE_CREATE_BS = "create table "
			+ TABLE_BUSSTOP + "( " + KEY_BS_LAT + " float, " 
			+ KEY_BS_LONG + " float, " 
			+ KEY_BS_ID + " integer PRIMARY KEY);";
	
	private static final String TABLE_CREATE_TRIP = "create table "
			+ TABLE_TRIPS + "( " + KEY_TRIP_START + " date, "
			+ KEY_TRIP_STARTBS + " integer, " 
			+ KEY_TRIP_END + " date, "
			+ KEY_TRIP_ENDBS + " integer, " 
			+ "FOREIGN KEY (" + KEY_TRIP_STARTBS + "  ) REFERENCES "
			+ TABLE_BUSSTOP + "(" + KEY_BS_ID + "), "
			+ "FOREIGN KEY (" + KEY_TRIP_ENDBS + "  ) REFERENCES "
			+ TABLE_BUSSTOP + "(" + KEY_BS_ID + ");";
	
	private static final String TABLE_CREATE_WAIT = "create table "
			+ TABLE_BUSSTOPWAIT + "( " + KEY_WAIT_TIME + " int, " 
			+ KEY_BS_ID + " integer REFERENCES "
			+ TABLE_BUSSTOP + "(" + KEY_BS_ID + ");";
			
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase database) {
		database.execSQL(TABLE_CREATE_BS);
		database.execSQL(TABLE_CREATE_TRIP);
		database.execSQL(TABLE_CREATE_WAIT);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		Log.w(DatabaseHelper.class.getName(),
				"Upgrading database from version " + oldVersion + " to "
						+ newVersion + ", which will destroy all old data");
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREATE_BS);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREATE_TRIP);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_CREATE_WAIT);
		onCreate(db);
	}

}