package com.nightscout.android.modules;

import android.app.Application;

import com.nightscout.android.Nightscout;
import com.nightscout.android.drivers.DriverModule;
import com.nightscout.android.exceptions.AcraFeedbackDialog;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.PreferencesModule;
import com.nightscout.android.ui.ActivityHierarchyServer;
import com.nightscout.android.ui.MonitorFragment;
import com.nightscout.android.ui.NightscoutNavigationDrawer;
import com.nightscout.android.ui.UiModule;
import com.orm.Database;

import org.acra.ACRA;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
    includes = {
        DriverModule.class,
        PreferencesModule.class,
        UiModule.class
    },
    injects = {
        Nightscout.class,
        MonitorFragment.class,
        NightscoutNavigationDrawer.class,
        Database.class,
        ActivityHierarchyServer.class
    },
    library = true
)
public class NightscoutModule {

    private Application app;

    public NightscoutModule(Application app) {
        this.app = app;
    }

    @Provides
    @Singleton
    Application providesApplication() {
        return app;
    }

    @Provides
    @Singleton
    FeedbackDialog providesReporter(Application app) {
        // TODO figure out why calls to ACRA crash when not in the main fragment?
        ACRA.init(app);
        ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());
        return new AcraFeedbackDialog();
    }

    @Provides
    @Singleton
    Database provideSugarDatabase(Application app) {
        return new Database(app.getApplicationContext());
    }
}
