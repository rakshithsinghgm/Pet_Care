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

    // private boolean _waitingForData = false;
    private Context _cx;
    TextView result_txt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_monitoring);

        this._cx = this.getApplicationContext();

        //init message listener
        Wearable.getMessageClient( this._cx ).addListener(this);

        new NodeMessageBroadcasterTask( _cx, REQUEST_ACTIVITY_STATE_PATH, null ).execute();
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "Received message on path: " +  messageEvent.getPath());

        String msgPath = messageEvent.getPath();

        if ( msgPath.equals(RESPONSE_ACTIVITY_STATE_PATH)) {

            // we have data!
            String prediction = "Error";

            if ( messageEvent.getData() != null && messageEvent.getData().length > 0 ) {
                Log.d( TAG, "Received elements, n=" + String.valueOf(messageEvent.getData().length) );

                byte currentClass = messageEvent.getData()[0];

                switch ( currentClass ) {
                    case 0:
                        prediction="Inactive/sleeping";
                        break;
                    case 1:
                        prediction="Active/not moving";
                        break;
                    case 2:
                        prediction="Active/moving";
                        break;
                }
            }

            result_txt = findViewById(R.id.result);
            result_txt.setText("Current Class is "+prediction);
        }
    }

    private void stopListeningForMessages() {
        Log.d(TAG,"Unregistering message listener");
        Wearable.getMessageClient(this.getApplicationContext() ).removeListener(this);
    }
}
