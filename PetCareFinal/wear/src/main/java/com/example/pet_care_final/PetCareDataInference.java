package com.example.pet_care_final;

import android.content.Context;
import android.util.Log;


import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class PetCareDataInference {

    static final String TAG = "pet_care_final";

    TfModel _tf;

    public PetCareDataInference(Context cx) {
        // load tf model
        this._tf = new TfModel(cx);
    }

    // given two lists of sensor magnitudes, classify the data
    //  returns -1 on failure
    public int classify(List<Float> linAccelData, List<Float> gyroscopeData ) {

        ArrayList<Float> stats = SensorDataAccumulator.calcStats( linAccelData );
        if ( !stats.addAll( SensorDataAccumulator.calcStats( gyroscopeData ) ) ) {
            Log.w(TAG,"Error concatenating lists");
            return -1;
        }

        return this._tf.predict( stats );
    }

    // given two pairs of parallel time and magnitudes, classify the data by second
    public int[] classify( List<Long> linAccelTimes, List<Float> linAccelData, List<Long> gyroscopeTimes, List<Float> gyroscopeData, int duration ) {

        int[] result = new int[3];  //NUM_CLASSES

        if ( linAccelTimes.isEmpty())
            return result;

        // batch by time
        Map<Long,List<Float>> laBatches = batchByKey( linAccelTimes, linAccelData );
        Map<Long,List<Float>> gyBatches = batchByKey( gyroscopeTimes, gyroscopeData );

        if (
                !laBatches.keySet().containsAll( gyBatches.keySet() )
                || !gyBatches.keySet().containsAll( laBatches.keySet() )
        ) {
            Log.w(TAG, "Batch size/time mismatch, classifying on entire batch instead");

            result[classify(linAccelData, gyroscopeData)] = duration;
            return result;
        }

        for ( Long k : laBatches.keySet() ) {

            // Log.d(TAG, "la Batch k="  + String.valueOf(k) + ", sz=" + laBatches.get(k).size() + ", total=" + linAccelData.size()  );
            // Log.d(TAG, "gy Batch k="  + String.valueOf(k) + ", sz=" + gyBatches.get(k).size() + ", total=" + gyroscopeData.size() );

             result[classify( laBatches.get(k), gyBatches.get(k) )] += 1;
        }

        // todo:  if batches.size() < duration, then we have missing time to fill

        return result;
    }

    // create batches of values grouped by key
    Map<Long,List<Float>> batchByKey( List<Long> keys, List<Float> values ) {

        Map<Long,List<Float>> result = new HashMap<>();

        if ( keys.isEmpty() || values.isEmpty() )
            return result;

        ArrayList<Float> currentValues = new ArrayList<>();

        // batch by key
        Long lastK = keys.get(0);

        for ( int i = 0; i < keys.size(); i++ ) {

            if ( !keys.get(i).equals( lastK ) )// control break
            {
                // Log.d(TAG, "Adding " + lastK + ": sz=" + currentValues.size());

                result.put( lastK, currentValues );
                lastK = keys.get(i);
                currentValues = new ArrayList<>();
            }

            currentValues.add(values.get(i));
        }

        // handle remaining
        if ( !currentValues.isEmpty() )
            result.put(lastK,currentValues);

        return result;
    }



}
