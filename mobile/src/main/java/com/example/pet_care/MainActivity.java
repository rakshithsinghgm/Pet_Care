package com.example.pet_care;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

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

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import static com.example.pet_care.Utils.readFile;
import static com.example.pet_care.Utils.showMsg;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener{

    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";
    // TextView logger;
    String TAG = "Mobile MainActivity";
    String flag;

    String current_label = null;

    boolean is_collecting = false;
    double progressbar_magnitude_max = 0.;
    ActivityData data = null;

    // UI widgets
    Switch switchDataCollection;
    RadioGroup rgCurrentActivity;
    RadioButton rbCurrentActivityInactive;
    RadioButton rbCurrentActivityActive;
    RadioButton rbCurrentActivitySleeping;
    TextView tvCurrentActivityText;
    TextView tvInactiveCount;
    TextView tvActiveCount;
    TextView tvSleepingCount;
    ProgressBar currentActivityProgress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // get ui widgets
        switchDataCollection = findViewById(R.id.switchDataCollection);
        rgCurrentActivity = findViewById(R.id.rgCurrentActivity);
        rbCurrentActivityInactive = findViewById(R.id.rbInactive);
        rbCurrentActivityActive = findViewById(R.id.rbActive);
        rbCurrentActivitySleeping = findViewById(R.id.rbSleeping);
        tvCurrentActivityText= findViewById(R.id.tvCurrentActivityText);
        currentActivityProgress= findViewById(R.id.currentActivityProgress);
        tvInactiveCount = findViewById(R.id.tvInactiveCount);
        tvActiveCount = findViewById(R.id.tvActiveCount);
        tvSleepingCount = findViewById(R.id.tvSleepingCount);

        try {
            this.data = new ActivityData(this.getApplicationContext());
            this.updateInstanceCounts();    // requires this.data
        }
        catch ( Exception ex ) {
            showMsg(this.getApplicationContext(), ex );
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Wearable.getDataClient(this).addListener(this);
    }

    //remove data listener
    @Override
    public void onPause() {
        super.onPause();
        Wearable.getDataClient(this).removeListener(this);
    }


    // Similar to Wear Device sendData Method
    private void sendData(String message) {
        PutDataMapRequest dataMap = PutDataMapRequest.create(flag_datapath);
        dataMap.getDataMap().putString("flag", message);
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
                })
        ;
    }

    // Similar to Wear Device receiver listener
    @Override
    public void onDataChanged(@NonNull DataEventBuffer dataEventBuffer) {
        Log.d(TAG, "onDataChanged: " + dataEventBuffer);
        for (DataEvent event : dataEventBuffer) {
            if (event.getType() == DataEvent.TYPE_CHANGED) {
                String path = event.getDataItem().getUri().getPath();
                if (datapath.equals(path)) {
                    DataMapItem dataMapItem = DataMapItem.fromDataItem(event.getDataItem());
                    String message = dataMapItem.getDataMap().getString("data");
                    Log.v(TAG, "Wear activity received message: " + message);
                    // Display message in UI
                    // logthis(message);
                    try {
                        this.onPetCareDataReceived(message);
                    } catch (Exception ex) {
                        showMsg(this.getApplicationContext(), ex );
                    }
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

    private void updateInstanceCounts() {

        tvInactiveCount.setText(
                String.valueOf( this.data.InstancesCount.getOrDefault( ActivityData.ACTIVITY_CLASS_INACTIVE_STRING, 0 ) )
        );

        tvActiveCount.setText(
                String.valueOf( this.data.InstancesCount.getOrDefault( ActivityData.ACTIVITY_CLASS_ACTIVE_STRING, 0 ) )
        );

        tvSleepingCount.setText(
                String.valueOf( this.data.InstancesCount.getOrDefault( ActivityData.ACTIVITY_CLASS_SLEEPING_STRING, 0 ) )
        );

    }

    // fired when we receive pet care data
    private void onPetCareDataReceived( String data ) {

        // the first element in the data is the watch time in case we need it, but that is omitted from parse
        Double[] sensorData = ActivityData.parseSensorData( data );

        // debug
        // Utils.showMsg(this.getApplicationContext(), data );

        if ( this.is_collecting ) {
            // determine current label and value for data collection
            current_label = null;

            if ( rbCurrentActivityInactive.isChecked() ) {
                current_label = ActivityData.ACTIVITY_CLASS_INACTIVE_STRING;
            } else if ( rbCurrentActivityActive.isChecked() ) {
                current_label = ActivityData.ACTIVITY_CLASS_ACTIVE_STRING;
            } else if ( rbCurrentActivitySleeping.isChecked() ) {
                current_label = ActivityData.ACTIVITY_CLASS_SLEEPING_STRING;
            }

            if ( current_label != null ) {
                // Log.v(TAG, "Appending data[], class=" + current_label);

                // add to data collection
                this.data.append(sensorData, current_label);

                // update collection labels with # of samples
                this.updateInstanceCounts();
            }
        }

        // always perform inference, if we can
        try {
            String classMsg = this.data.classify( sensorData );
            tvCurrentActivityText.setText(classMsg);
        } catch ( Exception ex ) {
            Log.e( TAG, ex.getMessage() );
        }

        // always update progressbar based on the received data
        this.updateProgressBar( ActivityData.maxMagnitude(sensorData) );

    }

    public void updateProgressBar( double sensorMag ) {

        this.progressbar_magnitude_max = Math.max( this.progressbar_magnitude_max, sensorMag );

        if ( this.progressbar_magnitude_max > 0 )
            this.currentActivityProgress.setProgress( (int)( ( sensorMag / this.progressbar_magnitude_max ) * 100. ) );

    }

    // start data collection
    public void btnStartCollection_click(View view) {

        is_collecting = switchDataCollection.isChecked();

        if ( is_collecting ) {
            showMsg(this.getApplicationContext(), "Starting data collection");
        } else {  // not collecting data
            showMsg(this.getApplicationContext(), "Stopping data collection");
        }

    }

    public void btnDelete_click(View view) {

        this.data.delete();
        try {
            this.data = new ActivityData(this.getApplicationContext()); // erase any saved state
            this.updateInstanceCounts();
        } catch ( Exception ex ) {
            showMsg(this.getApplicationContext(), ex );
        }

        showMsg( this.getApplicationContext(), "All data deleted");
    }

    public void btnSaveData_click(View view) {

        try {
            this.data.save();
            showMsg( this.getApplicationContext(), "Data saved");
        }
        catch (Exception ex ) {
            Utils.showMsg(this.getApplicationContext(), ex );
        }
    }
}
