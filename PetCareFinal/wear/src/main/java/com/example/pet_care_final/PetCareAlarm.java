package com.example.pet_care_final;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.SystemClock;
import android.util.Log;

public class PetCareAlarm implements AutoCloseable {

    static final String TAG = "PetCareAlarm";
    static final String INTENT_NAME = "com.example.pet_care_final";

    PendingIntent _pi;
    AlarmManager _mgr;
    Runnable _intervalEvent = null;
    Context _cx;

    BroadcastReceiver _recv = new BroadcastReceiver() {

        @Override
        public void onReceive(Context c, Intent i) {

            if ( _intervalEvent != null )
                _intervalEvent.run();

            setAlarm();
        }
    };

    public PetCareAlarm( Context cx, Runnable intervalEvent ) {

        this._intervalEvent = intervalEvent;
        _cx = cx;

        // alarm mgr
        _cx.registerReceiver(_recv, new IntentFilter( TAG ));
        _pi = PendingIntent.getBroadcast(cx, 0, new Intent( TAG ),0);
        _mgr = (AlarmManager) cx.getSystemService(Context.ALARM_SERVICE);

        setAlarm();
    }

    void setAlarm() {
        _mgr.set( AlarmManager.ELAPSED_REALTIME_WAKEUP, SystemClock.elapsedRealtime(), _pi );
    }

    @Override
    public void close() throws Exception {
        if ( _mgr != null ) {
            _mgr.cancel(_pi);
            _cx.unregisterReceiver(_recv);
        }
    }
}
