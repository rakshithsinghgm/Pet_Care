package com.example.pet_care;
import java.util.List;

public class Utils {
    public static double maximum(double data[]){
        if(data == null || data.length == 0) return 0.0;
        int length = data.length;
        double MAX = data[0];
        for (int i = 1; i < length; i++){
            MAX = data[i]>MAX?data[i]:MAX;
        }
        return MAX;
    }

    public static double minimum(double data[]){
        if(data == null || data.length == 0) return 0.0;
        int length = data.length;
        double MIN = data[0];
        for (int i = 1; i < length; i++){
            MIN = data[i]<MIN?data[i]:MIN;
        }
        return MIN;
    }

    public static double mean(double data[]){
        if(data == null || data.length == 0) return
                0.0;
        int length = data.length;
        double Sum = 0;
        for (int i = 0; i < length; i++)
            Sum = Sum + data[i];
        return Sum / length;
    }

    public static double variance(double data[]){
        if(data == null || data.length == 0) return 0.0;
        int length = data.length;
        double average = 0, s = 0, sum = 0;
        for (int i = 0; i<length; i++)
        {
            sum = sum + data[i];
        }
        average = sum / length;
        for (int i = 0; i<length; i++)
        {
            s = s + Math.pow(data[i] - average, 2);
        }
        s = s / length;
        return s;
    }

    public static double standardDeviation(double data[]){
        if(data == null || data.length == 0) return 0.0;
        double s = variance(data);
        s = Math.sqrt(s);
        return s;
    }

    public static double zeroCrossingRate(double data[]){
        int length = data.length;
        double num = 0;
        for (int i = 0; i < length - 1; i++)
        {
            if (data[i] * data[i + 1]< 0){
                num++;
            }
        }
        return num / (double)length;
    }

    public static double meanCrossingsRate(double data[]){
        if(data == null || data.length == 0) return 0.0;
        int length = data.length;
        double Sum = 0;
        double num = 0;
        double[] copydata = new double[length];
        for (int i = 0; i < length; i++)
        {
            copydata[i] = data[i];
            Sum +=copydata[i];
        }
        double avg = Sum/length;
        for (int i = 0; i < length; i++)
        {
            copydata[i] = copydata[i] - avg;
        }
        for (int i = 0; i < length - 1; i++)
        {
            if (copydata[i] * copydata[i + 1]< 0){
                num++;
            }
        }
        return num / length;
    }

    public static double energy(double[] data){
        if(data == null || data.length == 0) return 0.0;
        double sum = 0;
        for(int i=0;i<data.length;i++){
            sum+= Math.pow(data[i],2);
        }
        return sum/data.length;
    }

    public static double skew(double[] data){
        if(data == null || data.length == 0) return 0.0;
        double mean = mean(data);
        double dev = standardDeviation(data);
        double sum=0;
        for(int i=0;i<data.length;i++){
            sum+= Math.pow((data[i]-mean)/dev,3);
        }
        return sum/data.length;
    }

    public static double kurt(double[] data){
        if(data == null || data.length == 0) return 0.0;
        double mean = mean(data);
        double dev = standardDeviation(data);
        double sum=0;
        for(int i=0;i<data.length;i++){
            sum+= Math.pow((data[i]-mean)/dev,4);
        }
        return sum/data.length-3;
    }

    public static double centroid(double[] data){
        if(data == null || data.length == 0) return 0.0;
        double sum1 = 0;
        double sum2 = 0;
        double temp;
        double tempPow;
        for(int i=0;i<data.length;i++){
            temp = data[i];
            tempPow = temp*temp;
            sum1+=tempPow;
            sum2+=tempPow*i;
        }
        return sum2/sum1;
    }

    public static double rms(double[] list){
        double rms=0;
        double sum = 0;
        for(int i=0;i<list.length;i++){
            sum+= Math.pow(list[i],2);
        }
        rms = Math.sqrt(sum/list.length);
        return rms;
    }

    // https://stackoverflow.com/a/6018431/882436
    public static double[] toDoubles(List<Double> doubles ) {
        double[] target = new double[doubles.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = doubles.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }

    public static float[] toFloats2(List<Float> vals ) {
        float[] target = new float[vals.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = (float)vals.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }

    public static float[] toFloats(List<Double> doubles ) {
        float[] target = new float[doubles.size()];
        for (int i = 0; i < target.length; i++) {
            target[i] = (float)(double)doubles.get(i);                // java 1.5+ style (outboxing)
        }
        return target;
    }
}
