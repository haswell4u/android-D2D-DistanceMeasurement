package com.example.distancemeasurement.views;

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
import android.os.Handler;
import android.os.Message;
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

import com.example.distancemeasurement.Constants;
import com.example.distancemeasurement.Device;
import com.example.distancemeasurement.MeasurementService;
import com.example.distancemeasurement.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

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

    private Timer mTimer;
    private RemoveHandler mRemoveHandler;

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
                Toast.makeText(getApplicationContext(),
                        intent.getStringExtra(Constants.INTENT_CONTENTS_NAME_ERROR),
                        Toast.LENGTH_SHORT).show();
            }
            else if (type == Constants.INTENT_CONTENTS_TYPE_UPDATE_DEVICE) {
                @SuppressWarnings("unchecked")
                ArrayList<Device> list = (ArrayList<Device>) intent
                        .getSerializableExtra(Constants.INTENT_CONTENTS_NAME_DEVICE);

                synchronized (mAdapter) {
                    updateListView(list);
                }
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
                        closeApp(getString(R.string.error_permission_denied));
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
                    Constants.PREFERENCES_DEFAULT_STRING)
                    .equals(Constants.PREFERENCES_DEFAULT_STRING)) {
                mInitButton.setEnabled(false);
                if (isFileWriteMode())
                    getPrintWriter();
                clearList();
                clearText();
                startService(mServiceIntent);
                mTimer = new Timer();
                mRemoveHandler = new RemoveHandler();
                mStartButton.setEnabled(true);
                mStopButton.setEnabled(true);
            }
            else {
                Toast.makeText(this, getString(R.string.error_set_id),
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, SettingsActivity.class));
            }
        }
        else if (v.getId() == R.id.startButton) {
            mStartButton.setEnabled(false);
            startService(mServiceIntent);
            startRemoveTimer();
        }
        else if (v.getId() == R.id.stopButton)
            actionStopButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mTimer.cancel();
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

        mAdapter.notifyDataSetChanged();
    }

    private void startRemoveTimer() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mRemoveHandler.sendMessage(mRemoveHandler.obtainMessage());
            }
        }, Constants.CHECK_REMOVE_INTERVAL, Constants.CHECK_REMOVE_INTERVAL);
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
                            Constants.PREFERENCES_DEFAULT_STRING)))
                removeList.add(mAdapter.getItem(i));
        }

        for (Device device: removeList) {
            mAdapter.remove(device);
            addTextToTextView(createRemoveUserMessage(device.getId()));
        }

        mAdapter.notifyDataSetChanged();
        // Todo (Delete devices from the list in service)
    }

    private String createRemoveUserMessage(String id) {
        StringBuffer str = new StringBuffer(getString(R.string.message_device_remove));
        str.insert(Constants.OFFSET_MESSAGE_DEVICE_FOUND_REMOVED, id);
        return str.toString();
    }

    private void actionStopButton() {
        mInitButton.setEnabled(true);
        mTimer.cancel();
        stopService(mServiceIntent);
        mAdapter.notifyDataSetChanged();
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
                Constants.PREFERENCES_DEFAULT_BOOLEAN);
    }

    private boolean hasSpecificFileName() {
        return mSharedPreferences.getBoolean(Constants.PREFERENCES_NAME_FILENAME,
                Constants.PREFERENCES_DEFAULT_BOOLEAN);
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
                            Constants.PREFERENCES_DEFAULT_STRING)
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
                Toast.makeText(this, getString(R.string.message_pw_io_exception),
                        Toast.LENGTH_SHORT).show();
            }
        }
        catch (FileNotFoundException e) {
            actionStopButton();
            Toast.makeText(this, getString(R.string.message_pw_io_exception),
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

    private class RemoveHandler extends Handler {
        @Override
        public void handleMessage(@NonNull Message msg) {
            synchronized (mAdapter) {
                removeDevices();
            }
        }
    }
}
