package com.example.distancemeasurement;

import android.content.Context;
import android.content.Intent;
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
import androidx.preference.PreferenceManager;
import android.util.Log;

import java.io.UnsupportedEncodingException;
import java.util.List;

public class WifiAware {

    private MeasurementService mMeasurementService;

    private SharedPreferences mSharedPreferences;

    private WifiAwareManager mWifiAwareManager;
    private WifiAwareSession mWifiAwareSession;

    private PublishDiscoverySession mPublishDiscoverySession;
    private SubscribeDiscoverySession mSubscribeDiscoverySession;

    public WifiAware(MeasurementService service) {
        mMeasurementService = service;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mMeasurementService);

        mWifiAwareManager = (WifiAwareManager) mMeasurementService
                .getSystemService(Context.WIFI_AWARE_SERVICE);

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
            mMeasurementService.sendBroadcast(
                    new Intent(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED));
    }

    private void publishService() {
        PublishConfig config = new PublishConfig.Builder()
                .setServiceSpecificInfo(dataEncoding(mSharedPreferences
                        .getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                                Constants.PREFERENCES_DEFAULT_DEVICE_ID)))
                .setServiceName(Constants.WIFI_AWARE_SERVICE_NAME)
                .setRangingEnabled(true)
                .build();

        mWifiAwareSession.publish(config, new DiscoverySessionCallback() {
            @Override
            public void onPublishStarted(PublishDiscoverySession session) {
                mPublishDiscoverySession = session;
                mMeasurementService.sendMessage(mMeasurementService.getString(R.string.message_publish_service));
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
                mMeasurementService.sendMessage(mMeasurementService.getString(R.string.message_subscribe_service));
            }

            @Override
            public void onServiceDiscovered(PeerHandle peerHandle, byte[] serviceSpecificInfo, List<byte[]> matchFilter) {
                String id = dataDecoding(serviceSpecificInfo);
                synchronized (mMeasurementService.mPeerHandleList) {
                    if (!mMeasurementService.mPeerHandleList.containsKey(id)) {
                        mMeasurementService.mPeerHandleList.put(id, peerHandle);
                        mMeasurementService.sendMessage(createFindNewUserMessage(id));
                    }
                    else
                        mMeasurementService.mPeerHandleList.replace(id, peerHandle);
                }
            }
        }, null);
    }

    private String createFindNewUserMessage(String id) {
        StringBuffer str = new StringBuffer(mMeasurementService
                .getString(R.string.message_find_new_user));
        str.insert(Constants.OFFSET_MESSAGE_FIND_NEW_USER, id);
        return str.toString();
    }

    public void close() {
        if (mSubscribeDiscoverySession != null)
            mSubscribeDiscoverySession.close();
        if (mPublishDiscoverySession != null)
            mPublishDiscoverySession.close();
        if (mWifiAwareSession != null) {
            mWifiAwareSession.close();
            mMeasurementService.sendMessage(mMeasurementService.getString(R.string.message_close_session));
        }
    }

    private byte[] dataEncoding(String data) {
        byte[] bytes = null;

        try {
            bytes = data.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    private String dataDecoding(byte[] data) {
        String str = null;

        try {
            str = new String(data, "UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return str;
    }
}