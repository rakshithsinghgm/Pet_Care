package com.example.pet_care;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.io.File;
import java.util.Scanner;
import java.util.StringTokenizer;


public class Utils {

    public static void deleteFile( Context cx, String fname ) {
        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);
        file.delete();
    }

    public static void appendToFile( Context cx, String fname, String contents ) {
        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);

        try {
            FileOutputStream fs = new FileOutputStream(file, true);
            fs.write( contents.getBytes() );
            fs.close();
        }
        catch ( Exception ex ){
            Log.e("tag", "msg", ex );
        }
    }

    public static StringBuffer readFile( Context cx, String fname) throws IOException {

        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);

        StringBuffer sb = new StringBuffer();
        try (Scanner scanner = new Scanner(file)) {
            while(scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
                sb.append('\n');
            }
        }

        return sb;
    }

    // based on https://stackoverflow.com/a/326448/882436
    /*
    public static List<String> readFile( Context cx, String fname) throws IOException {

        File path = cx.getExternalFilesDir(null);
        File file = new File(path, fname);

        List<String> results = new ArrayList<>();

        try (Scanner scanner = new Scanner(file)) {
            while(scanner.hasNextLine()) {
                results.add(scanner.nextLine());
            }
            return results;
        }
    }

     */

    public static void showMsg( Context cx, String msg ) {
        Toast.makeText( cx, msg, Toast.LENGTH_SHORT).show();
    }

    public static void showMsg( Context cx, Exception ex ) {
        ex.printStackTrace();
        showMsg( cx, ex.getMessage());
    }
}
