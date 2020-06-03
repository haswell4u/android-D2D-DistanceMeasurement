package com.example.distancemeasurement;

import java.io.Serializable;

public class Device implements Serializable {

    private String id;
    private long time;
    private float dist;
    private int std;
    private int nam;
    private int nsm;
    private int rssi;

    public Device(String id, long time, float dist, int std, int nam, int nsm, int rssi) {
        this.id = id;
        this.time = time;
        this.dist = dist;
        this.std = std;
        this.nam = nam;
        this.nsm = nsm;
        this.rssi = rssi;
    }

    @Override
    public String toString() {
        return id + ": " + String.format("%.2f", dist) + "m";
    }

    public void update(Device device) {
        this.id = device.id;
        this.time = device.time;
        this.dist = device.dist;
        this.std = device.std;
        this.nam = device.nam;
        this.nsm = device.nsm;
        this.rssi = device.rssi;
    }

    public String print() {
        return id + " " + time + " " + dist + " " + std + " " + nam + " " + nsm + " " + rssi;
    }

    public String getId() {
        return id;
    }

    public long getTime() {
        return time;
    }
}
