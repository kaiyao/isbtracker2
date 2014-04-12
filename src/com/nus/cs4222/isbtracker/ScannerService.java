package com.nus.cs4222.isbtracker;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class ScannerService extends Service {
    private static final String LOGTAG = ScannerService.class.getName();

    private final IBinder mBinder = new ScannerBinder();
    private boolean mIsBound;

    public class ScannerBinder extends Binder {
        ScannerService getService() {
            return ScannerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        mIsBound = true;
        Log.d(LOGTAG, "onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        mIsBound = true;
        Log.d(LOGTAG, "onRebind");
    }

    @Override
    public boolean onUnbind(Intent intent) {
        mIsBound = false;
        Log.d(LOGTAG, "onUnbind");
        // Allow rebinding by returning true
        return true;
    }
}
