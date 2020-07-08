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
        sb.append(id + ": ");

        if (dist != Integer.MAX_VALUE)
            sb.append(String.format("%.2f", dist) + "m ");
        if (rssi_ble != Integer.MAX_VALUE)
            sb.append("(BLE RSSI = " + rssi_ble + ")");

        return sb.toString();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        Device newDevice = (Device) obj;
        return this.id.equals(newDevice.id);
    }

    public void update(Device device) {
        this.time = device.time;

        if (device.rssi_ble != Integer.MAX_VALUE)
            this.rssi_ble = device.rssi_ble;
        else if (device.dist != Integer.MAX_VALUE) {
            this.dist = device.dist;
            this.std = device.std;
            this.nam = device.nam;
            this.nsm = device.nsm;
            this.rssi = device.rssi;
        }
    }

    public String print() {
        StringBuffer sb = new StringBuffer();
        sb.append("[" + new SimpleDateFormat("MM/dd HH:mm:ss")
                .format(new Date(time)) + "] : " + id);

        if (dist != Integer.MAX_VALUE)
            sb.append(" " + dist + " " + std + " " + nam + " " + nsm + " " + rssi);

        if (rssi_ble != Integer.MAX_VALUE)
            sb.append(" " + rssi_ble);

        return sb.toString();
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }
}
