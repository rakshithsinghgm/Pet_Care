package com.example.pet_care_final;

import android.content.Context;
import android.os.PowerManager;

public class PetCareWakeLock implements AutoCloseable {

    public static final String TAG = "PetCareWakeLock";
    PowerManager.WakeLock _wakeLock;

    // create/acquire an infinite partial wakelock
    public PetCareWakeLock( Context cx, String tag ) {
        PowerManager pwm = (PowerManager)cx.getSystemService(Context.POWER_SERVICE);
        this._wakeLock = pwm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag );
        this._wakeLock.acquire();
    }

    @Override
    public void close() throws Exception {
        if ( this._wakeLock != null ) {
            _wakeLock.release();
            _wakeLock=null;
        }
    }
}
