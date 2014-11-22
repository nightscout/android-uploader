package com.nightscout.robolectric;

import org.junit.runners.model.InitializationError;
import org.robolectric.AndroidManifest;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.res.Fs;

// Shamelessly stolen from:
// http://blog.blundell-apps.com/android-gradle-app-with-robolectric-junit-tests/
public class RobolectricGradleRunner extends RobolectricTestRunner {
    private static final int MAX_SDK_SUPPORTED_BY_ROBOLECTRIC = 18;

    public RobolectricGradleRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected AndroidManifest getAppManifest(Config config) {
        String manifestProperty = "src/main/AndroidManifest.xml";
        String resProperty = "src/main/res";
        return new AndroidManifest(Fs.fileFromPath(manifestProperty), Fs.fileFromPath(resProperty)) {
            @Override
            public int getTargetSdkVersion() {
                return MAX_SDK_SUPPORTED_BY_ROBOLECTRIC;
            }
        };
    }
}