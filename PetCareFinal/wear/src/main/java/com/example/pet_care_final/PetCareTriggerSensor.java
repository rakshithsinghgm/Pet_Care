package com.example.pet_care_final;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.util.Log;

public class PetCareTriggerSensor extends TriggerEventListener implements AutoCloseable {

    private static final String TAG = "PetCareTriggerSensor";
    SensorManager _sm;
    Sensor _sensor;
    Runnable _callback;

    // establishes a perpetual trigger for trigger sensor type; call close() to cancel
    public PetCareTriggerSensor( Context cx, int sensorType, Runnable callback ) {

        _callback = callback;
        _sm = (SensorManager) cx.getSystemService(Context.SENSOR_SERVICE);
        _sensor = this._sm.getDefaultSensor( sensorType );
        _sm.requestTriggerSensor( this, this._sensor );
    }

    @Override
    public void onTrigger(TriggerEvent event) {
        // Do Work.

        // As it is a one shot sensor, it will be canceled automatically.
        // SensorManager.requestTriggerSensor(this, mSigMotion); needs to
        // be called again, if needed.
        Log.d(TAG,"onTrigger: " + event.sensor.getName());

        if ( _sensor != null && _sensor.getType() == event.sensor.getType() ) {
            if ( _callback != null )
                _callback.run();
            _sm.requestTriggerSensor( this, this._sensor );
        }
    }

    @Override
    public void close() throws Exception {

        if ( _sm != null && _sensor != null ) {
            _sm.cancelTriggerSensor(this,_sensor);
            _sensor = null;
            _sm = null;
        }

    }
}
