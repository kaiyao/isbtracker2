package com.nus.cs4222.isbtracker;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

public class QueryActivity extends FragmentActivity {
	
	private BusStops busStops;
	private Spinner busStopsSpinner;
	
	private Spinner daySpinner;
	
	private TimePicker startTimePicker;
	
	private TextView waitingTimeTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_query);
		
		busStops = new BusStops();
		
		busStopsSpinner = (Spinner) findViewById(R.id.bus_stops_spinner);
		List<String> list = new ArrayList<String>();
		for (BusStop bs : busStops.listOfStops){
			list.add(bs.getName());
		}
		ArrayAdapter<String> dataAdapter = new ArrayAdapter<String>(this,
			android.R.layout.simple_spinner_item, list);
		dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		busStopsSpinner.setAdapter(dataAdapter);
		
		daySpinner = (Spinner) findViewById(R.id.day_spinner);
		List<String> dayList = new ArrayList<String>();
		dayList.add("Sunday");
		dayList.add("Monday");
		dayList.add("Tuesday");
		dayList.add("Wednesday");
		dayList.add("Thursday");
		dayList.add("Friday");
		dayList.add("Saturday");
		ArrayAdapter<String> dataAdapter1 = new ArrayAdapter<String>(this,
			android.R.layout.simple_spinner_item, dayList);
		dataAdapter1.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		daySpinner.setAdapter(dataAdapter1);
		
		startTimePicker = (TimePicker) findViewById(R.id.start_time_picker);
		
		waitingTimeTextView = (TextView) findViewById(R.id.waiting_time);
	}

	
	/*
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.query, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	*/
}
