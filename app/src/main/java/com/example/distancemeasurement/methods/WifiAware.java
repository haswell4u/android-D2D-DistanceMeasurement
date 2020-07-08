package com.example.distancemeasurement.methods;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.wifi.aware.AttachCallback;
import android.net.wifi.aware.DiscoverySessionCallback;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.aware.PublishConfig;
import android.net.wifi.aware.PublishDiscoverySession;
import android.net.wifi.aware.SubscribeConfig;
import android.net.wifi.aware.SubscribeDiscoverySession;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.aware.WifiAwareSession;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.example.distancemeasurement.Constants;
import com.example.distancemeasurement.Device;
import com.example.distancemeasurement.MeasurementService;
import com.example.distancemeasurement.R;
import com.example.distancemeasurement.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class WifiAware {

    private MeasurementService mMeasurementService;

    private SharedPreferences mSharedPreferences;

    private WifiAwareManager mWifiAwareManager;
    private WifiAwareSession mWifiAwareSession;

    private PublishDiscoverySession mPublishDiscoverySession;
    private SubscribeDiscoverySession mSubscribeDiscoverySession;

    private Timer mTimer;

    public WifiAware(MeasurementService service) {
        mMeasurementService = service;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mMeasurementService);

        mWifiAwareManager = (WifiAwareManager) mMeasurementService
                .getSystemService(Context.WIFI_AWARE_SERVICE);

        mTimer = new Timer();

        obtainSession();
    }

    private void obtainSession() {
        if (mWifiAwareManager.isAvailable())
            mWifiAwareManager.attach(new AttachCallback() {
                @Override
                public void onAttached(WifiAwareSession session) {
                    super.onAttached(session);
                    mWifiAwareSession = session;
                    mMeasurementService.sendMessage(mMeasurementService.getString(R.string.message_obtain_session));
                    publishService();
                    subscribeService();
                }
            }, null);
        else
            mMeasurementService.sendError(mMeasurementService
                    .getString(R.string.message_wifi_aware_state_changed));
    }

    private void publishService() {
        PublishConfig config = new PublishConfig.Builder()
                .setServiceSpecificInfo(Utils.dataEncoding(mSharedPreferences
                        .getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                                Constants.PREFERENCES_DEFAULT_DEVICE_ID)))
                .setServiceName(Constants.WIFI_AWARE_SERVICE_NAME)
                .build();

        mWifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(PublishDiscoverySession session) {
                mPublishDiscoverySession = session;
                mMeasurementService.sendMessage(mMeasurementService
                        .getString(R.string.message_publish_service));
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                mPublishDiscoverySession.sendMessage(peerHandle,
                        Constants.WIFI_AWARE_CHECK_ALIVE_ID,
                        Utils.dataEncoding(mSharedPreferences
                                .getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                                        Constants.PREFERENCES_DEFAULT_DEVICE_ID)));
            }
        }, null);
    }

    private void subscribeService() {
        SubscribeConfig config = new SubscribeConfig.Builder()
                .setServiceName(Constants.WIFI_AWARE_SERVICE_NAME)
                .build();

        mWifiAwareSession.subscribe(config, new DiscoverySessionCallback() {
            @Override
            public void onSubscribeStarted(SubscribeDiscoverySession session) {
                mSubscribeDiscoverySession = session;
                mMeasurementService.sendMessage(mMeasurementService
                        .getString(R.string.message_subscribe_service));
            }

            @Override
            public void onServiceDiscovered(PeerHandle peerHandle,
                                            byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                String id = Utils.dataDecoding(serviceSpecificInfo);
                synchronized (mMeasurementService.mPeerHandleList) {
                    if (!mMeasurementService.mPeerHandleList.containsKey(id)) {
                        mMeasurementService.mPeerHandleList.put(id, peerHandle);
                        mMeasurementService.sendMessage(createFindUserMessage(id));
                    }
                    else
                        mMeasurementService.mPeerHandleList.replace(id, peerHandle);
                }
            }

            @Override
            public void onMessageReceived(PeerHandle peerHandle, byte[] message) {
                super.onMessageReceived(peerHandle, message);
                ArrayList<Device> list = new ArrayList<Device>();
                list.add(createDevice(Utils.dataDecoding(message)));
                mMeasurementService.sendMessage(createFindUserMessage(Utils.dataDecoding(message)));
                mMeasurementService.sendDevice(list);
            }
        }, null);
    }

    public void checkAlive() {
        long interval = Long.parseLong(mSharedPreferences
                .getString(Constants.PREFERENCES_NAME_REFRESH,
                        Constants.PREFERENCES_DEFAULT_REFRESH));

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                synchronized (mMeasurementService.mPeerHandleList) {
                    for (String id : mMeasurementService.mPeerHandleList.keySet())
                        mSubscribeDiscoverySession.sendMessage(
                                mMeasurementService.mPeerHandleList.get(id),
                                Constants.WIFI_AWARE_CHECK_ALIVE_ID,
                                Utils.dataEncoding(mSharedPreferences
                                        .getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                                                Constants.PREFERENCES_DEFAULT_DEVICE_ID)));
                }
            }
        }, interval, interval);
    }

    public Device createDevice(String id) {
        long time = System.currentTimeMillis();
        return new Device(id, time);
    }

    private String createFindUserMessage(String id) {
        StringBuffer str = new StringBuffer(mMeasurementService
                .getString(R.string.message_find_new_user));
        str.insert(Constants.OFFSET_MESSAGE_FIND_REMOVE_USER, id);
        return str.toString();
    }

    public void close() {
        mTimer.cancel();
        if (mSubscribeDiscoverySession != null)
            mSubscribeDiscoverySession.close();
        if (mPublishDiscoverySession != null)
            mPublishDiscoverySession.close();
        if (mWifiAwareSession != null) {
            mWifiAwareSession.close();
            mMeasurementService.sendMessage(mMeasurementService.getString(R.string.message_close_session));
        }
    }
}
