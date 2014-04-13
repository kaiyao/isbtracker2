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

import android.content.*;
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
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
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
    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1337;

    private ServiceConnection mConnection;
    private ScannerService mService;
    private boolean mIsBound;

	private LocationHelper locationGetter;
	
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

        checkForPlayServices();
    }

    /*
     * Check that Google Play services is available
     */
    private void checkForPlayServices() {
        int status = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        // If Google Play services is not available
        if (status != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(status)) {
                GooglePlayServicesUtil.getErrorDialog(status, this,
                        REQUEST_GOOGLE_PLAY_SERVICES).show();
            } else {
                Toast.makeText(this, "This device is not supported.", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_GOOGLE_PLAY_SERVICES:
                if (resultCode == RESULT_CANCELED) {
                    Toast.makeText(this, "Google Play Services must be installed.",
                            Toast.LENGTH_SHORT).show();
                    finish();
                }
                return;
        }
        super.onActivityResult(requestCode, resultCode, data);
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

        mConnection = new ServiceConnection() {
           @Override
           public void onServiceConnected(ComponentName name, IBinder service) {
               ScannerService.ScannerBinder binder = (ScannerService.ScannerBinder) service;
               mService = binder.getService();
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

    public void stopService(View v) {
        Intent intent = new Intent(this, ScannerService.class);
        stopService(intent);
        Log.d(LOGTAG, "Stop service");
    }

    public void startTracking(View v) {
        if (!mIsBound) {
            return;
        }

        mService.startTracking(new StateMachineListener(){

			@Override
			public void onStateMachineChanged(final StateMachine.State state) {
				new Handler(Looper.getMainLooper()).post(new Runnable() {
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

    	locationGetter = new LocationHelper(this);

        Log.v(LOGTAG, "Start Tracking");
    }
    
    public void stopTracking(View v) {
        if (!mIsBound) {
            return;
        }

    	mService.stopTracking();
        Log.v(LOGTAG, "Stop Tracking");
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

    /*
    private class ServiceBroadcastReceiver extends BroadcastReceiver {
        private boolean mReceiverIsRegistered;

        public void register() {
            if (!mReceiverIsRegistered) {
                registerReceiver(this, new IntentFilter(ScannerService.MESSAGE_TOPIC));
                mReceiverIsRegistered = true;
            }
        }

        public void unregister() {
            if (mReceiverIsRegistered) {
                unregisterReceiver(this);
                mReceiverIsRegistered = false;
            }
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (action.equals(ScannerService.MESSAGE_TOPIC)) {

                String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                if (subject.equals("Scanner")) {
                    if (intent.hasExtra("fixes")) {
                        mGpsFixes = intent.getIntExtra("fixes", 0);
                        mGpsSats = intent.getIntExtra("sats",0);
                    }
                    else if (intent.hasExtra("enable")) {
                        int enable = intent.getIntExtra("enable", -1);

                        if (mConnectionRemote != null) {
                            try {
                                if (enable == 1) {
                                    Log.d(LOGTAG, "Enabling scanning");
                                    mConnectionRemote.startScanning();
                                } else if (enable == 0) {
                                    Log.d(LOGTAG, "Disabling scanning");
                                    mConnectionRemote.stopScanning();
                                }
                            } catch (RemoteException e) {
                                Log.e(LOGTAG, "", e);
                            }
                        }
                    }

                    updateUI();
                    Log.d(LOGTAG, "Received a scanner intent...");
                    return;
                }
                if (subject.equals("WifiScanner")||subject.equals("GPSScanner")||subject.equals("CellScanner")) {
                    // We know and expect those to appear - they can be safely ignored.
                    return;
                }
                Log.e(LOGTAG, "", new IllegalArgumentException("Unknown scanner message: " + subject));
                return;
            }
        }
    }*/
}
