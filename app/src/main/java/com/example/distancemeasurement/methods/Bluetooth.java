package com.example.distancemeasurement.methods;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import com.example.distancemeasurement.Constants;
import com.example.distancemeasurement.Device;
import com.example.distancemeasurement.MeasurementService;
import com.example.distancemeasurement.R;
import com.example.distancemeasurement.Utils;

import java.util.ArrayList;
import java.util.List;

public class Bluetooth {

    private MeasurementService mMeasurementService;

    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
    private BluetoothLeScanner mBluetoothLeScanner;

    private SharedPreferences mSharedPreferences;

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            mBluetoothLeScanner.startScan(buildScanFilters(), buildScanSettings(), mScanCallback);
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            mMeasurementService.sendError(mMeasurementService
                    .getString(R.string.error_message_ble_adv_fail));
        }
    };

    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);

            String id = Utils.dataDecoding(result.getScanRecord()
                    .getServiceData(Constants.BLE_SERVICE_UUID));

            ArrayList<Device> list = new ArrayList<Device>();
            list.add(createDevice(id, result));
            mMeasurementService.sendMessage(createFindUserMessage(id));

            mMeasurementService.sendDevice(list);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
            mMeasurementService.sendError(mMeasurementService
                    .getString(R.string.error_message_ble_scn_fail));
        }
    };

    public Device createDevice(String id, ScanResult result) {
        long time = System.currentTimeMillis();
        int rssi_ble = result.getRssi();
        return new Device(id, time, rssi_ble);
    }

    private String createFindUserMessage(String id) {
        StringBuffer str = new StringBuffer(mMeasurementService
                .getString(R.string.message_find_new_user_ble));
        str.insert(Constants.OFFSET_MESSAGE_FIND_REMOVE_USER, id);
        return str.toString();
    }

    public Bluetooth(MeasurementService service) {
        mMeasurementService = service;

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mMeasurementService);

        mBluetoothManager = (BluetoothManager) mMeasurementService
                .getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();

        checkCondition();
    }

    private void checkCondition() {
        if (mBluetoothAdapter != null)
            if (mBluetoothAdapter.isEnabled())
                if (mBluetoothAdapter.isMultipleAdvertisementSupported()) {
                    mBluetoothLeAdvertiser = mBluetoothAdapter.getBluetoothLeAdvertiser();
                    mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
                    mMeasurementService.sendMessage(mMeasurementService
                            .getString(R.string.message_ble_ready));
                }
                else
                    mMeasurementService.sendError(mMeasurementService
                            .getString(R.string.error_message_ble_adv));
            else
                mMeasurementService.sendError(mMeasurementService
                        .getString(R.string.error_message_ble_turn_on));
        else
            mMeasurementService.sendError(mMeasurementService
                    .getString(R.string.error_message_ble));
    }

    public void startBluetoothLE() {
        AdvertiseSettings settings = buildAdvertiseSettings();
        AdvertiseData data = buildAdvertiseData();

        mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
    }

    private AdvertiseSettings buildAdvertiseSettings() {
        AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
        settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_POWER);
        settingsBuilder.setTimeout(0);
        return settingsBuilder.build();
    }

    private AdvertiseData buildAdvertiseData() {
        AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
        dataBuilder.addServiceUuid(Constants.BLE_SERVICE_UUID);
        dataBuilder.setIncludeDeviceName(true);

        dataBuilder.addServiceData(Constants.BLE_SERVICE_UUID,
                Utils.dataEncoding(mSharedPreferences
                        .getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                                Constants.PREFERENCES_DEFAULT_DEVICE_ID)));

        return dataBuilder.build();
    }

    private List<ScanFilter> buildScanFilters() {
        List<ScanFilter> scanFilters = new ArrayList<ScanFilter>();

        ScanFilter.Builder builder = new ScanFilter.Builder();
        builder.setServiceUuid(Constants.BLE_SERVICE_UUID);
        scanFilters.add(builder.build());

        return scanFilters;
    }

    private ScanSettings buildScanSettings() {
        ScanSettings.Builder builder = new ScanSettings.Builder();
        builder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);
        return builder.build();
    }

    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser != null) {
            mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
            mAdvertiseCallback = null;
            mMeasurementService.sendMessage(mMeasurementService
                    .getString(R.string.message_ble_adv_stop));
        }
    }

    public void stopScanning() {
        if (mBluetoothLeScanner != null) {
            mBluetoothLeScanner.stopScan(mScanCallback);
            mScanCallback = null;
            mMeasurementService.sendMessage(mMeasurementService
                    .getString(R.string.message_ble_scn_stop));
        }
    }

    public void close() {
        stopScanning();
        stopAdvertising();
    }
}
