package com.example.pet_care;

import java.util.ArrayList;
import java.util.List;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends WearableActivity implements DataClient.OnDataChangedListener, SensorEventListener {

    // handles sensor data accumulation/summation/etc
    private class SensorDataAccumulator {

        private List<Double> data = new ArrayList<Double>();

        public void append( float[] vals ) {
            double magnitude =
                    Math.sqrt(vals[0]*vals[0]+vals[1]*vals[1]+vals[2]*vals[2]);
            this.data.add(magnitude);
        }

        // writes data statistics in CSV format to the provided StringBuffer
        //  returns flag if data was written
        public boolean statsToCSV( StringBuffer sb ) {

            if ( this.data.isEmpty() )
                return false;

            double[] magArray = Utils.toDoubles(this.data);

            // min
            sb.append( Utils.minimum(magArray) );
            sb.append(',');

            // max
            sb.append( Utils.maximum(magArray) );
            sb.append(',');

            // mean
            sb.append( Utils.mean(magArray) );
            sb.append(',');

            // variance
            sb.append( Utils.variance(magArray) );
            sb.append(',');

            // std dev
            sb.append( Utils.standardDeviation(magArray) );
            sb.append(',');

            // zcr
            sb.append( Utils.zeroCrossingRate(magArray) );
            sb.append(',');

            return true;
        }
    }   // SensorDataAccumulator

    private final static String TAG = "Wear MainActivity";
    private TextView mTextView;
    Button myButton;

    // These are kinda like ROS Topics, datapath is for sending data, flag_datapath is for sending the class label
    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";

    // how frequently to send data
    private final static int SEND_DATA_SECS = 1;
    int lastSendTime = 0;

    public SensorManager sm;
    Sensor accelerometer;
    Sensor gyrocope;
    Sensor linear_accelerometer;

    private SensorDataAccumulator linaccelData = new SensorDataAccumulator();
    private SensorDataAccumulator gyroscopeData = new SensorDataAccumulator();

    /*
    private double x_val = 0.0;
    private double y_val = 0.0;
    private double z_val = 0.0;
    private double g_x_val = 0.0;
    private double g_y_val = 0.0;
    private double g_z_val = 0.0;
    private double l_x_val = 0.0;
    private double l_y_val = 0.0;
    private double l_z_val = 0.0;
     */

    private String cur_class; // 1 for Active, 0 for inactive
    private String data_message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);

        sm = (SensorManager) getSystemService ( Context.SENSOR_SERVICE );
        //accelerometer = sm.getDefaultSensor ( Sensor.TYPE_ACCELEROMETER );
        gyrocope = sm.getDefaultSensor (Sensor.TYPE_GYROSCOPE);
        linear_accelerometer = sm.getDefaultSensor (Sensor.TYPE_LINEAR_ACCELERATION);
        // sm.registerListener ( MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener ( MainActivity.this, gyrocope, SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener ( MainActivity.this, linear_accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // Enables Always-on
        setAmbientEnabled();
    }

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    //remove listener
    @Override
    public void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
        sm.unregisterListener(this);

    }

    //This is kinda like the receiver listener, it gets the String value associated with the flag key.
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (flag_datapath.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String message = dataMapItem.getDataMap().getString("flag");
                    cur_class = message;
                    Log.v(TAG, "Class value received from Phone was " + message);
                    String disp_text = "Class Value received from Phone was "+cur_class;
                    mTextView.setText(disp_text);

                } else {
                    // it's a path we're not listening to; skip
                    // Log.e(TAG, "Unrecognized path: " + path);

                }
            } else if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.v(TAG, "Data deleted : " + event.getDataItem().toString());
            } else {
                Log.e(TAG, "Unknown data event Type = " + event.getType());
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        int currentSeconds = (int) ( System.currentTimeMillis() / 1000);

        if ( lastSendTime == 0 )    // will be true on first sensor reading
            lastSendTime = currentSeconds;

        // Log.d(TAG, "currentSeconds = " + String.valueOf(currentSeconds));

        // has enough time elapsed to send data?
        if ( currentSeconds >= ( lastSendTime + SEND_DATA_SECS ) ) {

            // not sure if we need any concurrency controls here; looks like this is all run in a single UI thread

            // reset last send time
            lastSendTime = currentSeconds;

            // package and send data
            //  format is time_in_seconds,{lin_accel_data},{gyroscope_data},
            StringBuffer dataMsg = new StringBuffer( String.valueOf(currentSeconds ));
            dataMsg.append(',');

            // if we don't have data for either sensor, don't send at all
            if ( linaccelData.statsToCSV(dataMsg) && gyroscopeData.statsToCSV(dataMsg) )
                sendData(dataMsg.toString());
            else
                Log.e(TAG, "No data; message not sent");

            // reset data collectors
            linaccelData = new SensorDataAccumulator();
            gyroscopeData = new SensorDataAccumulator();
        }

        // always collect the data
        switch ( event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                linaccelData.append(event.values);
                break;
            case Sensor.TYPE_GYROSCOPE:
                gyroscopeData.append(event.values);
                break;
            default:
                Log.e(TAG, "Unknown data event Type = " + event.sensor.getType());
        }

        /*
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            x_val = event.values[0];
            y_val = event.values[1];
            z_val = event.values[2];
            //double magnitude_result = Math.sqrt ( (x_val * x_val) + (y_val * y_val) + (z_val * z_val) );
        }
        else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            g_x_val = event.values[0];
            g_y_val = event.values[1];
            g_z_val = event.values[2];
        }
        else if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            l_x_val = event.values[0];
            l_y_val = event.values[1];
            l_z_val = event.values[2];
        }

        // Concatenating all the sensor readings to one string
        data_message = x_val + "," + y_val + "," + z_val + ","+g_x_val+","+g_y_val+","+g_z_val+","+l_x_val+","+l_y_val+","+l_z_val+","+cur_class+"\n";
         */

    }


    // This method is used to send the data using the a path
    private void sendData(String message) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(datapath);
        dataMap.getDataMap().putString("data", message);
        PutDataRequest request = dataMap.asPutDataRequest();
        request.setUrgent();

        Task<DataItem> dataItemTask = Wearable.getDataClient(this).putDataItem(request);
        dataItemTask
                .addOnSuccessListener(new OnSuccessListener<DataItem>() {
                    @Override
                    public void onSuccess(DataItem dataItem) {
                        Log.d(TAG, "Sending message was successful: " + dataItem);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Sending message failed: " + e);
                    }
                });
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
