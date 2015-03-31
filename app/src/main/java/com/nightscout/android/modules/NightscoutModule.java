package com.nightscout.android.modules;

import android.app.Application;

import com.nightscout.android.Nightscout;
import com.nightscout.android.exceptions.AcraFeedbackDialog;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.preferences.PreferencesModule;
import com.nightscout.android.ui.MonitorFragment;
import com.nightscout.android.ui.NightscoutNavigationDrawer;
import com.nightscout.android.ui.UiModule;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        includes = {
                PreferencesModule.class,
                UiModule.class
        },
        injects = {
                Nightscout.class,
                MonitorFragment.class,
                NightscoutNavigationDrawer.class
        }
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
//        ACRA.init(app);
//        ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());
        return new AcraFeedbackDialog();
    }
}
