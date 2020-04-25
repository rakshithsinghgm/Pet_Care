package com.example.pet_care_final;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.hardware.Sensor;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.wearable.watchface.CanvasWatchFaceService;
import android.support.wearable.watchface.WatchFaceStyle;
import android.util.Log;
import android.view.SurfaceHolder;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;

// https://developer.android.com/training/wearables/watch-faces/drawing#Variables

public class WatchFaceService
        extends CanvasWatchFaceService
{

    private static final String TAG = "pet_care_final";

    long _lastTimeTickSeconds = 0;
    long _lastMotionDetectedSeconds = 0;
    static final long COLLECTION_INTERVAL_SECS = 1;
    static final long MIN_TICK_INTERVAL_SECS = 1;

    // wait this many seconds after motion no longer detected before shutting down sensors
    static final long SENSORS_SHUTDOWN_DELAY = 60*5;    // 5 minutes
    static final int NUM_CLASSES = 3;   // number of predicted classes
    int[] _stats = new int[NUM_CLASSES];
    static final int CLASS_SLEEPING = 0;
    int _lastSensorCollectionSecs = 0;
    byte _lastActivityState = (byte)CLASS_SLEEPING;

    // flags if start/stop is requested remotely
    boolean _requestStartPending = false;
    boolean _requestStopPending = false;

    PetCareTriggerSensor _motionTrigger = null;
    PetCareSensorManager _sensorMgr = null;
    PetCareDataInference _dataInference = null;

    final MessageClient.OnMessageReceivedListener _messageService = new MessageClient.OnMessageReceivedListener() {

        // send this to tell the watch to start data collection
        static final String START_DATA_COLLECTION_PATH = "/pet-care-sensor-data-start";

        // send this to tell the watch to stop data collection
        static final String STOP_DATA_COLLECTION_PATH = "/pet-care-sensor-data-stop";

        // send this to tell the watch to publish (and reset) its current data
        static final String REQUEST_DATA_PATH = "/pet-care-sensor-data-request";

        // the watch will publish to this path after it receives a publish command
        static final String RESPONSE_DATA_PATH = "/pet-care-sensor-data-response";

        // send this to tell the watch to publish the current activity state
        static final String REQUEST_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-request";

        // the watch will publish to this path after it receives a publish command
        static final String RESPONSE_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-response";

        @Override
        public void onMessageReceived(@NonNull MessageEvent messageEvent) {

            Log.d(TAG,"Message received: " + messageEvent.getPath() );

            final String msgPath = messageEvent.getPath();

            if ( msgPath.equals(START_DATA_COLLECTION_PATH) ) {
                Log.d(TAG,"Start data collection requested");
                _requestStartPending=true;
            }
            else if ( msgPath.equals(STOP_DATA_COLLECTION_PATH) ) {
                Log.d(TAG,"Stop data collection requested");
                _requestStopPending = true;
            }
            else if ( msgPath.equals(REQUEST_DATA_PATH )) {
                Log.d(TAG,"Data request received, publishing data");
                this.publishData();
            }
            else if ( msgPath.equals(REQUEST_ACTIVITY_STATE_PATH )) {
                Log.d(TAG,"Activity state request received, publishing activity state");
                this.publishState();
            }
            else {
                Log.d(TAG,"Unsupported path: " + msgPath );
            }

        }

        void publishData() {

            // serialize data
            try {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                ObjectOutputStream oos = new ObjectOutputStream(bos);
                oos.writeObject(_stats);
                byte[] data= bos.toByteArray();

                // reset stats
                _stats = new int[NUM_CLASSES];

                new NodeMessageBroadcasterTask( getApplicationContext(), RESPONSE_DATA_PATH, data ).execute();

            } catch ( Exception ex ) {
                ex.printStackTrace();
            }

        }

        void publishState() {
            new NodeMessageBroadcasterTask( getApplicationContext(), RESPONSE_ACTIVITY_STATE_PATH, new byte[] { _lastActivityState } ).execute();
        }
    };


    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG,"onCreate");

        // activity inference
        _dataInference = new PetCareDataInference(this.getApplicationContext());

        // messaging
        Wearable.getMessageClient( getApplicationContext() ).addListener(_messageService );

        // motion trigger
        _motionTrigger = new PetCareTriggerSensor(getApplicationContext(), Sensor.TYPE_MOTION_DETECT
                , new Runnable() {
            @Override
            public void run() {
                onMotionTrigger();
            }
        }
        );

        // sensor manager
        _sensorMgr = new PetCareSensorManager( getApplicationContext() );
        try {
            _sensorMgr.enableSensors(
                    new Runnable() {
                        @Override
                        public void run() {
                            onSensorInterval();
                        }
                    }
            );

        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
    }

    void onMotionTrigger() {
        _lastMotionDetectedSeconds = Utils.getCurrentSeconds();
        Log.d(TAG,"Motion detected!");
    }

    void onSensorInterval() {

        final int currentSeconds = (int) ( SystemClock.elapsedRealtime() / 1000);
        if ( _lastSensorCollectionSecs == 0 )
            _lastSensorCollectionSecs = currentSeconds;

        final int deltaSeconds =currentSeconds - _lastSensorCollectionSecs;
        if ( deltaSeconds <= COLLECTION_INTERVAL_SECS )
            return;

        _lastSensorCollectionSecs = currentSeconds;

        int[] preds = _dataInference.classify(
                _sensorMgr.linAccelSensorListener.times
                , _sensorMgr.linAccelSensorListener.magnitudes
                , _sensorMgr.gyroscopeSensorListener.times
                , _sensorMgr.gyroscopeSensorListener.magnitudes
                , deltaSeconds
        );

        _sensorMgr.clearSensorData();

        // accumulate stats
        // update last motion detection
        // update last activity state
        assert(preds.length==_stats.length);
        for ( int i =0; i < preds.length;i++) {
            _stats[i] += preds[i];
            Log.d(TAG, "c=" + i + "; v=" + _stats[i] );

            // if we detect motion states here, update last motion time
            //  the android trigger seems less sensitive to subtle movements
            if ( ( i != CLASS_SLEEPING ) && ( preds[i] > 0 ) )
                _lastMotionDetectedSeconds = currentSeconds;

            // update activity state.  allow overwrite with each iteration,
            //  since higher class = likely more accurate, or at least is what the user would expect
            if ( preds[i] > 0 )
                _lastActivityState = (byte)i;
        }

    }

    @Override
    public void onDestroy() {

        if ( _sensorMgr != null ) {
            try {
                _sensorMgr.disableSensors();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            } finally {
                _sensorMgr = null;
            }
        }

        if ( _motionTrigger != null ) {
            try {
                _motionTrigger.close();
            } catch (Exception ex ) {
                ex.printStackTrace();
            } finally {
                _motionTrigger=null;
            }
        }

        super.onDestroy();
    }

    @Override
    public Engine onCreateEngine() {
        /* provide your watch face implementation */
        return new Engine();
    }

    /* implement service callback methods */
    private class Engine extends CanvasWatchFaceService.Engine {

        @Override
        public void onCreate(SurfaceHolder holder) {
            super.onCreate(holder);
            /* initialize your watch face */
            // configure the system UI
            setWatchFaceStyle(new WatchFaceStyle.Builder(WatchFaceService.this)
                    .setBackgroundVisibility(WatchFaceStyle
                            .BACKGROUND_VISIBILITY_INTERRUPTIVE)
                    .setShowSystemUiTime(true)
                    .build());
        }

        @Override
        public void onPropertiesChanged(Bundle properties) {
            super.onPropertiesChanged(properties);
            /* get device features (burn-in, low-bit ambient) */
        }

        @Override
        public void onTimeTick() {
            super.onTimeTick();

            // this is the opportunity evaluate what has happened recently.
            // if there's an activity trigger, start up the sensors in the sensor manager
            Log.d(TAG,"onTimeTick");

            final long currentSeconds = Utils.getCurrentSeconds();

            if ( _lastTimeTickSeconds == 0 ) { // this is the first time being run
                _lastTimeTickSeconds = currentSeconds;
                _lastMotionDetectedSeconds = currentSeconds;
            }

            long lastTimeTickDelta = currentSeconds - _lastTimeTickSeconds;

            // wait for next tick?
            if ( lastTimeTickDelta < MIN_TICK_INTERVAL_SECS )
                return;

            // sufficient time has passed
            //  if the sensors aren't active, it's because the pet isn't moving.  accumulate sleeping time
            //  ideally we would check to see if the sensor is being charged or something

            if ( !_sensorMgr.isEnabled() )
                _stats[CLASS_SLEEPING] += lastTimeTickDelta; // do we need to account for lastSensorCollectionSecs?

            // motion detection logic
            //  if recently detected motion, enable the sensors if they weren't already
            //  else, wait until motion delay has passed, and then disable the sensors until we detect motion again

            long lastMotionDetectedDelta = currentSeconds - _lastMotionDetectedSeconds;
            if ( lastMotionDetectedDelta > SENSORS_SHUTDOWN_DELAY ) {

                // delay exceeded, we can shut down the sensors
                if ( _sensorMgr.isEnabled() ) {
                    Log.d(TAG, "Last motion detection time exceeded delay, stopping sensors");
                    try {
                        _sensorMgr.disableSensors();
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                    }
                }

            } else {  // recent activity, ensure sensors are enabled

                if ( !_sensorMgr.isEnabled() ) {
                    Log.d(TAG, "Enabling previously-disabled sensors");
                    try {
                        _sensorMgr.enableSensors(new Runnable() {
                            @Override
                            public void run() {
                                onSensorInterval();
                            }
                        });
                    } catch ( Exception ex ) {
                        ex.printStackTrace();
                    }
                }
            }

        }

        @Override
        public void onAmbientModeChanged(boolean inAmbientMode) {
            super.onAmbientModeChanged(inAmbientMode);
            /* the wearable switched between modes */
        }

        @Override
        public void onDraw(Canvas canvas, Rect bounds) {
            /* draw your watch face */
        }

        @Override
        public void onVisibilityChanged(boolean visible) {
            super.onVisibilityChanged(visible);
            /* the watch face became visible or invisible */
        }

    }
}
