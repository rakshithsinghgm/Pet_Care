package com.example.pet_care_final;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.ArrayList;
import java.util.List;

import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

public class TfModel {

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

    public int test( float[] sensor1, float[] sensor2 ) {

        ArrayList<Float> combined = new ArrayList<>();

        for ( float f : sensor1 )
            combined.add(f);

        for (float f : sensor2  )
            combined.add(f);

        //combined.remove(17);
        //combined.remove(5);

        return  this.test( combined.toArray(new Float[0]) );
    }

    public int test( Float[] data ) {

        // convert to 2d array
        float[][] floats2d = new float[1][data.length];
        for ( int i = 0; i < data.length; i++ )
            floats2d[0][i] = (float)data[i];

        float[][] result = new float[1][3];
        this._tfInt.run( floats2d, result );

        // convert highest prob to string
        int bestIdx = 0;
        for ( int i = 0; i < result[0].length; i++ ) {
            if ( result[0][i] > result[0][bestIdx] )
                bestIdx = i;
        }

        Log.w("WEAR", String.valueOf(bestIdx));
        return bestIdx;
    }
}