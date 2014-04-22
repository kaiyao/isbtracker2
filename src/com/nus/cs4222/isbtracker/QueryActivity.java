package com.nus.cs4222.isbtracker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
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
		
		busStops = BusStops.getInstance();
		
		busStopsSpinner = (Spinner) findViewById(R.id.bus_stops_spinner);
		List<String> list = new ArrayList<String>();
		for (BusStop bs : busStops.getListOfStops()){
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
		
		waitingTimeTextView = (TextView) findViewById(R.id.waiting_time_textview);
	}

	public void queryWaitingTime(View v){
		WaitingTimes wt = new WaitingTimes();
		
		BusStop selectedBusStop = null;
		String selectedBusStopString = (String) busStopsSpinner.getSelectedItem();
		for (BusStop bs : busStops.getListOfStops()){
			if (bs.getName().equalsIgnoreCase(selectedBusStopString)){
				selectedBusStop = bs;
				break;
			}
		}
		
		String selectedDay = (String) daySpinner.getSelectedItem();		
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("hh:mm");
			Date selectedTime;
			selectedTime = sdf.parse(startTimePicker.getCurrentHour() + ":" + startTimePicker.getCurrentMinute());
			waitingTimeTextView.setText("Avg: " + wt.getEstimatedTime(selectedBusStop.getId(), selectedDay, selectedTime));
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
				
		
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
