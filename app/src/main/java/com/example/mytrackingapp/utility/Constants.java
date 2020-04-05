package com.example.mytrackingapp.utility;

public class Constants {

    public static final String APP_NAME = "MyTrackingApp";
    public static final String NOTIFICATION_CHANNEL_ID = "trackerNotification";
    public static final String NOTIFICATION_CHANNEL_NAME = "Tracking Service";
    public static final String ACTIVITY = "activity";
    public static final String EXTRA = "extra";

    //ContinuousLocationAndActivityTracking
    public static final String STATIONARY = "stationary";
    public static final String WALKING = "walking";
    public static final String RUNNING = "running";
    public static final String AUTOMOTIVE = "automotive";
    public static final String CYCLING = "cycling";
    public static final String DISTANCE = "distance";
    public static final String UNKNOWN = "unknown";
    public static final String CONFIDENCE = "confidence";
    public final static String SPEED = "speed";
    public static final String LOCATION_CLIENT_NUll = "Location client is null";
    public static final String HANDLE_DETECTED_ACTIVITY = "handleDetectedActivity";
    public static final String LOCATION_PROVIDE_CHANGE_INTENT_FILTER = "android.location.PROVIDERS_CHANGED";

    //Display/Log text
    public static final String NO_ACCOUNT_ID_PASSED = "No accountId passed to the service, stopping service";
    public static final String TRACKING_ALREADY_STARTED = "Tracking is already started, skipping 'START SERVICE' call";
    public static final String STOP_SERVICE_PREPARE_DATA = "Stopping service while prepareDataToSave, context is null";
}
