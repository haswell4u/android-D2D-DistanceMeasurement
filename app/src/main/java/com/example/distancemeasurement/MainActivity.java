package com.example.distancemeasurement;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.aware.WifiAwareManager;
import android.net.wifi.rtt.WifiRttManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Toolbar mToolbar;
    private ListView mListView;
    private ScrollView mScrollView;
    private TextView mTextView;
    private Button mInitButton;
    private Button mStartButton;
    private Button mStopButton;

    private ArrayAdapter<Device> mAdapter;

    private Intent mServiceIntent;

    private SharedPreferences mSharedPreferences;

    private PrintWriter mPrintWriter;

    private BroadcastReceiver mServiceBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int type = intent.getIntExtra(Constants.INTENT_CONTENTS_NAME_TYPE,
                    Constants.INTENT_CONTENTS_DEFAULT_INT);

            if (type == Constants.INTENT_CONTENTS_TYPE_MESSAGE)
                addTextToTextView(intent.getStringExtra(Constants.INTENT_CONTENTS_NAME_MESSAGE));
            else if (type == Constants.INTENT_CONTENTS_TYPE_ERROR) {
                actionStopButton();
                addTextToTextView(intent.getStringExtra(Constants.INTENT_CONTENTS_NAME_ERROR));
            }
            else if (type == Constants.INTENT_CONTENTS_TYPE_UPDATE_DEVICE) {
                @SuppressWarnings("unchecked")
                ArrayList<Device> list = (ArrayList<Device>) intent
                        .getSerializableExtra(Constants.INTENT_CONTENTS_NAME_DEVICE);

                updateListView(list);
            }
        }
    };

    private BroadcastReceiver mWifiAwareBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            actionStopButton();
            addTextToTextView(getString(R.string.message_wifi_aware_state_changed));
        }
    };

    private BroadcastReceiver mWifiRttBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            actionStopButton();
            addTextToTextView(getString(R.string.message_wifi_rtt_state_changed));
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mServiceIntent = new Intent(this, MeasurementService.class);

        registerReceiver(mServiceBroadcastReceiver,
                new IntentFilter(Constants.ACTION_INFORMATION_GENERATED));
        registerReceiver(mWifiAwareBroadcastReceiver,
                new IntentFilter(WifiAwareManager.ACTION_WIFI_AWARE_STATE_CHANGED));
        registerReceiver(mWifiRttBroadcastReceiver,
                new IntentFilter(WifiRttManager.ACTION_WIFI_RTT_STATE_CHANGED));

        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_AWARE))
            closeApp(getString(R.string.error_message_wifi_aware));
        if(!getPackageManager().hasSystemFeature(PackageManager.FEATURE_WIFI_RTT))
            closeApp(getString(R.string.error_message_wifi_rtt));

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        checkPermissions();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == Constants.PERMISSIONS_REQUEST_CODE) {
            if (grantResults.length != 0) {
                for (int grantResult : grantResults) {
                    if (grantResult == PackageManager.PERMISSION_DENIED)
                        closeApp(getString(R.string.error_message_permission_denied));
                }
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mToolbar = (Toolbar) findViewById(R.id.toolbar_main);
        setSupportActionBar(mToolbar);

        mListView = (ListView) findViewById(R.id.listView);
        mScrollView = (ScrollView) findViewById(R.id.scrollView);
        mTextView = (TextView) findViewById(R.id.textView);

        mInitButton = (Button) findViewById(R.id.initButton);
        mStartButton = (Button) findViewById(R.id.startButton);
        mStopButton = (Button) findViewById(R.id.stopButton);
        mInitButton.setOnClickListener(this);
        mStartButton.setOnClickListener(this);
        mStopButton.setOnClickListener(this);

        mAdapter = new ArrayAdapter<Device>(this, android.R.layout.simple_list_item_1);
        mListView.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.initButton) {
            if (!mSharedPreferences.getString(Constants.PREFERENCES_NAME_DEVICE_ID,
                    Constants.PREFERENCES_DEFAULT_DEVICE_ID)
                    .equals(Constants.PREFERENCES_DEFAULT_DEVICE_ID)) {
                mInitButton.setEnabled(false);
                if (isFileWriteMode())
                    getPrintWriter();
                clearList();
                clearText();
                startService(mServiceIntent);
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(true);
            }
            else {
                Toast.makeText(this, getString(R.string.error_message_set_id),
                        Toast.LENGTH_SHORT).show();
            }
        }
        else if (v.getId() == R.id.startButton) {
            mStartButton.setEnabled(false);
            startService(mServiceIntent);
        }
        else if (v.getId() == R.id.stopButton)
            actionStopButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopService(mServiceIntent);
        unregisterReceiver(mWifiRttBroadcastReceiver);
        unregisterReceiver(mWifiAwareBroadcastReceiver);
        unregisterReceiver(mServiceBroadcastReceiver);
        closePrintWriter();
    }

    private void updateListView(ArrayList<Device> newDevices) {
        for (Device newDevice : newDevices) {
            Device oldDevice = hasDeviceInAdapter(newDevice);
            if (oldDevice != null)
                oldDevice.update(newDevice);
            else
                mAdapter.add(newDevice);
            if (isFileWriteMode())
                WriteToFile(newDevice);
        }

        removeDevices();
        mAdapter.notifyDataSetChanged();
    }

    private Device hasDeviceInAdapter(Device device) {
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (device.getId().equals(mAdapter.getItem(i).getId()))
                return mAdapter.getItem(i);
        }

        return null;
    }

    private void removeDevices() {
        ArrayList<Device> removeList = new ArrayList<Device>();

        long currTime = System.currentTimeMillis();
        for (int i = 0; i < mAdapter.getCount(); i++) {
            if (currTime - mAdapter.getItem(i).getTime() > Long.parseLong(mSharedPreferences
                    .getString(Constants.PREFERENCES_NAME_TIMEOUT,
                            Constants.PREFERENCES_DEFAULT_TIMEOUT)))
                removeList.add(mAdapter.getItem(i));
        }

        for (Device device: removeList)
            mAdapter.remove(device);
    }

    private void actionStopButton() {
        mInitButton.setEnabled(true);
        stopService(mServiceIntent);
        mStartButton.setEnabled(false);
        mStopButton.setEnabled(false);
    }

    private void closeApp(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        finish();
    }

    private void checkPermissions() {
        for (String perm : Constants.PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, perm)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, Constants.PERMISSIONS,
                        Constants.PERMISSIONS_REQUEST_CODE);
            }
        }
    }

    private String getCurrentTime() {
        return "[" + new SimpleDateFormat("MM/dd HH:mm:ss")
                .format(new Date(System.currentTimeMillis())) + "]";
    }

    private void addTextToTextView(String text) {
        mTextView.append(getCurrentTime() + " " + text + "\n");
        mScrollView.post(new Runnable() {
            @Override
            public void run() {
                mScrollView.fullScroll(View.FOCUS_DOWN);
            }
        });
        if (isFileWriteMode())
            WriteToFile(getCurrentTime() + " " + text);
    }

    private void clearList() {
        mAdapter.clear();
        mAdapter.notifyDataSetChanged();
    }

    private void clearText() {
        mTextView.setText("");
    }

    private boolean isFileWriteMode() {
        return mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_FILE,
                Constants.PREFERENCES_DEFAULT_FILE);
    }

    private boolean hasSpecificFileName() {
        return mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_FILENAME,
                Constants.PREFERENCES_DEFAULT_FILENAME);
    }

    private void WriteToFile(Device device) {
        mPrintWriter.write(device.print() + "\n");
        mPrintWriter.flush();
    }

    private void WriteToFile(String message) {
        mPrintWriter.write(message + "\n");
        mPrintWriter.flush();
    }

    private void getPrintWriter() {
        closePrintWriter();

        String filename = new SimpleDateFormat("MMddHHmmssSSS")
                .format(new Date(System.currentTimeMillis())) + Constants.FILE_WRITE_EXTENSION;

        if (hasSpecificFileName()) {
            filename = mSharedPreferences
                    .getString(Constants.PREFERENCES_NAME_FILENAME_TEXT,
                            Constants.PREFERENCES_DEFAULT_FILENAME_TEXT)
                    + Constants.FILE_WRITE_EXTENSION;
        }

        try {
            if (isExternalStorageWritable()) {
                File dir = new File(getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                        + Constants.FILE_WRITE_FOLDER);

                if (!dir.exists())
                    dir.mkdir();

                mPrintWriter = new PrintWriter(new File(dir, filename));
            }
            else {
                actionStopButton();
                Toast.makeText(this, getString(R.string.message_io_exception),
                        Toast.LENGTH_SHORT).show();
            }
        }
        catch (FileNotFoundException e) {
            actionStopButton();
            Toast.makeText(this, getString(R.string.message_io_exception),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void closePrintWriter() {
        if (mPrintWriter != null) {
            mPrintWriter.close();
            mPrintWriter = null;
        }
    }

    private boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state))
            return true;
        return false;
    }
}
