package com.example.pet_care_final;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Map;

public class PetCareSensorListener
        implements SensorEventListener
        , AutoCloseable {

    SensorManager _sm;
    Sensor _sensor;

    static final String TAG = "pet_care_final";
    // ArrayList<Float> _data = new ArrayList<>();

    // list of times; parallel to magnitudes
    public ArrayList<Long> times = new ArrayList<>();

    // list of magnitudes; parallel to times
    public ArrayList<Float> magnitudes = new ArrayList<>();

    long _lastSensorCollectionSecs = 0;
    static final int COLLECTION_INTERVAL_SECS = 1;//#seconds

    //boolean _isEnabled = false;
    // Context _cx;

    public PetCareSensorListener( Context cx, int sensorType ) {

        this._sm = (SensorManager) cx.getSystemService(Context.SENSOR_SERVICE);
        this._sensor = this._sm.getDefaultSensor(sensorType);
        this._sm.registerListener( this, this._sensor, SensorManager.SENSOR_DELAY_NORMAL );
        //this._isEnabled=true;

    }

    public boolean isEmpty() {
        return times.isEmpty();
    }

    public void clearData() {
        times.clear();
        magnitudes.clear();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if ( event.sensor.getType() == this._sensor.getType() ) {

            float[] vals = event.values;

            times.add(SystemClock.elapsedRealtime() / 1000 );

            Float mag = (float) Math.sqrt(vals[0] * vals[0] + vals[1] * vals[1] + vals[2] * vals[2]);
            magnitudes.add(mag);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}

    @Override
    public void close() throws Exception {
        if ( this._sm != null )
            this._sm.unregisterListener( this, this._sensor );
        //this._isEnabled=false;
    }
}

