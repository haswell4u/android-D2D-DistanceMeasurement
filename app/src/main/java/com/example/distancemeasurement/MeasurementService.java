package com.example.distancemeasurement;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.aware.PeerHandle;
import android.os.IBinder;

import androidx.preference.PreferenceManager;

import com.example.distancemeasurement.Constants;
import com.example.distancemeasurement.Device;
import com.example.distancemeasurement.R;
import com.example.distancemeasurement.methods.Bluetooth;
import com.example.distancemeasurement.methods.WifiAware;
import com.example.distancemeasurement.methods.WifiRtt;
import com.example.distancemeasurement.views.MainActivity;

import java.util.ArrayList;
import java.util.HashMap;

public class MeasurementService extends Service {

    private WifiAware mWifiAware;
    private WifiRtt mWifiRtt;
    private Bluetooth mBluetooth;

    private boolean isInitialized = false;

    private SharedPreferences mSharedPreferences;

    public HashMap<String, PeerHandle> mPeerHandleList = new HashMap<String, PeerHandle>();

    @Override
    public void onCreate() {
        super.onCreate();
        goForeground();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        if (mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_WIFI_AWARE,
                Constants.PREFERENCES_DEFAULT_BOOLEAN))
            mWifiAware = new WifiAware(this);

        if (mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_BLE,
                Constants.PREFERENCES_DEFAULT_BOOLEAN))
            mBluetooth = new Bluetooth(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isInitialized) {
            if (mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_WIFI_AWARE,
                    Constants.PREFERENCES_DEFAULT_BOOLEAN))
                mWifiAware.checkAlive();
            if (mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_WIFI_RTT,
                    Constants.PREFERENCES_DEFAULT_BOOLEAN))
                mWifiRtt = new WifiRtt(this);
            if (mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_BLE,
                    Constants.PREFERENCES_DEFAULT_BOOLEAN))
                mBluetooth.startBluetoothLE();
        }
        else
            isInitialized = true;

        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mWifiRtt != null)
            mWifiRtt.close();
        if (mWifiAware != null)
            mWifiAware.close();
        if (mBluetooth != null)
            mBluetooth.close();
        stopForeground(true);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void goForeground() {
        PendingIntent pendingIntent = PendingIntent.getActivity(this,
                Constants.PENDING_INTENT_REQUEST_CODE,
                new Intent(this, MainActivity.class), 0);
        createNotificationChannel();
        Notification notification = createNotification(pendingIntent);
        startForeground(Constants.NOTIFICATION_ID, notification);
    }

    private Notification createNotification(PendingIntent pendingIntent) {
        Notification n = new Notification.Builder(this, Constants.NOTIFICATION_CHANNEL_ID)
                .setContentTitle(getString(R.string.notification_title))
                .setContentText(getString(R.string.notification_text))
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(pendingIntent)
                .build();
        return n;
    }

    private void createNotificationChannel() {
        CharSequence name = getString(R.string.notification_channel_name);
        NotificationChannel channel = new NotificationChannel(Constants.NOTIFICATION_CHANNEL_ID,
                name, NotificationManager.IMPORTANCE_LOW);
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    public void sendMessage(String message) {
        sendBroadcast(new Intent(Constants.ACTION_INFORMATION_GENERATED)
                .putExtra(Constants.INTENT_CONTENTS_NAME_TYPE,
                        Constants.INTENT_CONTENTS_TYPE_MESSAGE)
                .putExtra(Constants.INTENT_CONTENTS_NAME_MESSAGE, message));
    }

    public void sendError(String message) {
        sendBroadcast(new Intent(Constants.ACTION_INFORMATION_GENERATED)
                .putExtra(Constants.INTENT_CONTENTS_NAME_TYPE,
                        Constants.INTENT_CONTENTS_TYPE_ERROR)
                .putExtra(Constants.INTENT_CONTENTS_NAME_ERROR, message));
    }

    public void sendDevice(ArrayList<Device> devices) {
        sendBroadcast(new Intent(Constants.ACTION_INFORMATION_GENERATED)
                .putExtra(Constants.INTENT_CONTENTS_NAME_TYPE,
                        Constants.INTENT_CONTENTS_TYPE_UPDATE_DEVICE)
                .putExtra(Constants.INTENT_CONTENTS_NAME_DEVICE, devices));
    }
}
