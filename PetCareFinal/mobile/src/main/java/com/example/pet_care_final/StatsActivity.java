package com.example.pet_care_final;

import androidx.appcompat.app.AppCompatActivity;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.utils.ColorTemplate;

import org.w3c.dom.Text;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;

public class StatsActivity extends AppCompatActivity {

    PieChart pieChart;
    PieData pieData;
    PieDataSet pieDataSet;
    ArrayList pieEntries;
    ArrayList PieEntryLabels;

    String date;
    SQLiteDatabase statsdb;
    Cursor cur = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();
        pieChart = findViewById(R.id.pieChart);

        Calendar cal = Calendar.getInstance();
        date = DateFormat.getDateInstance(DateFormat.FULL).format(cal.getTime());

        DispStats();

    }

    public void DispStats() {
        StatsDBHelper dbHelper = new StatsDBHelper(this);
        statsdb = dbHelper.getWritableDatabase();

        String SQL_READ_QUERY = "SELECT * FROM " + ActivityStats.StatsEntry.Table_Name + " WHERE " + ActivityStats.StatsEntry.Time_Stamp + " = ?";
        cur = statsdb.rawQuery(SQL_READ_QUERY, new String[]{String.valueOf(date)});
        cur.moveToFirst();

        int active_time = 0;
        int inactive_time =0;
        int sleeping_time = 0;
        String time_stamp = "";

        while (!cur.isAfterLast()) {
            active_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Active));
            inactive_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Inactive));
            sleeping_time = cur.getInt(cur.getColumnIndex(ActivityStats.StatsEntry.Sleeping));
            time_stamp = cur.getString(cur.getColumnIndex(ActivityStats.StatsEntry.Time_Stamp));
            cur.moveToNext();
        }

        TextView heading;
        heading = findViewById(R.id.heading);
        heading.setText("Stats for "+date+" is ");
        getEntries(active_time,inactive_time,sleeping_time);
        disp_graph();
    }

    private void getEntries(int a,int b, int c) {
        pieEntries = new ArrayList<>();
        a += 777;
        b += 888;
        c += 999;
        pieEntries.add(new PieEntry(a, 0));
        pieEntries.add(new PieEntry(b, 1));
        pieEntries.add(new PieEntry(c, 2));
    }

    public void disp_graph(){
        pieDataSet = new PieDataSet(pieEntries, "Activity Stats");
        pieData = new PieData(pieDataSet);
        pieChart.setData(pieData);
        pieDataSet.setColors(ColorTemplate.JOYFUL_COLORS);
        pieDataSet.setSliceSpace(2f);
        pieDataSet.setValueTextColor(Color.BLACK);
        pieDataSet.setValueTextSize(17f);
        pieDataSet.setSliceSpace(5f);
    }
}
