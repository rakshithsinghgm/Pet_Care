package com.example.pet_care_final;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Message;
import android.util.Log;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class PetCareDataService extends WearableListenerService
        implements SensorEventListener {

    private static final String TAG = "pet_care_final";
    // send this to tell the watch to start data collection
    public static final String START_DATA_COLLECTION_PATH = "/pet-care-sensor-data-start";

    // send this to tell the watch to stop data collection
    public static final String STOP_DATA_COLLECTION_PATH = "/pet-care-sensor-data-stop";

    // send this to tell the watch to publish (and reset) its current data
    public static final String REQUEST_DATA_PATH = "/pet-care-sensor-data-request";

    // the watch will publish to this path after it receives a publish command
    public static final String RESPONSE_DATA_PATH = "/pet-care-sensor-data-response";

    SensorManager _sm;
    Sensor _gyroscope;
    Sensor _linear_accelerometer;
    TfModel _tf;
    int _lastPredictionTime = 0;
    final int PREDICTION_INTERVAL_SECS = 1;  // how frequently a prediction is made
    static final int NUM_CLASSES = 3;   // number of predicted classes
    int[] _stats = new int[NUM_CLASSES];

    private SensorDataAccumulator _linaccelData = new SensorDataAccumulator();
    private SensorDataAccumulator _gyroscopeData = new SensorDataAccumulator();

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "MsgSvc created");
        // only seems to be created once/infrequently, so we can do some heavier work here

        // register sensors
        this._sm = (SensorManager) getSystemService ( Context.SENSOR_SERVICE );
        this._gyroscope = this._sm.getDefaultSensor (Sensor.TYPE_GYROSCOPE);
        this._linear_accelerometer = this._sm.getDefaultSensor (Sensor.TYPE_LINEAR_ACCELERATION);

        this.startCollection();
    }

    void startCollection() {

        this._sm.registerListener( this, this._gyroscope, SensorManager.SENSOR_DELAY_NORMAL );
        this._sm.registerListener( this, this._linear_accelerometer, SensorManager.SENSOR_DELAY_NORMAL );
    }

    void stopCollection() {

        this._sm.unregisterListener( this );    // covers all sensors
    }

    void publishData() {

        // serialize data
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(this._stats);
            byte[] data= bos.toByteArray();

            // reset stats
            this._stats = new int[NUM_CLASSES];

            new NodeMessageBroadcasterTask( this.getApplicationContext(), RESPONSE_DATA_PATH, data ).execute();

        } catch ( Exception ex ) {
            ex.printStackTrace();
        }

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {

        super.onMessageReceived(messageEvent);

        Log.d(TAG, "onMessageReceived: " + messageEvent);
        final String msgPath = messageEvent.getPath();

        if ( msgPath.equals(START_DATA_COLLECTION_PATH) ) {
            Log.d(TAG,"Starting data collection");
            this.startCollection();
        }
        else if ( msgPath.equals(STOP_DATA_COLLECTION_PATH) ) {
            Log.d(TAG,"Stopping data collection");
            this.stopCollection();
        }
        else if ( msgPath.equals(REQUEST_DATA_PATH )) {
            Log.d(TAG,"Data request received, publishing data");
            this.publishData();
        }
        else {
            Log.d(TAG,"Unsupported path: " + msgPath );
        }

        // Example: start UI activity
        /*
        if (messageEvent.getPath().equals(START_ACTIVITY_PATH)) {
            Intent startIntent = new Intent(this, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(startIntent);
        }
         */
    }

    @Override
    public void onSensorChanged( SensorEvent event ) {

        final int currentSeconds = (int) ( System.currentTimeMillis() / 1000);

        if ( this._lastPredictionTime == 0 )    // will be true on first sensor reading
            this._lastPredictionTime = currentSeconds;

        // has enough time elapsed to predict?
        if ( currentSeconds >= ( this._lastPredictionTime + PREDICTION_INTERVAL_SECS ) ) {

            // reset last prediction time
            this._lastPredictionTime = currentSeconds;

            // perform prediction
            try {
                ArrayList<Float> stats = this._linaccelData.getStats();
                if ( !stats.addAll( this._gyroscopeData.getStats() ))
                    throw new Exception("Error combining sensor stats");

                // load tf on demand
                if ( this._tf == null )
                    this._tf = new TfModel(this.getApplicationContext());

                int classIdx = this._tf.predict( stats );

                this._stats[classIdx] += PREDICTION_INTERVAL_SECS;

            } catch ( Exception ex ) {
                Log.e(TAG, ex.getMessage());
            }

            // reset data collectors
            this._linaccelData = new SensorDataAccumulator();
            this._gyroscopeData = new SensorDataAccumulator();
        }

        switch ( event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                this._linaccelData.append(event.values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                this._gyroscopeData.append(event.values);
                break;
            default:
                Log.e(TAG, "Unknown data event Type = " + event.sensor.getType());
        }
            
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {}
}   // PetCareDataService
