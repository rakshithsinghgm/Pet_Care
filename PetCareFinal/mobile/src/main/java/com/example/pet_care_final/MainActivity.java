package com.example.pet_care_final;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity  {

    String datapath = "/data_path";
    String flag_datapath = "/flag_datapath";
    String TAG = "Mobile MainActivity";
    Switch montior_switch;
    boolean monitor_flag=false;

    Button stats_btn;
    Button active_monitoring_btn;

    private SQLiteDatabase statsdb;
    private Cursor cur;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();

        montior_switch = findViewById(R.id.switch1);
        stats_btn = findViewById(R.id.stats_btn);
        active_monitoring_btn = findViewById(R.id.active_monitor_btn);

        // todo:  get current job status, and set monitor_switch value

    }

    public void call_activemonitor_act(View view) {

        Intent activemonitoring_intent = new Intent(this, ActiveMonitoringActivity.class);
        this.startActivity(activemonitoring_intent);
    }

    public void call_stats_act(View view) {

        Intent stats_act_intent = new Intent(this, StatsActivity.class);
        this.startActivity(stats_act_intent);
    }


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
