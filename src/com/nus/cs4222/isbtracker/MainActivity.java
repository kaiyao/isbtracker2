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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.content.*;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.nus.cs4222.isbtracker.R;
import com.nus.cs4222.isbtracker.route.BusRoutes;

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
    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private ServiceConnection mConnection;
    private ScannerService mService;
    private boolean mIsBound;

	private LocationHelper locationGetter;
	
	private TextView currentStateTextView;
	private TextView logTextView;

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

        ApplicationContext.getInstance().init(getApplicationContext());
        assert(getApplicationContext() == ApplicationContext.get());        
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
                
            case R.id.menu_item_query:
            	Intent intent = new Intent(this, QueryActivity.class);
            	startActivity(intent);
            	return true;

            // For any other choice, pass it to the super()
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    
    @Override
    public void onStart() {
        super.onStart();

        mConnection = new ServiceConnection() {
           @Override
           public void onServiceConnected(ComponentName name, IBinder service) {
               ScannerService.ScannerBinder binder = (ScannerService.ScannerBinder) service;
               mService = binder.getService();
               
               // Set state machine listener (before binding to service)
               mService.setStateMachineListener(mStateMachineListener);
               
               mIsBound = true;
               Log.d(LOGTAG, "Service connected");
           }

           @Override
           public void onServiceDisconnected(ComponentName name) {
               mService = null;
               mIsBound = false;
               Log.d(LOGTAG, "Service disconnected");
           }
        };

        // Start and bind to service
        Intent intent = new Intent(this, ScannerService.class);
        startService(intent);
        bindService(intent, mConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();

        unbindService(mConnection);
        mConnection = null;
    }

    public void startTracking(View v) {
        if (!mIsBound) {
            return;
        }

        mService.setStateMachineListener(mStateMachineListener);
        mService.startTracking();

    	locationGetter = new LocationHelper(this);

        Log.v(LOGTAG, "Start Tracking");
    }
    
    private StateMachineListener mStateMachineListener = new StateMachineListener(){
	    final Handler handler = new Handler(Looper.getMainLooper());
	    final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

		@Override
		public void onStateMachineChanged(final StateMachine.State state) {
			handler.post(new Runnable() {
				@Override
				public void run() {
					switch(state){
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
			handler.post(new Runnable() {
				@Override
				public void run() {
					String timeString = dateFormat.format(new Date());
					String desiredText = logTextView.getText() + "\n" + timeString + " " + message;
					if (desiredText.length() > 5000) {
						desiredText = desiredText.substring(desiredText.length() - 5000);
					}
					logTextView.setText(desiredText);
				}
			});
		}

	};
    
    public void stopTracking(View v) {
        if (!mIsBound) {
            return;
        }

    	mService.stopTracking();
        Log.v(LOGTAG, "Stop Tracking");
    }
    
    public void getLastLocation(View v) {    	
    	locationGetter.getLastLocation();
    	
    	/*
    	Location l = new Location("");
		l.setLatitude(1.293477);
		l.setLongitude(103.781353);
    	
    	BusRoutes br = new BusRoutes();    	
    	String message = "Distance: " + br.getDistanceFromRoutes(l);
    	String desiredText = logTextView.getText() + "\n" + message;
		logTextView.setText(desiredText);
		*/
    }
    
    public void getCurrentLocation(View v) {
    	locationGetter.getCurrentLocation();
    }
    
    public void getContinuousLocation(View v) {
    	if (locationGetter.mUpdatesRequested) {
	    	locationGetter.stopContinousLocation();
    	}else{
    		locationGetter.getContinuousLocation();
    	}
    }
    
    private class SyncDataTask extends AsyncTask<Void, Integer, Long> {
        // Do the long-running work in here
    	protected Long doInBackground(Void... params) {
    		CollectedDataCache c = CollectedDataCache.getInstance();
        	c.uploadWaitingTime();
        	
        	ServerSideComms s = new ServerSideComms();
        	s.getData();
        	
			return (long) 0;
		}

        // This is called each time you call publishProgress()
        protected void onProgressUpdate(Integer... progress) {
            
        }

        // This is called when doInBackground() is finished
        protected void onPostExecute(Long result) {
        	Toast.makeText(ApplicationContext.get(),
                    "Sync complete", Toast.LENGTH_SHORT).show();
        }		
    }
    
    public void syncData(View v){
    	new SyncDataTask().execute();
    	
    }
    
    public void onDtnTest(View v){
    	Toast.makeText(ApplicationContext.get(),
                "Test DTN", Toast.LENGTH_SHORT).show();
    	
    }
}
