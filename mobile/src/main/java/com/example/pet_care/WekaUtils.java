package com.example.pet_care;

import android.content.Context;

import org.w3c.dom.Attr;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Random;

import weka.classifiers.Classifier;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomForest;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;
import weka.core.SparseInstance;
import weka.classifiers.meta.FilteredClassifier;
import weka.core.converters.CSVLoader;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Evaluation;
import weka.core.pmml.jaxbbindings.DecisionTree;


import static com.example.pet_care.Utils.showMsg;
import static weka.core.SerializationHelper.read;

public class WekaUtils {

    // serialize a weka model, overwrites any existing file
    public static void writeFile(Context cx, String fname, Classifier cls ) throws Exception {

        // delete if already exists
        Utils.deleteFile(cx, fname);

        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);

        weka.core.SerializationHelper.write(file.getAbsolutePath(), cls);
    }

    public static Instances getWekaInstances( String csv ) throws Exception {

        // based on https://waikato.github.io/weka-wiki/use_weka_in_your_java_code/
        // File path = cx.getExternalFilesDir(null);
        // File file = new File(path, fname);

        InputStream csvStream = new ByteArrayInputStream(csv.getBytes());
        CSVLoader loader = new CSVLoader();
        loader.setNoHeaderRowPresent(true);
        //loader.setSource(file);
        loader.setSource( csvStream );
        Instances data = loader.getDataSet();
        data.setClassIndex(data.numAttributes() - 1);

        return data;
    }

    public static List<String> getClassLabels( Instances data ) {
        Enumeration<Object> objectEnumeration = data.classAttribute().enumerateValues();
        ArrayList<String> result = new ArrayList<String>();

        while ( objectEnumeration.hasMoreElements() ) {
            result.add((String)objectEnumeration.nextElement());
        }

        return result;
    }

    public static Classifier loadClassifier( Context cx, String fname ) throws Exception {
        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);
        return (Classifier)weka.core.SerializationHelper.read(file.getAbsolutePath());
    }

    public static J48 getJ48Classifier( Instances data ) throws Exception {
        J48 tree = new J48();         // new instance of tree
        tree.setUnpruned(true);
        tree.buildClassifier(data);   // build classifier
        return tree;
    }

    public static NaiveBayes getNBClassifier( Instances data ) throws Exception {
        NaiveBayes result = new NaiveBayes();
        result.buildClassifier(data);
        return result;
    }

    public static RandomForest getRFClassifier( Instances data ) throws Exception {
        RandomForest result = new RandomForest();
        result.buildClassifier(data);
        return result;
    }

    public static double evalClassifier( Classifier cls, Instances data ) throws Exception {
        Evaluation eval = new Evaluation(data);
        eval.crossValidateModel(cls, data, 10, new Random(1));
        //eval.evaluateModel(cls, data);
        return eval.pctCorrect();
    }

    // based on provided Lecture 6 codes
    // usage:
    //      double [] detected=classification(min,max,var,std);
    //      text1.setText("class: " +String.valueOf(detected[0])+" probabilities: "+String.valueOf(detected[1]));
    public static Map.Entry<String,Double> classification(Classifier cls, List<String> labels, Double[] data ) throws Exception {

        double [] predicted_class =new double[2];
        predicted_class[0]=0.0;
        predicted_class[1]=0.0;

        ArrayList<Attribute> attributes = new ArrayList<>();

        Instance instance = new SparseInstance( data.length - 1 );
        for ( int i = 0; i < data.length; i++) {
            attributes.add(new Attribute("a" + String.valueOf(i), i));
            instance.setValue(attributes.get(i), data[i]);
        }

        attributes.add(new Attribute("label", labels,data.length));

        Instances datasetConfiguration;
        datasetConfiguration = new Instances("data", attributes, 0);

        datasetConfiguration.setClassIndex( attributes.size() - 1 );
        instance.setDataset(datasetConfiguration);

        double[] distribution;
        distribution = cls.distributionForInstance(instance);

        return new AbstractMap.SimpleEntry<String,Double>(
                labels.get((int)cls.classifyInstance(instance))
                , Math.max(distribution[0], distribution[1])*100
        );
    }
}
