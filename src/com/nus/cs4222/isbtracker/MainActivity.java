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

import android.content.IntentFilter;
import android.os.Bundle;
import android.text.Spanned;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
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
public class MainActivity extends FragmentActivity{
	
	private ActivityRecognitionHelper activityRecognition;
	private LocationHelper locationGetter;
	
	/*
     * Holds activity recognition data, in the form of
     * strings that can contain markup
     */
    private ArrayAdapter<Spanned> mStatusAdapter;

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
                // Clear the list adapter
                mStatusAdapter.clear();

                // Update the ListView from the empty adapter
                mStatusAdapter.notifyDataSetChanged();

                // Remove log files
                /*if (!mLogFile.removeLogFiles()) {
                    Log.e(ActivityUtils.APPTAG, getString(R.string.log_file_deletion_error));

                // Display the results to the user
                } else {

                    Toast.makeText(
                            this,
                            R.string.logs_deleted,
                            Toast.LENGTH_LONG).show();
                }*/
                // Continue by passing true to the menu handler
                return true;

            // Display the update log
            case R.id.menu_item_showlog:

                // Update the ListView from log files
                //updateActivityHistory();

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
    	Log.v("MainActivity", "Start Tracking");
    	StateMachine.getInstance(this);
    	activityRecognition = new ActivityRecognitionHelper(this);
    	activityRecognition.startUpdates();
    	//locationGetter = new LocationHelper(this);
    	
    }
    
    public void stopTracking(View v){
    	Log.v("MainActivity", "Stop Tracking");
    	activityRecognition.stopUpdates();
    	//locationGetter.stopContinousLocation();
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
