package com.example.pet_care_final;

import java.util.ArrayList;
import java.util.List;

// handles sensor data accumulation/summation/etc
public class SensorDataAccumulator {

    public List<Double> data = new ArrayList<Double>();

    public void append( float[] vals ) {
        double magnitude =
                Math.sqrt(vals[0]*vals[0]+vals[1]*vals[1]+vals[2]*vals[2]);
        this.data.add(magnitude);
    }

    public ArrayList<Float> getStats( ) {
        return null;
    }

    public static ArrayList<Float> calcStats( List<Float> data ) {

        double[] magArray = Utils.toDoubles2( data );

        ArrayList<Float> floats = new ArrayList<>();

        // min
        floats.add( (float)Utils.minimum(magArray) );

        // max
        floats.add( (float)Utils.maximum(magArray) );

        // mean
        floats.add( (float)Utils.mean(magArray) );;

        // variance
        floats.add( (float)Utils.variance(magArray) );

        // std dev
        floats.add( (float)Utils.standardDeviation(magArray) );

        // zcr
        floats.add( (float)Utils.zeroCrossingRate(magArray) );

        // mcr
        floats.add( (float)Utils.meanCrossingsRate(magArray) );

        // energy
        floats.add( (float)Utils.energy(magArray) );

        // skew
        floats.add( (float)Utils.skew(magArray) );

        // kurtosis
        floats.add( (float)Utils.kurt(magArray) );

        // centroid
        floats.add( (float)Utils.centroid(magArray) );

        // rms
        floats.add( (float)Utils.rms(magArray) );

        // return Utils.toFloats2(floats);
        return floats;
    }

    // writes data statistics in CSV format to the provided StringBuffer
    //  returns flag if data was written
    public boolean statsToCSV( StringBuffer sb ) {

        if ( this.data.isEmpty() )
            return false;

        double[] magArray = Utils.toDoubles(this.data);

        // min
        sb.append( Utils.minimum(magArray) );
        sb.append(',');

        // max
        sb.append( Utils.maximum(magArray) );
        sb.append(',');

        // mean
        sb.append( Utils.mean(magArray) );
        sb.append(',');

        // variance
        sb.append( Utils.variance(magArray) );
        sb.append(',');

        // std dev
        sb.append( Utils.standardDeviation(magArray) );
        sb.append(',');

        // zcr
        sb.append( Utils.zeroCrossingRate(magArray) );
        sb.append(',');

        // mcr
        sb.append( Utils.meanCrossingsRate(magArray) );
        sb.append(',');

        // energy
        sb.append( Utils.energy(magArray) );
        sb.append(',');

        // skew
        sb.append( Utils.skew(magArray) );
        sb.append(',');

        // kurtosis
        sb.append( Utils.kurt(magArray) );
        sb.append(',');

        // centroid
        sb.append( Utils.centroid(magArray) );
        sb.append(',');

        // rms
        sb.append( Utils.rms(magArray) );
        sb.append(',');

        return true;
    }
}   // SensorDataAccumulator
