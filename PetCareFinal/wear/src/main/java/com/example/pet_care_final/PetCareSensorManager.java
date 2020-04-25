package com.example.pet_care_final;

import android.content.Context;
import android.hardware.Sensor;

public class PetCareSensorManager {

    private static final String TAG = "PetCareSensorManager";

    public PetCareSensorListener linAccelSensorListener = null;
    public PetCareSensorListener gyroscopeSensorListener = null;
    Context _cx;
    PetCareWakeLock _wl = null;
    PetCareAlarm _alarm = null;
    boolean _enabled = false;

    public PetCareSensorManager( Context cx ) {

        // init the sensors
        this._cx =cx;

        // sensors
        linAccelSensorListener = new PetCareSensorListener( this._cx, Sensor.TYPE_LINEAR_ACCELERATION );
        gyroscopeSensorListener = new PetCareSensorListener( this._cx, Sensor.TYPE_GYROSCOPE );

    }

    public boolean isEnabled() { return _enabled;}

    public void clearSensorData() {
        linAccelSensorListener.clearData();
        gyroscopeSensorListener.clearData();
    }

    public void enableSensors( Runnable alarmCallback ) throws Exception {

        // (re)create the alarm
        if ( _alarm != null )
            _alarm.close();
        _alarm = new PetCareAlarm(_cx, alarmCallback );

        // (re)create wakelock.  without an active wake lock, sensor times will be wrong
        if ( _wl != null )
            _wl.close();
        _wl = new PetCareWakeLock( this._cx, TAG );

        // enable sensors
        linAccelSensorListener.enable();
        gyroscopeSensorListener.enable();

        _enabled=true;
    }

    public void disableSensors() throws Exception {
        if ( _alarm != null ) {
            _alarm.close();
            _alarm = null;
        }

        linAccelSensorListener.disable();
        gyroscopeSensorListener.disable();

        if ( _wl != null ) {
            _wl.close();
            _wl = null;
        }

        _enabled=false;
    }
}
