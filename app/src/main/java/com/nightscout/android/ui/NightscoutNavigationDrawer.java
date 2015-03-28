package com.nightscout.android.ui;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.nightscout.android.BuildConfig;
import com.nightscout.android.CollectorService;
import com.nightscout.android.Nightscout;
import com.nightscout.android.R;
import com.nightscout.android.events.EventFragment;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.settings.SettingsActivity;

import javax.inject.Inject;

import it.neokree.materialnavigationdrawer.MaterialNavigationDrawer;
import it.neokree.materialnavigationdrawer.elements.MaterialAccount;
import it.neokree.materialnavigationdrawer.elements.MaterialSection;
import it.neokree.materialnavigationdrawer.elements.listeners.MaterialSectionListener;

public class NightscoutNavigationDrawer extends MaterialNavigationDrawer {

    @Inject
    FeedbackDialog feedbackDialog;
//    @Inject AppContainer appContainer;


    @Override
    public void init(Bundle bundle) {
        Nightscout app = Nightscout.get(this);
        app.inject(this);

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
                Log.d("XXX", "Sync requested");
                Intent syncIntent = new Intent(getApplicationContext(), CollectorService.class);
                syncIntent.setAction(CollectorService.ACTION_POLL);
                syncIntent.putExtra(CollectorService.NUM_PAGES, 2);
                syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.STD_SYNC);
                getApplicationContext().startService(syncIntent);
            }
        });
        addSection(sync);

        Intent gapsSyncIntent = new Intent(getApplicationContext(), CollectorService.class);
        gapsSyncIntent.setAction(CollectorService.ACTION_SYNC);
        gapsSyncIntent.putExtra(CollectorService.NUM_PAGES, 2);
        gapsSyncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.SYNC_TYPE);
        MaterialSection gapSync = newSection("Gap sync", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                Log.d("XXX", "Sync requested");
                Intent syncIntent = new Intent(getApplicationContext(), CollectorService.class);
                syncIntent.setAction(CollectorService.ACTION_POLL);
                syncIntent.putExtra(CollectorService.NUM_PAGES, 20);
                syncIntent.putExtra(CollectorService.SYNC_TYPE, CollectorService.GAP_SYNC);
                getApplicationContext().startService(syncIntent);
            }
        });
        addSection(gapSync);


        MaterialSection allLog = newSection("Event logs", EventFragment.newAllLogPanel());
        addSection(allLog);

//        MaterialSection uploaderLog = newSection("Uploader logs", EventFragment.newUploadLogPanel());
//        addSection(uploaderLog);
//
//        MaterialSection deviceLog = newSection("Device log", EventFragment.newDeviceLogPanel());
//        addSection(deviceLog);

        MaterialSection feedback = newSection("Report an issue", new MaterialSectionListener() {
            @Override
            public void onClick(MaterialSection materialSection) {
                feedbackDialog.show();
            }
        });
        addBottomSection(feedback);
        MaterialSection settings = newSection("Settings", android.R.drawable.ic_menu_preferences, new Intent(getApplicationContext(), SettingsActivity.class));
//        MaterialSection settings = newSection("Settings", new SettingsActivity.MainPreferenceFragment());
        addBottomSection(settings);

        allowArrowAnimation();
        disableLearningPattern();
        setBackPattern(MaterialNavigationDrawer.BACKPATTERN_BACK_TO_FIRST);
        enableToolbarElevation();
    }
}
