package com.example.pet_care;

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
    private final static String TAG = "Wear MainActivity";
    private TextView mTextView;
    Button myButton;

    // These are kinda like ROS Topics, datapath is for sending data, flag_datapath is for sending the class label
    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";


    public SensorManager sm;
    Sensor accelerometer;
    Sensor gyrocope;
    Sensor linear_accelerometer;
    private double x_val = 0.0;
    private double y_val = 0.0;
    private double z_val = 0.0;
    private double g_x_val = 0.0;
    private double g_y_val = 0.0;
    private double g_z_val = 0.0;
    private double l_x_val = 0.0;
    private double l_y_val = 0.0;
    private double l_z_val = 0.0;
    private String cur_class; // 1 for Active, 0 for inactive
    private String data_message;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);

        sm = (SensorManager) getSystemService ( Context.SENSOR_SERVICE );
        accelerometer = sm.getDefaultSensor ( Sensor.TYPE_ACCELEROMETER );
        gyrocope = sm.getDefaultSensor (Sensor.TYPE_GYROSCOPE);
        linear_accelerometer = sm.getDefaultSensor (Sensor.TYPE_LINEAR_ACCELERATION);
        sm.registerListener ( MainActivity.this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
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
                    Log.e(TAG, "Unrecognized path: " + path);
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
        sendData(data_message);
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
