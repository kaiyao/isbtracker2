package com.nus.cs4222.isbtracker;

import android.content.Context;
import android.telephony.TelephonyManager;
import android.util.Log;
import nus.dtn.api.fwdlayer.ForwardingLayerInterface;
import nus.dtn.api.fwdlayer.ForwardingLayerProxy;
import nus.dtn.middleware.api.DtnMiddlewareInterface;
import nus.dtn.middleware.api.DtnMiddlewareProxy;
import nus.dtn.middleware.api.MiddlewareEvent;
import nus.dtn.middleware.api.MiddlewareListener;
import nus.dtn.util.Descriptor;

/**
 * Created by khteo on 4/23/14.
 */
public class DtnComms {
    private static final String LOGTAG = DtnComms.class.getSimpleName();

    private Context mContext;

    private DtnMiddlewareInterface middleware;
    private ForwardingLayerInterface fwdLayer;
    private Descriptor descriptor;

    boolean mIsStarted;

    public DtnComms() {
        mContext = ApplicationContext.get();
        mIsStarted = false;
    }

    public void start() {
        // Don't start the middleware again if it is already started
        if (mIsStarted) return;

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

                        // Register a listener for received messages
                        //ChatMessageListener messageListener = new ChatMessageListener();
                        //fwdLayer.addMessageListener(descriptor, messageListener);
                    } catch (Exception e) {
                        Log.e(LOGTAG, "Exception in middleware start listener", e);
                    }
                }
            });

            mIsStarted = true;
        } catch (Exception e) {
            // Log the exception
            Log.e(LOGTAG, "Exception in onCreate()", e);
        }
    }

    public void stop() {
        if (!mIsStarted) return;

        try {
            middleware.stop();
            mIsStarted = false;
        } catch (Exception e) {
            Log.e(LOGTAG, "Exception on stopping middleware", e);
        }
    }
}
