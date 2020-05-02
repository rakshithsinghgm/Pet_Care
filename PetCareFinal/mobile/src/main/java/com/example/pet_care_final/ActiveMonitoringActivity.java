package com.example.pet_care_final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;

public class ActiveMonitoringActivity extends AppCompatActivity implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "ACTIVE_MONITORING_ACTIVITY";

    // private boolean _waitingForData = false;
    private Context _cx;
    TextView result_txt;

    static final long UPDATE_DELAY_MS = 1000;

    Handler _handler = new Handler();
    Runnable _updateState = new Runnable() {
        @Override
        public void run() {
            new NodeMessageBroadcasterTask( _cx, Constants.REQUEST_ACTIVITY_STATE_PATH, null ).execute();
            _handler.postDelayed(_updateState, UPDATE_DELAY_MS );
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_active_monitoring);

        this._cx = this.getApplicationContext();

        Log.d(TAG,"onCreate");
    }

    @Override
    protected void onPause() {
        super.onPause();

        Log.d(TAG,"onPause");

        _handler.removeCallbacks(_updateState);
        Wearable.getMessageClient(this.getApplicationContext() ).removeListener(this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG,"onResume");

        //init message listener
        Wearable.getMessageClient( this._cx ).addListener(this);

        _handler.post(_updateState);
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "Received message on path: " +  messageEvent.getPath());

        String msgPath = messageEvent.getPath();

        if ( msgPath.equals( Constants.RESPONSE_ACTIVITY_STATE_PATH)) {

            // we have data!
            String prediction = "Error";

            if ( messageEvent.getData() != null && messageEvent.getData().length > 0 ) {
                Log.d( TAG, "Received element; v=" + String.valueOf(messageEvent.getData()[0]) );

                byte currentClass = messageEvent.getData()[0];

                switch ( currentClass ) {
                    case Constants.ACTIVITY_CLASS_SLEEPING:
                        prediction="Inactive/sleeping";
                        break;
                    case Constants.ACTIVITY_CLASS_INACTIVE:
                        prediction="Active/not moving";
                        break;
                    case Constants.ACTIVITY_CLASS_ACTIVE:
                        prediction="Active/moving";
                        break;
                }
            }

            result_txt = findViewById(R.id.result);
            result_txt.setText("Current Class is "+prediction);
        }
    }
}
