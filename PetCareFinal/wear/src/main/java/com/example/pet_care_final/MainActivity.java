package com.example.pet_care_final;

import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.widget.Button;
import android.widget.TextView;

// this is just here so the android studio debugger has something to launch
//  could probably figure out a way to remove it and keep debugging easy
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

    }

}