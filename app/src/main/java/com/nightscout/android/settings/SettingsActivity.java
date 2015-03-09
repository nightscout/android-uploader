package com.nightscout.android.settings;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.provider.Settings;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

import com.google.common.base.Optional;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.nightscout.android.BuildConfig;
import com.nightscout.android.R;
import com.nightscout.android.barcode.AndroidBarcode;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferenceKeys;
import com.nightscout.android.preferences.PreferencesValidator;
import com.nightscout.core.barcode.NSBarcodeConfig;
import com.nightscout.core.preferences.NightscoutPreferences;
import com.nightscout.core.utils.RestUriUtils;

import java.util.List;

public class SettingsActivity extends FragmentActivity {
    private MainPreferenceFragment mainPreferenceFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setupActionBar();

        refreshFragments();
    }

    private void refreshFragments() {
        mainPreferenceFragment = new MainPreferenceFragment();
        getFragmentManager().beginTransaction().replace(android.R.id.content,
                mainPreferenceFragment).commit();
    }

    private void setupActionBar() {
        if (getActionBar() != null) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.home) {
            NavUtils.navigateUpFromSameTask(this);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    protected static void showValidationError(final Context context, final String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.invalid_input_title);
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, null);
        builder.show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult scanResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        NightscoutPreferences prefs = new AndroidPreferences(this);
        if (scanResult != null && scanResult.getContents() != null) {
            NSBarcodeConfig barcode = new NSBarcodeConfig(scanResult.getContents());
            if (barcode.hasMongoConfig()) {
                prefs.setMongoUploadEnabled(true);
                if (barcode.getMongoUri().isPresent()) {
                    prefs.setMongoClientUri(barcode.getMongoUri().get());
                    prefs.setMongoCollection(barcode.getMongoCollection().orNull());
                    prefs.setMongoDeviceStatusCollection(
                            barcode.getMongoDeviceStatusCollection().orNull());
                }
            } else {
                prefs.setMongoUploadEnabled(false);
            }
            if (barcode.hasApiConfig()) {
                prefs.setRestApiEnabled(true);
                prefs.setRestApiBaseUris(barcode.getApiUris());
            } else {
                prefs.setRestApiEnabled(false);
            }
            refreshFragments();
        }
    }

    public static class MainPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            setupBarcodeScanner();
            setupValidation();
            setupVersionNumbers();
        }

        private void setupVersionNumbers() {
            findPreference("about_version_number").setSummary(BuildConfig.VERSION_CODENAME);
            findPreference("about_version_number").setSummary(BuildConfig.VERSION_NAME);
            findPreference("about_build_hash").setSummary(BuildConfig.GIT_SHA);
            findPreference("about_device_id").setSummary(Settings.Secure.getString(getActivity().getContentResolver(),
                    Settings.Secure.ANDROID_ID));
        }

        private void setupBarcodeScanner() {
            findPreference("auto_configure").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AndroidBarcode(getActivity()).scan();
                    return true;
                }
            });
        }

        private void setupValidation() {
            findPreference(PreferenceKeys.API_URIS).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String combinedUris = (String) newValue;
                            List<String> splitUris = RestUriUtils.splitIntoMultipleUris(combinedUris);
                            for (String uri : splitUris) {
                                Optional<String> error = PreferencesValidator.validateRestApiUriSyntax(
                                        getActivity(), uri);
                                if (error.isPresent()) {
                                    showValidationError(getActivity(), error.get());
                                    return false;
                                }
                            }
                            return true;
                        }
                    });
            findPreference(PreferenceKeys.MONGO_URI).setOnPreferenceChangeListener(
                    new Preference.OnPreferenceChangeListener() {
                        @Override
                        public boolean onPreferenceChange(Preference preference, Object newValue) {
                            String mongoUri = (String) newValue;
                            Optional<String> error = PreferencesValidator.validateMongoUriSyntax(
                                    getActivity(), mongoUri);
                            if (error.isPresent()) {
                                showValidationError(getActivity(), error.get());
                                return false;
                            }
                            return true;
                        }
                    });
        }
    }
}
