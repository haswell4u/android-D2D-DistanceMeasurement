package com.example.distancemeasurement;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.aware.PeerHandle;
import android.net.wifi.rtt.RangingRequest;
import android.net.wifi.rtt.RangingResult;
import android.net.wifi.rtt.RangingResultCallback;
import android.net.wifi.rtt.WifiRttManager;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.preference.PreferenceManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class WifiRtt {

    private MeasurementService mMeasurementService;

    private SharedPreferences mSharedPreferences;

    private WifiRttManager mWifiRttManager;
    private RangingCallback mRangingCallback;

    private Timer mTimer;

    private boolean isMeasuring = false;

    public WifiRtt(MeasurementService service) {
        mMeasurementService = service;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mMeasurementService);

        mWifiRttManager = (WifiRttManager) mMeasurementService
                .getSystemService(Context.WIFI_RTT_RANGING_SERVICE);
        mRangingCallback = new RangingCallback();

        mTimer = new Timer();

        startRangingRequest();
    }

    public void startRangingRequest() {
        long interval = Long.parseLong(mSharedPreferences
                .getString(Constants.PREFERENCES_NAME_INTERVAL,
                        Constants.PREFERENCES_DEFAULT_INTERVAL));

        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (mMeasurementService.mPeerHandleList.size() != 0) {
                    if (!isMeasuring) {
                        isMeasuring = true;
                        try {
                            mWifiRttManager.startRanging(createRequest(),
                                    mMeasurementService.getMainExecutor(), mRangingCallback);
                        } catch (SecurityException e) {
                            mMeasurementService.sendBroadcast(
                                    new Intent(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED));
                        }
                    }
                }
            }
        }, interval, interval);
    }

    private Device createDevice(RangingResult rangingResult) {
        String id = getKeyByValue(mMeasurementService.mPeerHandleList,
                rangingResult.getPeerHandle());

        long time = System.currentTimeMillis();
        float dist = rangingResult.getDistanceMm() / 1000f;
        int std = rangingResult.getDistanceStdDevMm();
        int nam = rangingResult.getNumAttemptedMeasurements();
        int nsm = rangingResult.getNumSuccessfulMeasurements();
        int rssi = rangingResult.getRssi();

        Device device = new Device(id, time, dist, std, nam, nsm, rssi);

        return device;
    }

    private RangingRequest createRequest() {
        RangingRequest.Builder builder = new RangingRequest.Builder();

        for (Map.Entry<String, PeerHandle> entry : mMeasurementService.mPeerHandleList.entrySet())
            builder.addWifiAwarePeer(entry.getValue());

        return builder.build();
    }

    private void sendDevice(ArrayList<Device> devices) {
        mMeasurementService.sendBroadcast(new Intent(Constants.ACTION_INFORMATION_GENERATED)
                .putExtra(Constants.INTENT_CONTENTS_NAME_TYPE,
                        Constants.INTENT_CONTENTS_TYPE_UPDATE_DEVICE)
                .putExtra(Constants.INTENT_CONTENTS_NAME_DEVICE, devices));
    }

    private String getKeyByValue(HashMap<String, PeerHandle> map, PeerHandle target) {
        for (Map.Entry<String, PeerHandle> entry : map.entrySet()) {
            if (entry.getValue().equals(target))
                return entry.getKey();
        }
        return null;
    }

    public void close() {
        mTimer.cancel();
    }

    private class RangingCallback extends RangingResultCallback {
        @Override
        public void onRangingFailure(int code) {
            mMeasurementService.sendMessage(mMeasurementService
                    .getString(R.string.message_wifi_rtt_ranging_failure));
            isMeasuring = false;
        }

        @Override
        public void onRangingResults(@NonNull List<RangingResult> results) {
            ArrayList<Device> list = new ArrayList<Device>();

            for (RangingResult rangingResult : results) {
                if (rangingResult.getStatus() == RangingResult.STATUS_SUCCESS) {
                    list.add(createDevice(rangingResult));
                    mMeasurementService.sendMessage(mMeasurementService
                            .getString(R.string.message_wifi_rtt_ranging_status_success));
                }
                else
                    mMeasurementService.sendMessage(mMeasurementService
                            .getString(R.string.message_wifi_rtt_ranging_status_fail));
            }

            sendDevice(list);
            isMeasuring = false;
        }
    }
}
