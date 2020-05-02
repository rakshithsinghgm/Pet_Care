package com.example.pet_care_final;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

public class TfModel {

    static final String TAG = "pet_care_final";

    Interpreter _tfInt = null;
    Context _cx;

    public TfModel( Context cx ) {

        this._cx = cx;
        try {
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this._cx, "cat_model.tflite");
            this._tfInt = new Interpreter(tfliteModel);
        }
        catch ( IOException ex ) {
            Log.e("WEAR", "Error loading tflite");
        }

    }

    public int predict( List<Float> data ) {

        // increase for tf-lite benchmarking
        int nruns = 1;

        // convert to 2d array
        float[][] floats2d = new float[1][data.size()];
        for ( int i = 0; i < data.size(); i++ )
            floats2d[0][i] = (float)data.get(i);

        float[][] result = new float[1][3];

        long start_ms = SystemClock.elapsedRealtime();

        for ( int i = 0; i < nruns; i++ )
            this._tfInt.run( floats2d, result );

        long end_ms = SystemClock.elapsedRealtime();

        if ( nruns > 1 ) {
            Log.d(TAG, "Benchmark: completed " + nruns + " predictions in " + ( end_ms - start_ms )
                    + " ms.  avg=" + ( ( end_ms-start_ms) / ( float )nruns ) + " ms/run" );
        }

        int bestIdx = 0;
        for ( int i = 1; i < result[0].length; i++ ) {
            if ( result[0][i] > result[0][bestIdx] )
                bestIdx = i;
        }

        //Log.d (TAG, String.valueOf(bestIdx));
        return bestIdx;
    }
}