package com.example.pet_care_final;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.CapabilityClient;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Date;
import java.text.SimpleDateFormat;

public class PetCareJobService extends JobService implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "PET_CARE_JOB_SERVICE";

    private static final int NUM_CLASSES = 3;// number of activity classes we can predict

    // start messages
    // these should match wear/petcaredataservice.java

    // send this to tell the watch to start data collection
    public static final String START_DATA_COLLECTION_PATH = "/pet-care-sensor-data-start";

    // send this to tell the watch to stop data collection
    public static final String STOP_DATA_COLLECTION_PATH = "/pet-care-sensor-data-stop";

    // send this to tell the watch to publish (and reset) its current data
    public static final String REQUEST_DATA_PATH = "/pet-care-sensor-data-request";

    // the watch will publish to this path after it receives a publish command
    public static final String RESPONSE_DATA_PATH = "/pet-care-sensor-data-response";

    // end messages

    private  boolean jobCancelled = false;
    private boolean _waitingForData = false;
    private Context _cx;


    private SQLiteDatabase statsdb;
    private Cursor cur;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //Date date = sdf.format(new Date ());//tc:  does not compile
    Date date = null;

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d(TAG,"Job Started");

        this._cx = this.getApplicationContext();

        // init message listener
        Wearable.getMessageClient( this._cx ).addListener(this);

        dobackGroundWork(params);
        return true;
    }

    private void dobackGroundWork(final JobParameters params) {

        new Thread(new Runnable() {
            @Override
            public void run() {

                if(jobCancelled){
                    return;
                }

                Log.d(TAG,"Running job");

                // Broadcast a message to get the data
                _waitingForData = true;
                new NodeMessageBroadcasterTask( _cx, REQUEST_DATA_PATH, null ).execute();

                // wait for the message
                while ( _waitingForData ) {
                    try {
                        Log.d(TAG,"Data not yet received. Sleeping for 1 second.");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    // after 10 seconds or so, may want to send another request or do something else

                }

                // data has been received, and we are done
                stopListeningForMessages();

                // This tells the system, job finished and releases resources. Helps save battery
                jobFinished(params,true);
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG,"Job Cancelled before starting");
        jobCancelled = true;
        _waitingForData=false;   // may not be necessary
        this.stopListeningForMessages();
        return true;
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "Received message on path: " +  messageEvent.getPath());

        String msgPath = messageEvent.getPath();

        if ( msgPath.equals(RESPONSE_DATA_PATH)) {

            if ( !_waitingForData ) {
                Log.d(TAG,"Received data that we didn't ask for.  Possible duplicate");
                return;
            }

            // we have data!
            int[] data =deserializeActivityData( messageEvent.getData() );
            Log.d( TAG, "Received elements, n=" + String.valueOf(data.length) );

            if ( data.length > 0 ) {
                // we have the class as the index, and the number of seconds in each class as the value
                // add to a database or something
                for ( int i = 0; i < data.length; i++ ) {
                    Log.d(TAG, "Class=" + i + ", seconds=" + data[i] );
                }
            }

             long insert_succes = add_row(data);
             if(insert_succes == -1){
                 Log.d("TAG","Insert Operation Failed");
                 update_row(data);
             }
             else{
                 Log.d(TAG,"Insert Operation Worked");
             }
            _waitingForData=false; // reset flag so the job can terminate

        }

    }

    private void stopListeningForMessages() {
        Log.d(TAG,"Unregistering message listener");
        Wearable.getMessageClient(this.getApplicationContext() ).removeListener(this);
    }

    // converts array of bytes to array of ints
    //  Resulting length should equal NUM_CLASSES
    private int[] deserializeActivityData( byte[] data ) {

        int[] result = new int[0];
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            ObjectInputStream in = new ObjectInputStream(bis);
            result = (int[])in.readObject();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
        return result;
    }

    private long add_row(int[] data){

        //Assuming data[0] = Active value, data[1] = Inactive value, data[2] = Sleeping value;
        int active_value = data[0];
        int inactive_value = data[1];
        int sleeping_value = data[2];

        ContentValues cv = new ContentValues();

        cv.put(ActivityStats.StatsEntry.Active,active_value);
        cv.put(ActivityStats.StatsEntry.Inactive,inactive_value);
        cv.put(ActivityStats.StatsEntry.Sleeping,sleeping_value);
        cv.put(ActivityStats.StatsEntry.Time_Stamp, String.valueOf(date));
        long success_val = statsdb.insert(ActivityStats.StatsEntry.Table_Name,null,cv);
        return success_val;
    }

    private void update_row(int[] data){
        int active_value = data[0];
        int inactive_value = data[1];
        int sleeping_value = data[2];
        cur = statsdb.rawQuery("UPDATE " +ActivityStats.StatsEntry.Table_Name + " WHERE "+ ActivityStats.StatsEntry.Time_Stamp +
                " = " + String.valueOf(date),null);
        if(cur==null){
            Log.d(TAG,"Update function failed");
            return;
        }
        else{
            Log.d(TAG,"Update Row Worked");
        }
    }
}
