package com.example.pet_care_final;

import android.content.Intent;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;


public class MainActivity extends WearableActivity {


    // https://github.com/android/wear-os-samples/tree/master/DataLayer

    private final static String TAG = "Wear MainActivity";
    private TextView mTextView;
    //private ProgressBar progressBar;
    Button myButton;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextView = findViewById(R.id.text);
        //progressBar = findViewById(R.id.progressBar);

        // todo (?):  https://stackoverflow.com/questions/39058837/android-wear-measuring-sensors-and-preventing-ambient-mode-sleep

        // on main activity create, fire up the background service
        Intent startIntent = new Intent(this, PetCareDataService.class);
        startService(startIntent);

        // Enables Always-on
        // setAmbientEnabled();
    }


}