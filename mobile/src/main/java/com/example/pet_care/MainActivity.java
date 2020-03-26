package com.example.pet_care;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
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

import java.io.Writer;

public class MainActivity extends AppCompatActivity implements DataClient.OnDataChangedListener{

    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";
    Button active_button;
    Button inactive_button;
    Button no_activity_button;
    TextView logger;
    String TAG = "Mobile MainActivity";
    String flag="3";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        active_button = findViewById(R.id.active_btn);
        active_button.setOnClickListener ( new View.OnClickListener () {
                                   public void onClick(View v) {
                                       flag = "1";
                                       sendData(flag);
                                   }
                               }
        );
        inactive_button = findViewById(R.id.inactive_btn);
        inactive_button.setOnClickListener( new View.OnClickListener () {
                                                public void onClick(View v) {
                                                    flag = "0";
                                                    sendData(flag);
                                                }
                                            }
        );

        no_activity_button = findViewById(R.id.no_activity);
        no_activity_button.setOnClickListener( new View.OnClickListener () {
                                                public void onClick(View v) {
                                                    flag = "3";
                                                    sendData(flag);
                                                }
                                            }
        );

        logger = findViewById(R.id.logger);
        logger.setMovementMethod(new ScrollingMovementMethod());
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
                    logthis(message);
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

    //  Appends values to the TextView
    private void logthis(String newinfo) {
        // Check to see if data is not empty
        if (newinfo.compareTo("") != 0) {
            logger.append(newinfo);
        }
    }
}
