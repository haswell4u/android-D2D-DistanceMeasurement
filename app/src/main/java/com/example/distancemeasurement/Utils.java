package com.example.distancemeasurement;

import java.io.UnsupportedEncodingException;

public class Utils {

    public static byte[] dataEncoding(String data) {
        byte[] bytes = null;

        try {
            bytes = data.getBytes("UTF-8");
        }
        catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        return bytes;
    }

    public static String dataDecoding(byte[] data) {
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
