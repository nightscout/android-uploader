package com.nightscout.android.debug;

import android.app.Application;

import com.nightscout.android.MainActivity;
import com.nightscout.android.exceptions.AcraFeedbackDialog;
import com.nightscout.android.exceptions.FeedbackDialog;
import com.nightscout.android.modules.NightscoutModule;
import com.nightscout.android.ui.AppContainer;

import org.acra.ACRA;

import java.util.TimeZone;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

@Module(
        addsTo = NightscoutModule.class,
        injects = {
                MainActivity.class,
                DebugAppContainer.class
        },
        overrides = true
)
public final class DebugNightscoutModule {
    @Provides
    @Singleton
    AppContainer provideAppContainer(DebugAppContainer debugAppContainer) {
        return debugAppContainer;
    }

    @Provides
    @Singleton
    FeedbackDialog providesReporter(Application app) {
        ACRA.init(app);
        ACRA.getErrorReporter().putCustomData("timezone", TimeZone.getDefault().getID());
        return new AcraFeedbackDialog();
    }
}
