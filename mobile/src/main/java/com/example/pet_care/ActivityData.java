package com.example.pet_care;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;
import org.tensorflow.lite.support.model.Model;

import javax.xml.datatype.DatatypeConfigurationException;

public class ActivityData {

    Context _cx;

    // stringbuffer containing csv of data
    StringBuffer _sb = new StringBuffer();

    Interpreter _tfInt = null;

    public static String data_fname = "data.csv";
    public static String clsTF_fname = "cat_model.tflite";

    public Map<String, Integer> InstancesCount = new HashMap<>();

    // public static final String ACTIVITY_CLASS_UNKNOWN_STRING = "Unknown";

    // int values must match tensorflow transformed values
    public static final String ACTIVITY_CLASS_SLEEPING_STRING = "Sleeping";
    public static final String ACTIVITY_CLASS_INACTIVE_STRING = "Inactive";
    public static final String ACTIVITY_CLASS_ACTIVE_STRING = "Active";

    public static final Map<Integer, String> ActivityClasses = new HashMap<Integer, String>(){{
        put(0, ACTIVITY_CLASS_SLEEPING_STRING);
        put(1, ACTIVITY_CLASS_INACTIVE_STRING);
        put(2, ACTIVITY_CLASS_ACTIVE_STRING);
    }};


    public ActivityData( Context cx ) throws Exception {
        this._cx = cx;

        // init instances count
        for ( String s : ActivityClasses.values() )
            this.InstancesCount.put( s, 0);

        // attempt to restore state
        try {
            this._sb = Utils.readFile( this._cx, data_fname );

            // java split on newlines
            String[] lines = this._sb.toString().split("\\R+");

            // count instances per class
            // classname is last element on line
            boolean hasUnknowns = false;
            for ( String line : lines ) {
                String[] elems = line.split(",");

                if ( elems.length > 1) {
                    String className = elems[elems.length-1].trim();
                    if ( !this.incrementClassnameCount(className))
                        hasUnknowns = true;
                }
            }

            if ( hasUnknowns )
                Utils.showMsg(this._cx, "Saved activity data has unrecognized classes");

            // load tf-lite model
            MappedByteBuffer tfliteModel = FileUtil.loadMappedFile(this._cx, clsTF_fname );
            this._tfInt = new Interpreter(tfliteModel);
        }
        catch ( IOException ex ) {

            Utils.showMsg(this._cx, "Error loading activity data and/or model");

        }
        catch ( Exception ex ) {
            throw ex;
        }

    }

    // returns false if the provided class name is unrecognized
    private boolean incrementClassnameCount( String className ) {

        boolean result = true;
        Object classCount = this.InstancesCount.get( className );

        if ( classCount == null )
            return false;

        this.InstancesCount.put( className,
                (int)this.InstancesCount.get( className ) + 1
        );

        return true;
    }

    // append parsed sensor data to the collection of data
    public void append( Double[] data, String className ) {

        for ( int i = 0; i < data.length; i++ ) {
            this._sb.append(data[i]);
            this._sb.append(',');
        }
        this._sb.append(className);
        this._sb.append('\n');

        // update class stats
        if ( !incrementClassnameCount(className) )
            Utils.showMsg(this._cx, "Unrecognized class name: " + className );

    }

    // classify the data, return a string with classification and confidence
    public String classify( Double[] data ) throws Exception {

        List<Float> floatList = Utils.toFloatList(data);

        float[][] floats2d = new float[1][floatList.size()];
        for ( int i = 0; i < floatList.size(); i++ )
            floats2d[0][i] = (float)floatList.get(i);

        float[][] result = new float[1][ActivityClasses.size()];
        this._tfInt.run( floats2d, result );

        // convert highest prob to string
        int bestIdx = 0;
        for ( int i = 0; i < result[0].length; i++ ) {
            if ( result[0][i] > result[0][bestIdx] )
                bestIdx = i;
        }

        // format string for display w/ pct
        String fmtPct =  new DecimalFormat("#.####").format(result[0][bestIdx]);
        return ActivityClasses.get( bestIdx ) + " / " + fmtPct;
    }

    // save/overwrite the data to disk
    public void save() throws Exception {

        Utils.deleteFile( this._cx, data_fname );
        Utils.appendToFile( this._cx, data_fname, this._sb.toString());
    }

    // delete all data files
    public void delete() {
        Utils.deleteFile( this._cx, ActivityData.data_fname );
    }


    // parse the sensor data string (CSV)
    public static Double[] parseSensorData( String s ) {

        // expected csv format:
        //  watch time (seconds)
        //  LinearAcceleration fields:  min, max, mean, variance, std.dev, zcr, mcr, energy, skew, kurtosis, centroid, rms
        //  Gyroscope fields:           min, max, mean, variance, std.dev, zcr, mcr, energy, skew, kurtosis, centroid, rms

        StringTokenizer tok = new StringTokenizer(s,",", false);
        ArrayList<Double> result = new ArrayList<>();

        while ( tok.hasMoreElements() ) {
            String t = tok.nextToken();
            if ( t != null && !t.isEmpty() )
                result.add( Double.valueOf( t ) );
        }

        result.remove(0);   // remove the watch time

        return result.toArray( new Double[0] );
    }

    // calculate the largest magnitude between the min and max elements of the parsed sensor data
    public static double maxMagnitude( Double[] data ) {

        //  calculate largest magnitude in min,max diff between linearaccel and gyroscope, use that
        //  linaccel indices (min,max):     0,1
        //  gyroscope indices (min,max):    12,13
        double linmag = Math.sqrt(
                Math.pow( data[0], 2. ) + Math.pow( data[1], 2. )
        );

        double gymag = Math.sqrt(
                Math.pow( data[12], 2. ) + Math.pow( data[13], 2. )
        );

        return Math.max( linmag, gymag );
    }

}
