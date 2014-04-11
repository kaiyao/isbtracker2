/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nus.cs4222.isbtracker;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import com.nus.cs4222.isbtracker.R;

import android.support.v4.app.FragmentActivity;

/**
 * Sample application that demonstrates the use of
 * ActivityRecognitionClient}. It registers for activity detection updates
 * at a rate of 20 seconds, logs them to a file, and displays the detected
 * activities with their associated confidence levels.
 * <p>
 * An IntentService receives activity detection updates in the background
 * so that detection can continue even if the Activity is not visible.
 */
public class MainActivity extends FragmentActivity {
    private static final String LOGTAG = MainActivity.class.getName();
	
	private LocationHelper locationGetter;
	private StateMachine stateMachine;
	
	private TextView currentStateTextView;
	private TextView logTextView;

    /*
     *  Intent filter for incoming broadcasts from the
     *  IntentService.
     */
    IntentFilter mBroadcastFilter;

    /*
     * Set main UI layout, get a handle to the ListView for logs, and create the broadcast
     * receiver.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the main layout
        setContentView(R.layout.activity_main);
        
        currentStateTextView = (TextView) findViewById(R.id.current_state_textview);
        logTextView = (TextView) findViewById(R.id.log_textview);
        
    }

    /*
     * Register the broadcast receiver and update the log of activity updates
     */
    @Override
    protected void onResume() {
        super.onResume();
    }

    /*
     * Create the menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }

    /*
     * Handle selections from the menu
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle item selection
        switch (item.getItemId()) {

            // Clear the log display and remove the log files
            case R.id.menu_item_clearlog:
                
            	logTextView.setText("Log:");
            	
                // Continue by passing true to the menu handler
                return true;

            // Display the update log
            case R.id.menu_item_showlog:

                // Update the ListView from log files
                //updateActivityHistory();
            	
            	logTextView.setText(logTextView.getText() + "\nMenu item does nothing");

                // Continue by passing true to the menu handler
                return true;

            // For any other choice, pass it to the super()
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }
    
    public void startTracking(View v){
    	Log.v(LOGTAG, "Start Tracking");
    	stateMachine = StateMachine.getInstance(this);
    	stateMachine.setListener(new StateMachineListener(){

			@Override
			public void onStateMachineChanged() {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						switch(stateMachine.getCurrentState()){
						case Elsewhere:
							currentStateTextView.setText("Elsewhere");
							break;
						case OnBus:
							currentStateTextView.setText("On Bus");
							break;
						case PossiblyOnBus:
							currentStateTextView.setText("Possibly On Bus");
							break;
						case PossiblyWaitingForBus:
							currentStateTextView.setText("Possibly Waiting For Bus");
							break;
						case WaitingForBus:
							currentStateTextView.setText("Waiting For Bus");
							break;
						default:
							break;
						}
					}
				});
			}

			@Override
			public void onLogMessage(final String message) {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
					@Override
					public void run() {
						String timeString = new SimpleDateFormat("y-M-d H:m:s", Locale.US).format(new Date());
						String desiredText = logTextView.getText() + "\n" + timeString + " " + message;
						if (desiredText.length() > 5000) {
							desiredText = desiredText.substring(desiredText.length() - 5000);
						}
						logTextView.setText(desiredText);
					}
				});
			}
    		
    	});    	
    	
    	stateMachine.startTracking();
    	locationGetter = new LocationHelper(this);
    	
    }
    
    public void stopTracking(View v){
    	Log.v(LOGTAG, "Stop Tracking");
    	stateMachine.stopTracking();
    }
    
    public void getLastLocation(View v) {    	
    	locationGetter.getLastLocation();
    }
    
    public void getCurrentLocation(View v) {
    	locationGetter.getCurrentLocation();
    }
    
    public void getContinuousLocation(View v) {    	
    	locationGetter.getContinuousLocation();
    }
}
