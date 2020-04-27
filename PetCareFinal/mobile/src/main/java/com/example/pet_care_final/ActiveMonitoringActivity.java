package com.example.pet_care_final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

import static com.example.pet_care_final.PetCareJobService.REQUEST_DATA_PATH;

public class ActiveMonitoringActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "ACTIVE_MONITORING_ACTIVITY";

    // send this to tell the watch to publish the current activity state
    static final String REQUEST_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-request";

    // the watch will publish to this path after it receives a publish command
    static final String RESPONSE_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-response";

    private boolean _waitingForData = false;
    private Context _cx;

    TextView result_txt;

    String prediction;

    int[] result = new int[1];
    int[] data = new int[1];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_monitoring);

        this._cx = this.getApplicationContext();

        //init message listener
        Wearable.getMessageClient( this._cx ).addListener(this);

        _waitingForData = true;
        new NodeMessageBroadcasterTask( _cx, REQUEST_ACTIVITY_STATE_PATH, null ).execute();
        // data has been received, and we are done

        //stopListeningForMessages();

        data[0]=999;
        if(data[0]==1){
            prediction = "Active";
        }
        else if(data[0]==2){
            prediction = "Inactive";
        }
        else if(data[0]==3){
            prediction="Sleeping";
        }
        else{
            prediction="None";
        }
        result_txt = findViewById(R.id.result);
        result_txt.setText("Current Class is "+prediction);

    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "Received message on path: " +  messageEvent.getPath());

        String msgPath = messageEvent.getPath();

        if ( msgPath.equals(RESPONSE_ACTIVITY_STATE_PATH)) {

            if ( !_waitingForData ) {
                Log.d(TAG,"Received data that we didn't ask for.  Possible duplicate");
                return;
            }

            // we have data!
            data = deserializeActivityData( messageEvent.getData() );
            Log.d( TAG, "Received elements, n=" + String.valueOf(data.length) );

        }
            _waitingForData=false; // reset flag so the job can terminate

    }

    private void stopListeningForMessages() {
        Log.d(TAG,"Unregistering message listener");
        Wearable.getMessageClient(this.getApplicationContext() ).removeListener(this);
    }

    private int[] deserializeActivityData( byte[] data ) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(bis);
            result = (int[])in.readObject();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return result;
    }
}
