package com.example.distancemeasurement;

import androidx.annotation.Nullable;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Device implements Serializable {

    private String id;
    private long time;
    private float dist = Integer.MAX_VALUE;
    private int std;
    private int nam;
    private int nsm;
    private int rssi;
    private int rssi_ble = Integer.MAX_VALUE;

    public Device(String id, long time, float dist, int std, int nam, int nsm, int rssi) {
        this.id = id;
        this.time = time;
        this.dist = dist;
        this.std = std;
        this.nam = nam;
        this.nsm = nsm;
        this.rssi = rssi;
    }

    public Device(String id, long time, int rssi_ble) {
        this.id = id;
        this.time = time;
        this.rssi_ble = rssi_ble;
    }

    public Device(String id, long time) {
        this.id = id;
        this.time = time;
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Device ID: " + id + "\n");

        if (fromRTT(this))
            sb.append("Distance measured by WiFi RTT: " + String.format("%.2f", dist) + "m" + "\n");
        if (fromBLE(this))
            sb.append("RSSI measured by BLE: " + rssi_ble + "\n");

        return sb.substring(0, sb.length() - 1);
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Device newDevice = (Device) obj;
        return this.id.equals(newDevice.id);
    }

    private boolean fromRTT(Device device) {
        return device.dist != Integer.MAX_VALUE;
    }

    private boolean fromBLE(Device device) {
        return device.rssi_ble != Integer.MAX_VALUE;
    }

    public void update(Device device) {
        this.time = device.time;

        if (fromRTT(device)) {
            this.dist = device.dist;
            this.std = device.std;
            this.nam = device.nam;
            this.nsm = device.nsm;
            this.rssi = device.rssi;
        }
        else if (fromBLE(device))
            this.rssi_ble = device.rssi_ble;
    }

    public String print() {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + new SimpleDateFormat("MM/dd HH:mm:ss")
                .format(new Date(time)) + "] - Device ID: " + id);

        if (fromRTT(this))
            sb.append(", Distance: " + dist
                    + ", Standard deviation: " + std
                    + ", # of Attempted Measurements: " + nam
                    + ", # of Successful Measurements: " + nsm
                    + ", RSSI from WiFi RTT: " + rssi);
        if (fromBLE(this))
            sb.append(", RSSI from BLE: " + rssi_ble);

        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }
}
