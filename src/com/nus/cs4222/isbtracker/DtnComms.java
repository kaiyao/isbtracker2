package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import nus.dtn.api.fwdlayer.ForwardingLayerException;
import nus.dtn.api.fwdlayer.ForwardingLayerInterface;
import nus.dtn.api.fwdlayer.ForwardingLayerProxy;
import nus.dtn.api.fwdlayer.MessageListener;
import nus.dtn.middleware.api.DtnMiddlewareInterface;
import nus.dtn.middleware.api.DtnMiddlewareProxy;
import nus.dtn.middleware.api.MiddlewareEvent;
import nus.dtn.middleware.api.MiddlewareListener;
import nus.dtn.util.Descriptor;
import nus.dtn.util.DtnMessage;
import nus.dtn.util.DtnMessageException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by khteo on 4/23/14.
 */
public class DtnComms {
    private static final String LOGTAG = DtnComms.class.getSimpleName();

    private static final long UPDATE_INTERVAL_MILLIS = 20000;

    private Context mContext;

    private DtnMiddlewareInterface middleware;
    private ForwardingLayerInterface fwdLayer;
    private Descriptor descriptor;

    // To perform DTN broadcasts periodically on another thread
    HandlerThread dtnUpdateThread;
    Handler handler;

    AtomicBoolean mIsStarted;

    public DtnComms() {
        mContext = ApplicationContext.get();
        mIsStarted = new AtomicBoolean();
    }

    public void start() {
        // Don't start the middleware again if it is already started
        if (mIsStarted.get()) return;

        try {
            // Start the middleware
            middleware = new DtnMiddlewareProxy(mContext);
            middleware.start(new MiddlewareListener() {
                public void onMiddlewareEvent(MiddlewareEvent event) {
                    try {

                        // Check if the middleware failed to start
                        if (event.getEventType() != MiddlewareEvent.MIDDLEWARE_STARTED) {
                            throw new Exception("Middleware failed to start, is it installed?");
                        }

                        // Get the fwd layer API
                        fwdLayer = new ForwardingLayerProxy(middleware);

                        // Get a descriptor for this user
                        // Typically, the user enters the username, but here we simply use IMEI number
                        TelephonyManager telephonyManager =
                                (TelephonyManager) mContext.getSystemService(Context.TELEPHONY_SERVICE);
                        descriptor = fwdLayer.getDescriptor("com.nus.cs4222.isbtracker", telephonyManager.getDeviceId());

                        // Set the broadcast address
                        fwdLayer.setBroadcastAddress("com.nus.cs4222.isbtracker", "everyone");

                        // Create HandlerThread for broadcasts
                        dtnUpdateThread = new HandlerThread("DtnUpdateThread");
                        dtnUpdateThread.start();
                        handler = new Handler(dtnUpdateThread.getLooper());
                        handler.post(broadcastTimings);

                        // TODO: Implement this
                        // Register a listener for received messages
                        //ChatMessageListener messageListener = new ChatMessageListener();
                        //fwdLayer.addMessageListener(descriptor, messageListener);
                    } catch (Exception e) {
                        Log.e(LOGTAG, "Exception in middleware start listener", e);
                    }
                }
            });

            mIsStarted.set(true);
        } catch (Exception e) {
            // Log the exception
            Log.e(LOGTAG, "Exception in onCreate()", e);
        }
    }

    public void stop() {
        if (!mIsStarted.get()) return;

        try {
            mIsStarted.set(false);
            handler.removeCallbacks(broadcastTimings);
            handler = null;
            dtnUpdateThread.quit();
            dtnUpdateThread = null;
            middleware.stop();
        } catch (Exception e) {
            Log.e(LOGTAG, "Exception on stopping middleware", e);
        }
    }

    private Runnable broadcastTimings = new Runnable() {
        @Override
        public void run() {
            // This may be an unnecessary check; do it for paranoia
            if (mIsStarted.get()) {
                Log.d(LOGTAG, "DTN broadcast");
                // TODO: Send broadcast message here
                DtnMessage message = new DtnMessage();
                
                ServerSideComms comm = new ServerSideComms();
                
                
                // Data part
                try {
					message.addData()
					.writeLong(comm.getLastUpdated().getTime())
					.addFile(new File(mContext.getExternalFilesDir(null), "Timings.csv"));
				} catch (DtnMessageException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                try {
					fwdLayer.sendMessage ( descriptor , message , "everyone" , null );
				} catch (ForwardingLayerException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
                
                
                Toast.makeText(ApplicationContext.get(),
                        "DTN broadcast", Toast.LENGTH_SHORT).show();
                handler.postDelayed(broadcastTimings, UPDATE_INTERVAL_MILLIS);
            }
        }
    };
    
    /** Listener for received chat messages. */
    private class DataMessageListener 
        implements MessageListener {

        /** {@inheritDoc} */
        public void onMessageReceived ( String source , 
                                        String destination , 
                                        DtnMessage message ) {

            try { 

                // Read the DTN message
                // Data part
                message.switchToData();
                Long remoteLastUpdatedTimeLong = message.readLong();
                Date remoteLastUpdatedTime = new Date(remoteLastUpdatedTimeLong);
                int numFiles = message.getNumFiles();
                File f = message.getFile(0);
                
                ServerSideComms comm = new ServerSideComms();
                if (comm.getLastUpdated().before(remoteLastUpdatedTime)){
        			File destFile = new File(mContext.getExternalFilesDir(null), "Timings.csv");
        			copy(f, destFile);
                }
                

                // Update the text view in Main UI thread
                createToast ("Received updated timings from " + source);
            }
            catch ( Exception e ) {
                // Log the exception
                Log.e ( "BroadcastApp" , "Exception on message event" , e );
                // Tell the user
                createToast ( "Exception on message event, check log" );
            }
        }
    }
    
    public void copy(File src, File dst) throws IOException {
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dst);

        // Transfer bytes from in to out
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }
    
    /** Helper method to create toasts. */
    private void createToast ( String toastMessage ) {

        // Use a 'final' local variable, otherwise the compiler will complain
        final String toastMessageFinal = toastMessage;

        // Post a runnable in the Main UI thread
        handler.post ( new Runnable() {
                @Override
                public void run() {
                    Toast.makeText ( mContext , 
                                     toastMessageFinal , 
                                     Toast.LENGTH_SHORT ).show();
                }
            } );
    }
}
