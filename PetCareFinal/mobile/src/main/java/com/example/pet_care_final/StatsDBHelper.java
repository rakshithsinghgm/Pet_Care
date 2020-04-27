package com.example.pet_care_final;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;
import com.example.pet_care_final.ActivityStats.*;

public class StatsDBHelper extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "statsdb.db";
    public static final int DATABASE_VERSION = 1;

    public StatsDBHelper(@Nullable Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        final String SQL_STATS_DB_TABLE = "CREATE TABLE " +
                StatsEntry.Table_Name + " (" +
                StatsEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                StatsEntry.Time_Stamp + " STRING, " +
                StatsEntry.Active + " INTEGER, " +
                StatsEntry.Inactive + " INTEGER, " +
                StatsEntry.Sleeping + " INTEGER" +
                ");";
        db.execSQL(SQL_STATS_DB_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

        db.execSQL("DROP TABLE IF EXISTS " + StatsEntry.Table_Name);
        onCreate(db);

    }
}
