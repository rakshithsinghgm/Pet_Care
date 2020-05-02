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
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.sql.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

public class PetCareJobService extends JobService implements MessageClient.OnMessageReceivedListener {

    private static final String TAG = "PET_CARE_JOB_SERVICE";

    private  boolean jobCancelled = false;
    private boolean _waitingForData = false;
    private Context _cx;

    private SQLiteDatabase statsdb;
    private Cursor cur;
    Calendar cal = Calendar.getInstance();
    String date = DateFormat.getDateInstance(DateFormat.FULL).format(cal.getTime());

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d(TAG,"Job Started");

        this._cx = this.getApplicationContext();
        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();
        // init message listener
        Wearable.getMessageClient( this._cx ).addListener(this);

        dobackGroundWork(params);
        return true;
    }

    private void dobackGroundWork(final JobParameters params) {

        final int WAIT_TIMEOUT_SECS = 10;

        new Thread(new Runnable() {
            @Override
            public void run() {

                if(jobCancelled){
                    return;
                }

                Log.d(TAG,"Running job");

                // Broadcast a message to get the data
                _waitingForData = true;
                new NodeMessageBroadcasterTask( _cx, Constants.REQUEST_DATA_PATH, null ).execute();

                // wait 10 secs for the message
                int waitTimeSecs = 0;
                while ( _waitingForData ) {

                    try {
                        Log.d(TAG,"Data not yet received. Sleeping for 1 second.");
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    if ( ( waitTimeSecs++ ) > WAIT_TIMEOUT_SECS ) {
                        Log.d(TAG,"Wait timeout exceeded, exiting");
                        break;
                    }
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

        if ( statsdb != null )
            statsdb.close();

        return true;
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "Received message on path: " +  messageEvent.getPath());

        String msgPath = messageEvent.getPath();

        if ( msgPath.equals( Constants.RESPONSE_DATA_PATH )) {

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
            statsdb.close();
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

        ContentValues cv = new ContentValues();
        cv.put(ActivityStats.StatsEntry.Sleeping, data[Constants.ACTIVITY_CLASS_SLEEPING] );
        cv.put(ActivityStats.StatsEntry.Inactive, data[Constants.ACTIVITY_CLASS_INACTIVE] );
        cv.put(ActivityStats.StatsEntry.Active, data[Constants.ACTIVITY_CLASS_ACTIVE] );
        cv.put(ActivityStats.StatsEntry.Time_Stamp, String.valueOf(date));

        // distance = active_time*speed ( speed = 80 cms/second, distance in miles )
        double distance = data[Constants.ACTIVITY_CLASS_ACTIVE] * 80;
        cv.put(ActivityStats.StatsEntry.Distance,distance);

        long success_val = statsdb.insert(ActivityStats.StatsEntry.Table_Name,null,cv);
        return success_val;
    }


    private void update_row(int[] data){

        // read the record matching today's date
        String SQL_READ_QUERY = "SELECT * FROM " + ActivityStats.StatsEntry.Table_Name + " WHERE " + ActivityStats.StatsEntry.Time_Stamp + " = ?";
        cur = statsdb.rawQuery(SQL_READ_QUERY, new String[]{String.valueOf(date)});
        cur.moveToFirst();

        int active_value = data[ Constants.ACTIVITY_CLASS_ACTIVE];
        int inactive_value = data[Constants.ACTIVITY_CLASS_INACTIVE];
        int sleeping_value = data[Constants.ACTIVITY_CLASS_SLEEPING];
        double dist = 0.0;

        int past_active_time=0;
        int past_inactive_time=0;
        int past_sleeping_time=0;
        String past_time_stamp;
        double past_distance = 0.0;

        while (!cur.isAfterLast()) {
            past_sleeping_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Sleeping));
            past_inactive_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Inactive));
            past_active_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Active));
            past_time_stamp = cur.getString(cur.getColumnIndex(ActivityStats.StatsEntry.Time_Stamp));
            past_distance = cur.getDouble(cur.getColumnIndex(ActivityStats.StatsEntry.Distance));
            cur.moveToNext();
        }
        cur.close();

        // increment it with fresh readings
        active_value+=past_active_time;
        inactive_value+=past_inactive_time;
        sleeping_value+=past_sleeping_time;
        dist = past_distance + ( active_value*80);


        //update the db where it finds the matching record
        ContentValues cv = new ContentValues();
        cv.put(ActivityStats.StatsEntry.Sleeping,sleeping_value);
        cv.put(ActivityStats.StatsEntry.Inactive,inactive_value);
        cv.put(ActivityStats.StatsEntry.Active,active_value);
        cv.put(ActivityStats.StatsEntry.Time_Stamp, String.valueOf(date));
        cv.put(ActivityStats.StatsEntry.Distance,dist);

        int res_cur = statsdb.update(ActivityStats.StatsEntry.Table_Name, cv," timestamp = ?",new String[]{date});

        if(res_cur==-1){
            Log.d(TAG,"Update function failed");
            return;
        }
        else{
            Log.d(TAG,"Update Row Worked");
        }
    }
}
