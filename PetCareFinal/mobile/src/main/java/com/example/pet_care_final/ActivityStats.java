package com.example.pet_care_final;

import android.provider.BaseColumns;

public class ActivityStats {

    private ActivityStats() {}
    public static final class StatsEntry implements BaseColumns{
        public static final String Table_Name = "stats";
        public static final String Time_Stamp = "timestamp";
        public static final String Active = "active";
        public static final String Inactive = "inactive";
        public static final String Sleeping = "sleeping";
        public static final String Distance = "distance";
    }
}
