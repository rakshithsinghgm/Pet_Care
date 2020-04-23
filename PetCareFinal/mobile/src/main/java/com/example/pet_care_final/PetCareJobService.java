package com.example.pet_care_final;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.util.Log;

public class PetCareJobService extends JobService {

    private static final String TAG = "PET_CARE_JOB_SERVICE";
    private  boolean jobCancelled = false;

    @Override
    public boolean onStartJob(JobParameters params) {

        Log.d(TAG,"Job Started");
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

                // Insert whatever work you want to do in the thread.

                // This tells the system, job finished and releases resources. Helps save battery
                jobFinished(params,true);
            }
        }).start();
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        Log.d(TAG,"Job Cancelled before starting");
        jobCancelled = true;
        return true;
    }
}
