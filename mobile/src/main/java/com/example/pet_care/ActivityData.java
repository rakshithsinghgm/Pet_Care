package com.example.pet_care;

import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import static com.example.pet_care.WekaUtils.evalClassifier;
import static com.example.pet_care.WekaUtils.getNBClassifier;
import static com.example.pet_care.WekaUtils.getRFClassifier;

public class ActivityData {

    Context _cx;

    // stringbuffer containing csv of data
    StringBuffer _sb = new StringBuffer();

    Classifier _dt = null;
    Classifier _rf = null;
    Classifier _nb = null;

    Instances _wekaInstances = null;

    public static String data_fname = "data.csv";
    public static String clsRF_fname = "rf.model";
    public static String clsDT_fname = "dt.model";
    public static String clsNB_fname = "nb.model";

    public static final String ACTIVITY_CLASS_UNKNOWN_STRING = "Unknown";
    public static final String ACTIVITY_CLASS_INACTIVE_STRING = "Inactive";
    public static final String ACTIVITY_CLASS_WALKING_STRING = "Walking";
    public static final String ACTIVITY_CLASS_RUNNING_STRING = "Running";

    public Map<String, Integer> InstancesCount = new HashMap<>();


    public ActivityData( Context cx ) throws Exception {
        this._cx = cx;

        // init instances count
        this.InstancesCount.put( ACTIVITY_CLASS_UNKNOWN_STRING, 0 );
        this.InstancesCount.put( ACTIVITY_CLASS_INACTIVE_STRING, 0 );
        this.InstancesCount.put( ACTIVITY_CLASS_WALKING_STRING, 0 );
        this.InstancesCount.put( ACTIVITY_CLASS_RUNNING_STRING, 0 );

        // attempt to restore state
        try {
            this._sb = Utils.readFile( this._cx, data_fname );
            this._wekaInstances = WekaUtils.getWekaInstances( this._sb.toString() );

            // count instances per class
            boolean hasUnknowns = false;
            for (Instance i : this._wekaInstances ) {

                String className =i.stringValue(i.classIndex());
                if ( !this.incrementClassnameCount(className))
                    hasUnknowns = true;
            }

            if ( hasUnknowns )
                Utils.showMsg(this._cx, "Saved activity data has unrecognized classes");

            this._dt = WekaUtils.loadClassifier(this._cx, clsDT_fname);
            this._rf = WekaUtils.loadClassifier(this._cx, clsRF_fname );
            this._nb = WekaUtils.loadClassifier(this._cx, clsNB_fname );

            Log.d("AD", "ActivityData restored successfully");
        }
        catch ( IOException ex ) {}  // ignore file not found
        catch ( Exception ex ) {
            throw ex;
        }

    }

    // returns false if the provided class name is unrecognized
    private boolean incrementClassnameCount( String className ) {

        boolean result = true;
        Object classCount = this.InstancesCount.get( className );

        // remap unknown classes to unknown category
        if ( classCount == null ) {
            result = false;
            className = ACTIVITY_CLASS_UNKNOWN_STRING;
        }

        this.InstancesCount.put( className,
                (int)this.InstancesCount.get( className ) + 1
        );

        return result;
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

    // classify the data, return a dict or something with algorithm and classification and confidence
    public String classify( Double[] data ) throws Exception {

        if ( this._dt != null ) {
            Map.Entry<String, Double> dt = WekaUtils.classification(this._dt, WekaUtils.getClassLabels( this._wekaInstances ), data);
            return dt.getKey() + " / " + String.valueOf(dt.getValue());
        }

        return "";

    }

    // train the models and return accuracy stats in a string (hacky)
    public String train() throws Exception {

        StringBuffer result = new StringBuffer();

        // recreate weka instances from current data
        this._wekaInstances = WekaUtils.getWekaInstances( this._sb.toString() );

        // j48
        result.append("J48: ");
        this._dt = WekaUtils.getJ48Classifier(this._wekaInstances);
        result.append( WekaUtils.evalClassifier( this._dt, this._wekaInstances) );

        // random forest
        result.append("\nRF: ");
        this._rf = WekaUtils.getRFClassifier(this._wekaInstances);
        result.append( WekaUtils.evalClassifier( this._rf, this._wekaInstances) );

        // naive bayes
        result.append("\nNB: ");
        this._nb = WekaUtils.getNBClassifier(this._wekaInstances);
        result.append( WekaUtils.evalClassifier( this._nb, this._wekaInstances) );

        return result.toString();
    }

    // save/overwrite the models and data to disk
    public void save() throws Exception {

        Utils.deleteFile( this._cx, data_fname );
        Utils.appendToFile( this._cx, data_fname, this._sb.toString());

        WekaUtils.writeFile( this._cx, clsDT_fname, this._dt );
        WekaUtils.writeFile( this._cx, clsRF_fname, this._rf );
        WekaUtils.writeFile( this._cx, clsNB_fname, this._nb );
    }

    // delete all data files
    public void delete() {
        Utils.deleteFile( this._cx, ActivityData.data_fname );
        Utils.deleteFile( this._cx, ActivityData.clsDT_fname );
        Utils.deleteFile( this._cx, ActivityData.clsNB_fname );
        Utils.deleteFile( this._cx, ActivityData.clsRF_fname );
    }


    // parse the sensor data string (CSV)
    public static Double[] parseSensorData( String s ) {

        // expected csv format:
        //  watch time (seconds)
        //  LinearAcceleration fields:  min, max, mean, variance, std.dev, zcr
        //  Gyroscope fields:           min, max, mean, variance, std.dev, zcr

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
        //  gyroscope indices (min,max):    6,7
        double linmag = Math.sqrt(
                Math.pow( data[0], 2. ) + Math.pow( data[1], 2. )
        );

        double gymag = Math.sqrt(
                Math.pow( data[6], 2. ) + Math.pow( data[7], 2. )
        );

        return Math.max( linmag, gymag );
    }

}
