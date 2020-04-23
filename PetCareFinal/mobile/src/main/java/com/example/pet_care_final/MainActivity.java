package com.example.pet_care_final;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class MainActivity extends AppCompatActivity {

    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";
    String TAG = "Mobile MainActivity";
    Switch montior_switch;
    boolean monitor_flag=false;

    TextView stats;

    public static final String ACTIVITY_CLASS_SLEEPING_STRING = "Sleeping";
    public static final String ACTIVITY_CLASS_INACTIVE_STRING = "Inactive";
    public static final String ACTIVITY_CLASS_ACTIVE_STRING = "Active";

    private SQLiteDatabase statsdb;
    private Cursor cur;

    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    //Date date = sdf.format(new Date ());//tc:  does not compile
    Date date = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();

        montior_switch = findViewById(R.id.switch1);
        if ( montior_switch.isChecked() ) monitor_flag = true;
        else monitor_flag = false;

        if (monitor_flag == true){
            scheduleJob();
        }
        else //(monitor_flag == false) tc:  does not compile
        {
            cancelJob();
        }

        stats = findViewById(R.id.stats);

        DispStats();

        // DispStats();

    }

    // this function will monitor pet activity by communicating with the watch app
    public void Monitor(){
        /*
        *
        *   Should Set these following actions in the background
        *   Should send a flag to the watch and receive predictions from the watch at fixed intervals of time
        *   Should Log the predictions with their timestamp in a database
        *
        * */

    }


    /*

    ### SERVICES OPTIONAL ###
    public void startService(View V) {

        String flag = " ";
        Intent serviceIntent = new Intent(this, PetCareService.class);
        serviceIntent.putExtra("inputExtra", flag);
        ContextCompat.startForegroundService(this, serviceIntent);
    }

    public void stopService(View V) {

        Intent serviceIntent = new Intent(this, PetCareService.class);
        stopService(serviceIntent);

    }

    */

    // Display Stats Sections

    public void DispStats() {

        String SQL_READ_QUERY = "SELECT * FROM "+ActivityStats.StatsEntry.Table_Name +" WHERE "+ActivityStats.StatsEntry.Time_Stamp +" = ?";
        statsdb.rawQuery(SQL_READ_QUERY, new String[] {String.valueOf(date)});

    }
    /*
    // Data Communiaction Section
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

     */

    private void add_table(){

        int active_value = 999;
        int inactive_value = 999;
        int sleeping_value = 999;

        ContentValues cv = new ContentValues();

        cv.put(ActivityStats.StatsEntry.Active,active_value);
        cv.put(ActivityStats.StatsEntry.Inactive,inactive_value);
        cv.put(ActivityStats.StatsEntry.Sleeping,sleeping_value);
        cv.put(ActivityStats.StatsEntry.Time_Stamp, String.valueOf(date));

        statsdb.insert(ActivityStats.StatsEntry.Table_Name,null,cv);

    }


    /*
    private void read_table(int position){
        if(!cur.move(position)){
            return;
        }
        cur.MoveToFirst();
        Timestamp time_stamp = Timestamp.valueOf(cur.getString(cur.getColumnIndex(ActivityStats.StatsEntry.Time_Stamp)));

        int active_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Active));

        int inactive_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Inactive));
        
        int sleeping_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Sleeping));

        textView.setText("Time Spent in Active Class"+ active_time +"\n" +"Time Spent in Inactive Class"+ inactive_time +"\n" +"Time Spent in Sleeping Class"+ sleeping_time +"\n");
    }

    */

    /*
    private Cursor read_all(){
        return statsdb.query(
                ActivityStats.StatsEntry.Table_Name,
                null,
                null,
                null,
                null,
                null,
                ActivityStats.StatsEntry.Time_Stamp + " DESC");
        )
    } */


    private void scheduleJob(){
        ComponentName comp_name = new ComponentName(this,PetCareJobService.class);
        JobInfo info = new JobInfo.Builder(777,comp_name)
                .setPeriodic(15*60*1000)
                .setPersisted(true)
                .build();

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.schedule(info);

    }
    public void cancelJob(){

        JobScheduler scheduler = (JobScheduler) getSystemService(JOB_SCHEDULER_SERVICE);
        scheduler.cancel(777);
    }
}
