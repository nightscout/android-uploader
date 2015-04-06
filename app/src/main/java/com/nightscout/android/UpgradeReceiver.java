package com.nightscout.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.preferences.PreferencesValidator;
import com.nightscout.android.ui.NightscoutNavigationDrawer;

import net.tribe7.common.base.Joiner;
import net.tribe7.common.base.Splitter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class UpgradeReceiver extends BroadcastReceiver {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private AndroidPreferences preferences;
    private Context context;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getDataString().contains("com.nightscout.android")) {
            this.context = context;
            Intent mainActivity = new Intent(context, NightscoutNavigationDrawer.class);
            mainActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            preferences = new AndroidPreferences(context);
            performUpdates();
            context.startActivity(mainActivity);
        }
    }

    private void performUpdates() {
        migrateToNewStyleRestUris();
        ensureSavedUrisAreValid();
        migrateToRawEnable();
    }

    private void migrateToRawEnable() {
        if (preferences.isCalibrationUploadEnabled() || preferences.isSensorUploadEnabled()) {
            log.debug("Enabling raw upload");
            preferences.setRawEnabled(true);
        }
        preferences.deleteKey(context.getString(R.string.cloud_cal_data));
        preferences.deleteKey(context.getString(R.string.cloud_sensor_data));
    }

    private void migrateToNewStyleRestUris() {
        log.debug("Looking for legacy Rest style URIs to update");
        List<String> newUris = new ArrayList<>();
        for (String uriString : preferences.getRestApiBaseUris()) {
            if (uriString.contains("@http")) {
                List<String> splitUri = Splitter.on('@').splitToList(uriString);
                Uri oldUri = Uri.parse(splitUri.get(1));
                String newAuthority = Joiner.on('@').join(splitUri.get(0), oldUri.getEncodedAuthority());
                Uri newUri = oldUri.buildUpon().encodedAuthority(newAuthority).build();
                newUris.add(newUri.toString());
            } else {
                newUris.add(uriString);
            }
        }
        preferences.setRestApiBaseUris(newUris);
    }

    private void ensureSavedUrisAreValid() {
        if (PreferencesValidator.validateMongoUriSyntax(context,
                preferences.getMongoClientUri()).isPresent()) {
            preferences.setMongoClientUri(null);
        }
        List<String> filteredRestUris = new ArrayList<>();
        for (String uri : preferences.getRestApiBaseUris()) {
            if (!PreferencesValidator.validateRestApiUriSyntax(context, uri).isPresent()) {
                filteredRestUris.add(uri);
            }
        }
        preferences.setRestApiBaseUris(filteredRestUris);
    }
}
