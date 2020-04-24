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

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.MessageClient;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity  {

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
    BarChart chart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();

        montior_switch = findViewById(R.id.switch1);

        // todo:  get current job status, and set monitor_switch value

        //DispStats();
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

        String SQL_READ_QUERY = "SELECT * FROM " + ActivityStats.StatsEntry.Table_Name + " WHERE " + ActivityStats.StatsEntry.Time_Stamp + " = ?";
        statsdb.rawQuery(SQL_READ_QUERY, new String[]{String.valueOf(date)});

        int active_time = 30;
        int inactive_time = 50;
        int sleeping_time = 150;
        String time_stamp = "";
        cur.moveToFirst();
        while (!cur.isAfterLast()) {
            active_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Active));
            inactive_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Inactive));
            sleeping_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Sleeping));
            time_stamp = cur.getString(cur.getColumnIndex(ActivityStats.StatsEntry.Time_Stamp));
            cur.moveToNext();
        }
        stats.setText("Time Spent in Active Class"+ active_time +"\n" +"Time Spent in Inactive Class"+ inactive_time +"\n" +"Time Spent in Sleeping Class"+ sleeping_time +"\n");

        /*
        ArrayList<BarEntry> BarEntry = new ArrayList<>();

        BarEntry.add(new BarEntry(active_time, 0));
        BarEntry.add(new BarEntry(inactive_time, 1));
        BarEntry.add(new BarEntry(sleeping_time, 2));

        BarDataSet dataSet = new BarDataSet(BarEntry, "Pet Stats");
        BarData data = new BarData();
        data.addDataSet(dataSet);

        //final ArrayList<String> labels = new ArrayList<>();
        //labels.add("Active");
        //labels.add("Inactive");
        //labels.add("Sleeping");

        final List list_x_axis_name = new ArrayList<>();
        list_x_axis_name.add("Active");
        list_x_axis_name.add("Inactive");
        list_x_axis_name.add("Sleeping");

        XAxis xAxis = chart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setCenterAxisLabels(true);
        xAxis.setLabelRotationAngle(-90);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(list_x_axis_name));

        //chart.setDataSer(dataSet);
        chart.setData(data);
        chart.setFitBars(true);

         */
    }

    /*
    private void read_table(int position){
        if(!cur.move(position)){
            return;
        }
        cur.moveToFirst();
        Timestamp time_stamp = Timestamp.valueOf(cur.getString(cur.getColumnIndex(ActivityStats.StatsEntry.Time_Stamp)));

        int active_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Active));

        int inactive_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Inactive));
        
        int sleeping_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Sleeping));

        stats.setText("Time Spent in Active Class"+ active_time +"\n" +"Time Spent in Inactive Class"+ inactive_time +"\n" +"Time Spent in Sleeping Class"+ sleeping_time +"\n");
    }



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
    }

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        Log.d(TAG, "MainActivity received message on path: " +  messageEvent.getPath());
    }
     */

    public void monitorswitch_click(View view) {
        if ( montior_switch.isChecked() )
            scheduleJob();
        else
            cancelJob();
    }

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
