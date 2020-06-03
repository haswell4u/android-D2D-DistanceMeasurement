package com.example.distancemeasurement;

import android.Manifest;

public class Constants {

    public static final int PERMISSIONS_REQUEST_CODE = 1000;
    public static final String[] PERMISSIONS = {
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.CHANGE_WIFI_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
    };

    public static final String ACTION_INFORMATION_GENERATED = "com.example.distancemeasurement";
    public static final String INTENT_CONTENTS_NAME_TYPE = "type";
    public static final String INTENT_CONTENTS_NAME_MESSAGE = "message";
    public static final String INTENT_CONTENTS_NAME_DEVICE = "device";
    public static final int INTENT_CONTENTS_TYPE_MESSAGE = 100;
    public static final int INTENT_CONTENTS_TYPE_UPDATE_DEVICE = 200;
    public static final int INTENT_CONTENTS_DEFAULT_INT = -1;

    public static final String WIFI_AWARE_SERVICE_NAME = "Test";

    public static final int OFFSET_MESSAGE_FIND_NEW_USER = 12;

    public static final int PENDING_INTENT_REQUEST_CODE = 0;
    public static final int NOTIFICATION_ID = 10;
    public static final String NOTIFICATION_CHANNEL_ID = "com.example.distancemeasurement";

    public static final String PREFERENCES_NAME_FILE = "file";
    public static final boolean PREFERENCES_DEFAULT_FILE = false;
    public static final String PREFERENCES_NAME_FILENAME = "filename";
    public static final boolean PREFERENCES_DEFAULT_FILENAME = false;
    public static final String PREFERENCES_NAME_FILENAME_TEXT = "filename_text";
    public static final String PREFERENCES_DEFAULT_FILENAME_TEXT = "SNQz4YoYRL";
    public static final String PREFERENCES_NAME_DEVICE_ID = "id";
    public static final String PREFERENCES_DEFAULT_DEVICE_ID = "SNQz4YoYRL";
    public static final String PREFERENCES_NAME_INTERVAL = "interval";
    public static final String PREFERENCES_DEFAULT_INTERVAL = "-1";
    public static final String PREFERENCES_NAME_TIMEOUT = "timeout";
    public static final String PREFERENCES_DEFAULT_TIMEOUT = "-1";

    public static final String FILE_WRITE_EXTENSION = ".txt";
}
