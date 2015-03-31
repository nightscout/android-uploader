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
import com.nightscout.android.events.AndroidEventReporter;
import com.nightscout.android.events.EventFragment;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.android.settings.SettingsActivity;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class NightscoutNavigationDrawer extends MaterialNavigationDrawer {

    @Inject
    FeedbackDialog feedbackDialog;
    Tracker mTracker;
    private AndroidPreferences preferences;
    private AndroidEventReporter reporter;
//    @Inject AppContainer appContainer;


    @Override
    public void init(Bundle bundle) {
        Nightscout app = Nightscout.get(this);
        app.inject(this);

        mTracker = ((Nightscout) getApplicationContext()).getTracker();
        preferences = new AndroidPreferences(this);
        reporter = AndroidEventReporter.getReporter(this);
        reporter.report(EventType.APPLICATION, EventSeverity.INFO,
                getApplicationContext().getString(R.string.app_started));


        MaterialAccount account = new MaterialAccount(this.getResources(), "Nightscout", BuildConfig.VERSION_CODENAME, R.drawable.ic_launcher, R.drawable.nscafe);
        this.addAccount(account);


//        setDrawerHeaderImage(R.drawable.ic_launcher);
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
                syncIntent.putExtra("requested", true);
                syncIntent.setAction(CollectorService.ACTION_POLL);
                syncIntent.putExtra(CollectorService.NUM_PAGES, 2);
                syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.STD_SYNC);
                getApplicationContext().startService(syncIntent);
            }
        });
        addSection(sync);

        MaterialSection gapSync = newSection("Gap sync", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                Log.d("XXX", "Sync requested");
                Intent syncIntent = new Intent(getApplicationContext(), CollectorService.class);
                syncIntent.putExtra("requested", true);
                syncIntent.setAction(CollectorService.ACTION_POLL);
                syncIntent.putExtra(CollectorService.NUM_PAGES, 20);
                syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.GAP_SYNC);
                getApplicationContext().startService(syncIntent);


            }
        });
        addSection(gapSync);


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

        allowArrowAnimation();
        disableLearningPattern();
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        enableToolbarElevation();
        Log.d("XXX", "Attempting to start service");
        Intent uploadIntent = new Intent(getBaseContext(), ProcessorService.class);
        getApplicationContext().startService(uploadIntent);
        syncIntent = new Intent(getApplicationContext(), CollectorService.class);
        syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.NON_SYNC);
        getApplicationContext().startService(syncIntent);

        Log.d("XXX", "Service should be started");

    }


    @Override
    protected void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStop(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("XXX", "Configuration changed");
        super.onConfigurationChanged(newConfig);
    }
}
