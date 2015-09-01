package com.nightscout.android.ui;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.BuildConfig;
import com.nightscout.android.CollectorService;
import com.nightscout.android.Nightscout;
import com.nightscout.android.ProcessorService;
import com.nightscout.android.R;
import com.nightscout.android.events.EventFragment;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.settings.SettingsActivity;

import java.net.URI;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class NightscoutNavigationDrawer extends MaterialNavigationDrawer {

    private final String TAG = NightscoutNavigationDrawer.class.getSimpleName();

    @Inject
    FeedbackDialog feedbackDialog;
    Tracker mTracker;
    private AndroidPreferences preferences;

    @Override
    public void init(Bundle bundle) {
        Nightscout app = Nightscout.get(this);
        app.inject(this);

        mTracker = ((Nightscout) getApplicationContext()).getTracker();
        preferences = new AndroidPreferences(this);


        MaterialAccount account = new MaterialAccount(this.getResources(), "Nightscout", BuildConfig.VERSION_CODENAME, R.drawable.ic_launcher, R.drawable.nscafe);
        this.addAccount(account);

        MaterialSection section = newSection("Home", new MonitorFragment());
        addSection(section);

        Intent syncIntent = new Intent(getApplicationContext(), CollectorService.class);
        syncIntent.setAction(CollectorService.ACTION_SYNC);
        syncIntent.putExtra(CollectorService.NUM_PAGES, 2);
        syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.SYNC_TYPE);
        MaterialSection sync = newSection("Start syncing", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                Intent syncIntent = new Intent(getApplicationContext(), CollectorService.class);
                syncIntent.setAction(CollectorService.ACTION_POLL);
                syncIntent.putExtra(CollectorService.NUM_PAGES, 1);
                syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.STD_SYNC);
                getApplicationContext().startService(syncIntent);
            }
        });
        addSection(sync);

        MaterialSection allLog = newSection("Event logs", EventFragment.newAllLogPanel());
        addSection(allLog);

        MaterialSection feedback = newSection("Report an issue", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                feedbackDialog.show();
            }
        });
        addSection(feedback);
        MaterialSection settings = newSection("Settings", android.R.drawable.ic_menu_preferences, new Intent(getApplicationContext(), SettingsActivity.class));
//        MaterialSection settings = newSection("Settings", new SettingsActivity.MainPreferenceFragment());
//        MaterialSection settings = newSection("Settings", android.R.drawable.ic_menu_preferences, new SettingsActivity.MainPreferenceFragment());
        addBottomSection(settings);

        Log.d(TAG, "Attempting to start service");
        Intent uploadIntent = new Intent(getBaseContext(), ProcessorService.class);
        getApplicationContext().startService(uploadIntent);
        syncIntent = new Intent(getApplicationContext(), CollectorService.class);
        syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.STD_SYNC);
        getApplicationContext().startService(syncIntent);

        Log.d(TAG, "Service should be started");

        MaterialSection close = newSection("Close", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                Intent collectorIntent = new Intent(getApplicationContext(), CollectorService.class);
                getApplicationContext().stopService(collectorIntent);
                Intent processorIntent = new Intent(getApplicationContext(), ProcessorService.class);
                getApplicationContext().stopService(processorIntent);
                finish();
            }
        });
        addSection(close);

        allowArrowAnimation();
        disableLearningPattern();
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        enableToolbarElevation();

    }

    // FIXME - Hack to enable full gap sync
    // Sets the last upload time for each counter to 0 so that everything is uploaded
    private void setupGapSync() {
        preferences.setLastEgvSysTime(0);
        preferences.setLastMeterSysTime(0);
        if (preferences.isMqttEnabled()) {
            preferences.setLastEgvMqttUpload(0);
            preferences.setLastMeterMqttUpload(0);
            preferences.setLastSensorMqttUpload(0);
            preferences.setLastCalMqttUpload(0);
            preferences.setLastInsMqttUpload(0);
        }
        // Note: A service on a different port could potentially write to a different
        // database but we're only going to treat each each unique host as a different endpoint.
        if (preferences.isRestApiEnabled()) {
            for (String endPoint : preferences.getRestApiBaseUris()) {
                URI uri = URI.create(endPoint);
                String id = uri.getHost();
                preferences.setLastEgvBaseUpload(0, id);
                preferences.setLastMeterBaseUpload(0, id);
                preferences.setLastSensorBaseUpload(0, id);
                preferences.setLastCalBaseUpload(0, id);
                preferences.setLastInsBaseUpload(0, id);
            }
        }
        if (preferences.isMongoUploadEnabled()) {
            preferences.setLastEgvBaseUpload(0, "MongoDB");
            preferences.setLastMeterBaseUpload(0, "MongoDB");
            preferences.setLastSensorBaseUpload(0, "MongoDB");
            preferences.setLastCalBaseUpload(0, "MongoDB");
            preferences.setLastInsBaseUpload(0, "MongoDB");
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
//        super.onBackPressed();
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d(TAG, "Configuration changed");
        super.onConfigurationChanged(newConfig);
    }
}
