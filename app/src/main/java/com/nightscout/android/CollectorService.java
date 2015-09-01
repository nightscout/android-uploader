package com.nightscout.android;

import android.app.IntentService;
import android.content.Intent;

import com.nightscout.android.db.DbUtils;
import com.nightscout.android.events.reporters.AndroidEventReporter;
import com.nightscout.android.preferences.AndroidPreferences;
import com.nightscout.core.Timestamped;
import com.nightscout.core.dexcom.CRCFailError;
import com.nightscout.core.drivers.AbstractDevice;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.DeviceType;
import com.nightscout.core.drivers.DexcomG4;
import com.nightscout.core.events.reporters.EventReporter;
import com.nightscout.core.events.EventSeverity;
import com.nightscout.core.events.EventType;
import com.nightscout.core.model.v2.Download;
import com.nightscout.core.model.v2.DownloadStatus;
import com.nightscout.core.model.v2.SensorGlucoseValue;
import com.nightscout.core.utils.ListUtils;

import net.tribe7.common.base.Optional;
import net.tribe7.common.base.Supplier;

import org.joda.time.Duration;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;

public class CollectorService extends IntentService {
    private static final Logger log = LoggerFactory.getLogger(CollectorService.class);

    // Max time to wait between grabbing info from the devices. Set to 5 minutes, because that is how often the dex reads from sensor.
    private static final int MAX_POLL_WAIT_SEC = 600;

    public static final String ACTION_SYNC = "com.nightscout.android.dexcom.action.SYNC";
    public static final String ACTION_POLL = "com.nightscout.android.dexcom.action.POLL";

    public static final String NUM_PAGES = "NUM_PAGES";
    public static final String STD_SYNC = "STD_SYNC";
    public static final String SYNC_TYPE = "SYNC_TYPE";

    private EventReporter reporter;
    private AndroidPreferences preferences;
    protected AbstractDevice device = null;
    @Inject
    @Named("dexcomDriverSupplier")
    Supplier<DeviceTransport> dexcomDriverSupplier;

    public CollectorService(String name) {
        super(name);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        reporter = AndroidEventReporter.getReporter(getApplicationContext());
        preferences = new AndroidPreferences(getApplicationContext());

        dexcomDriverSupplier = new DexcomDriverSupplier(getApplicationContext(), preferences);
        initializeDevice();
    }

    private void initializeDevice() {
        DeviceType deviceType = preferences.getDeviceType();
        if (deviceType == DeviceType.DEXCOM_G4 || deviceType == DeviceType.DEXCOM_G4_SHARE2) {
            device = new DexcomG4(dexcomDriverSupplier, reporter);
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (device == null) {
            log.error("Device not initialized. Returning.");
            return;
        }

        Optional<Timestamped> newestElementInDb =
            Optional.<Timestamped>fromNullable(
                DbUtils.getNewestElementInDb(preferences.getDeviceType()).orNull());
        Download download = null;
        try {
            download = device.downloadAllAfter(newestElementInDb);
            Collector.completeWakefulIntent(intent);
            if (download.status != DownloadStatus.SUCCESS) {
                log.error("Bad download, will try again");

                return;
            }
            DbUtils.updateAllRecords(download);
        } catch (NegativeArraySizeException | CRCFailError | IndexOutOfBoundsException e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                            getApplicationContext().getString(R.string.event_fail_log));
        } catch (Exception e) {
            reporter.report(EventType.DEVICE, EventSeverity.ERROR,
                            getApplicationContext().getString(R.string.unknown_fail_log));
        }
        if (download != null) {
            Duration durationToNextPoll = Seconds.seconds(MAX_POLL_WAIT_SEC).toStandardDuration();
            Optional<SensorGlucoseValue> lastReadValue = ListUtils.lastOrEmpty(download.g4_data.sensor_glucose_values);
            if (lastReadValue.isPresent()) {
                long readTimeDifferenceSec =
                    (download.g4_data.receiver_system_time_sec - lastReadValue
                        .get().timestamp.system_time_sec) % MAX_POLL_WAIT_SEC;
                durationToNextPoll.minus(readTimeDifferenceSec);
            }
            durationToNextPoll.getMillis();
        }
        Collector.completeWakefulIntent(intent);
    }
}
