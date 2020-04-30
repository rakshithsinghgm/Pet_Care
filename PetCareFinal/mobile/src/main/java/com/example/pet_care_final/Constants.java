package com.example.pet_care_final;

//make the class non-extendable by adding final
public final class Constants {
    //Hide the constructor
    private Constants(){}

    public static final int NUM_ACTIVITY_CLASSES = 3;
    public static final int ACTIVITY_CLASS_SLEEPING = 0;
    public static final int ACTIVITY_CLASS_INACTIVE = 1;
    public static final int ACTIVITY_CLASS_ACTIVE = 2;

    public static final String ACTIVITY_CLASS_SLEEPING_STRING = "Sleeping";
    public static final String ACTIVITY_CLASS_INACTIVE_STRING = "Inactive";
    public static final String ACTIVITY_CLASS_ACTIVE_STRING = "Active";

    // send this to tell the watch to publish (and reset) its current data
    public static final String REQUEST_DATA_PATH = "/pet-care-sensor-data-request";

    // the watch will publish to this path after it receives a publish command
    public static final String RESPONSE_DATA_PATH = "/pet-care-sensor-data-response";

    // send this to tell the watch to publish the current activity state
    public static final String REQUEST_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-request";

    // the watch will publish to this path after it receives a publish command
    public static final String RESPONSE_ACTIVITY_STATE_PATH = "/pet-care-sensor-activity-state-response";

}
