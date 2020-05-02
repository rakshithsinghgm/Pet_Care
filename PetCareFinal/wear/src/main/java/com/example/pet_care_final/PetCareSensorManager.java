package com.example.pet_care_final;

import android.content.Context;
import android.hardware.Sensor;

public class PetCareSensorManager implements AutoCloseable {

    private static final String TAG = "PetCareSensorManager";

    public PetCareSensorListener linAccelSensorListener = null;
    public PetCareSensorListener gyroscopeSensorListener = null;
    PetCareWakeLock _wl = null;
    PetCareAlarm _alarm = null;

    public PetCareSensorManager( Context cx, Runnable alarmCallback ) throws Exception {

        // (re)create the alarm
        if ( _alarm != null )
            _alarm.close();
        _alarm = new PetCareAlarm( cx, alarmCallback );

        // (re)create wakelock.  without an active wake lock, sensor times will be wrong
        if ( _wl != null )
            _wl.close();
        _wl = new PetCareWakeLock( cx, TAG );

        // enable sensors
        if ( linAccelSensorListener != null )
            linAccelSensorListener.close();
        linAccelSensorListener = new PetCareSensorListener( cx, Sensor.TYPE_LINEAR_ACCELERATION );

        if ( gyroscopeSensorListener != null )
            gyroscopeSensorListener.close();
        gyroscopeSensorListener = new PetCareSensorListener( cx, Sensor.TYPE_GYROSCOPE );
    }

    public void clearSensorData() {
        if ( linAccelSensorListener != null )
            linAccelSensorListener.clearData();

        if ( gyroscopeSensorListener != null )
            gyroscopeSensorListener.clearData();
    }

    @Override
    public void close() throws Exception {
        if ( _alarm != null ) {
            _alarm.close();
            _alarm = null;
        }

        if ( linAccelSensorListener != null ) {
            linAccelSensorListener.close();
            linAccelSensorListener = null;
        }

        if ( gyroscopeSensorListener != null ) {
            gyroscopeSensorListener.close();
            gyroscopeSensorListener = null;
        }

        if ( _wl != null ) {
            _wl.close();
            _wl = null;
        }
    }
}
