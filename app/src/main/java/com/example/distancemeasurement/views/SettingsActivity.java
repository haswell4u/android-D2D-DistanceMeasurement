package com.example.distancemeasurement.views;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.EditTextPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.example.distancemeasurement.Constants;
import com.example.distancemeasurement.R;

public class SettingsActivity extends AppCompatActivity {

    Toolbar mToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.settings, new SettingsFragment())
                .commit();

        mToolbar = (Toolbar) findViewById(R.id.toolbar_settings);
        setSupportActionBar(mToolbar);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.settings_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.back:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);

            setDisable(PackageManager.FEATURE_WIFI_AWARE,
                    Constants.PREFERENCES_NAME_WIFI_AWARE,
                    getString(R.string.error_wifi_aware_support));
            setDisable(PackageManager.FEATURE_WIFI_RTT,
                    Constants.PREFERENCES_NAME_WIFI_RTT,
                    getString(R.string.error_wifi_rtt_support));
            setDisable(PackageManager.FEATURE_BLUETOOTH_LE,
                    Constants.PREFERENCES_NAME_BLE,
                    getString(R.string.error_ble_support));

            setDisableDependency(Constants.PREFERENCES_NAME_WIFI_AWARE,
                    Constants.PREFERENCES_NAME_WIFI_RTT);
            setDisableDependency(Constants.PREFERENCES_NAME_FILE,
                    Constants.PREFERENCES_NAME_FILENAME);

            setInputType(Constants.PREFERENCES_NAME_FILENAME_TEXT, InputType.TYPE_CLASS_TEXT);
            setInputType(Constants.PREFERENCES_NAME_DEVICE_ID, InputType.TYPE_CLASS_TEXT);
            setInputType(Constants.PREFERENCES_NAME_INTERVAL, InputType.TYPE_CLASS_NUMBER);
            setInputType(Constants.PREFERENCES_NAME_TIMEOUT, InputType.TYPE_CLASS_NUMBER);
            setInputType(Constants.PREFERENCES_NAME_REFRESH, InputType.TYPE_CLASS_NUMBER);

            setIntervalDependency(Constants.PREFERENCES_NAME_TIMEOUT,
                    Constants.PREFERENCES_NAME_REFRESH, Constants.PREFERENCES_NAME_INTERVAL);
        }

        private void setDisable(String feature, String preference, String message) {
            if (!getContext().getPackageManager()
                    .hasSystemFeature(feature)) {
                SwitchPreferenceCompat switchPreferenceCompat = getPreferenceManager()
                        .findPreference(preference);
                switchPreferenceCompat.setChecked(false);
                switchPreferenceCompat.setSummary(message);
                switchPreferenceCompat.setEnabled(false);
            }
        }

        private void setIntervalDependency(String iPreference,
                                           String dPreference1, String dPreference2) {
            final EditTextPreference iEditTextPreference = getPreferenceManager()
                    .findPreference(iPreference);
            final EditTextPreference dEditTextPreference1 = getPreferenceManager()
                    .findPreference(dPreference1);
            final EditTextPreference dEditTextPreference2 = getPreferenceManager()
                    .findPreference(dPreference2);
            iEditTextPreference
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (Integer.parseInt(dEditTextPreference1.getText()) >
                                    (int) (Integer.parseInt((String) newValue)
                                            * Constants.INTERVAL_DEPENDENCY_RATIO))
                                dEditTextPreference1
                                        .setText(Integer.toString((int) ((Integer
                                                .parseInt((String) newValue))
                                                * Constants.INTERVAL_DEPENDENCY_RATIO)));
                            if (Integer.parseInt(dEditTextPreference2.getText()) >
                                    (int) (Integer.parseInt((String) newValue)
                                            * Constants.INTERVAL_DEPENDENCY_RATIO))
                                dEditTextPreference2
                                        .setText(Integer.toString((int) ((Integer
                                                .parseInt((String) newValue))
                                                * Constants.INTERVAL_DEPENDENCY_RATIO)));
                            return true;
                        }
                    });
            dEditTextPreference1
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (Integer.parseInt((String) newValue) >
                                    (int) (Integer.parseInt(iEditTextPreference.getText())
                                            * Constants.INTERVAL_DEPENDENCY_RATIO))
                                iEditTextPreference
                                        .setText(Integer.toString((int) ((Integer
                                                .parseInt((String) newValue))
                                                / Constants.INTERVAL_DEPENDENCY_RATIO)));
                            return true;
                        }
                    });
            dEditTextPreference2
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (Integer.parseInt((String) newValue) >
                                    (int) (Integer.parseInt(iEditTextPreference.getText())
                                            * Constants.INTERVAL_DEPENDENCY_RATIO))
                                iEditTextPreference
                                        .setText(Integer.toString((int) ((Integer
                                                .parseInt((String) newValue))
                                                / Constants.INTERVAL_DEPENDENCY_RATIO)));
                            return true;
                        }
                    });
        }

        private void setDisableDependency(String iPreference, String dPreference) {
            SwitchPreferenceCompat iSwitchPreferenceCompat = getPreferenceManager()
                    .findPreference(iPreference);
            final SwitchPreferenceCompat dSwitchPreferenceCompat = getPreferenceManager()
                    .findPreference(dPreference);

            iSwitchPreferenceCompat
                    .setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            if (!(Boolean) newValue)
                                dSwitchPreferenceCompat.setChecked(false);
                            return true;
                        }
                    });
        }

        private void setInputType(String preference, final int inputType) {
            EditTextPreference editTextPreference = getPreferenceManager()
                    .findPreference(preference);
            editTextPreference
                    .setOnBindEditTextListener(new EditTextPreference.OnBindEditTextListener() {
                @Override
                public void onBindEditText(@NonNull EditText editText) {
                    editText.setInputType(inputType);
                }
            });
        }
    }
}
