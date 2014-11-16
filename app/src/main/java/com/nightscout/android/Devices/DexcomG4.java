package com.nightscout.android.devices;

import android.content.Context;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.util.Log;

import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.nightscout.android.MainActivity;
import com.nightscout.android.Nightscout;
import com.nightscout.android.TimeConstants;
import com.nightscout.android.dexcom.CRCFailRuntimeException;
import com.nightscout.android.dexcom.DownloadStatus;
import com.nightscout.android.dexcom.G4Constants;
import com.nightscout.android.dexcom.G4Download;
import com.nightscout.android.dexcom.GlucoseUnit;
import com.nightscout.android.dexcom.ReadData;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.SensorRecord;

import java.io.IOException;
import java.util.Date;
import java.util.List;

public class DexcomG4 extends AbstractPollDevice {
    private static final String TAG = DexcomG4.class.getSimpleName();

    public DexcomG4(int deviceNum, Context context) {
        super(deviceNum, context, G4Constants.DRIVER);
        this.remote=false;
    }

    @Override
    public void start(){
        super.start();
    }

    @Override
    protected G4Download doDownload() {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NSDownload");

        boolean rootEnabled= PreferenceManager.getDefaultSharedPreferences(context).getBoolean("root_support_enabled",false);
        Tracker tracker = ((Nightscout) context).getTracker();
        G4Download download=new G4Download();
        if (rootEnabled){
            ((USBSerialTransport) transport).setUsePowerManagement(true);
        }
        wl.acquire();
        if (acquireTransport()) {
            try {
                // FIXME - need to figure out how to manage gap syncs.
                int numOfPages=2;
                ReadData readData = new ReadData(transport);
                // TODO: need to check if numOfPages if valid on ReadData side
                List<EGVRecord> recentRecords = readData.getRecentEGVsPages(numOfPages);
                List<MeterRecord> meterRecords = readData.getRecentMeterRecords();
                // TODO: need to check if numOfPages if valid on ReadData side
                List<SensorRecord> sensorRecords = readData.getRecentSensorRecords(numOfPages);
                List<CalRecord> calRecords = readData.getRecentCalRecords();

                long timeSinceLastRecord = readData.getTimeSinceEGVRecord(recentRecords.get(recentRecords.size() - 1));
                // TODO: determine if the logic here is correct. I suspect it assumes the last record was less than 5
                // minutes ago. If a reading is skipped and the device is plugged in then nextUploadTime will be
                // set to a negative number. This situation will eventually correct itself.
                // Also assumes that at least one record exists.
                long nextUploadTime = TimeConstants.FIVE_MINUTES_MS - (timeSinceLastRecord * TimeConstants.SEC_TO_MS)+G4Constants.TIME_SYNC_OFFSET;
                long displayTime = readData.readDisplayTime().getTime();
//                int batLevel = readData.readBatteryLevel();
                int batLevel = 100;

                G4Download.G4DownloadBuilder downloadBuilder= new G4Download.G4DownloadBuilder();
                downloadBuilder.setDownloadTimestamp(new Date().getTime())
                        .setEGVRecords(recentRecords)
                        .setDisplayTime(displayTime)
                        .setSensorRecords(sensorRecords)
                        .setCalRecords(calRecords)
                        .setMeterRecords(meterRecords)
                        .setDownloadStatus(DownloadStatus.SUCCESS)
                        .setUnits(GlucoseUnit.MGDL)
                        .setReceiverBattery(batLevel)
                        .setNextUploadTime(nextUploadTime)
                        .setDriver(driver)
                        .setUploaderBattery(MainActivity.batLevel);
                download=downloadBuilder.build();
                // FIXME: does not currently handle gap sync functionality
            } catch (ArrayIndexOutOfBoundsException e) {
                Log.wtf("Unable to read from the dexcom, maybe it will work next time", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Array Index out of bounds")
                        .setFatal(false)
                        .build());
            } catch (NegativeArraySizeException e) {
                Log.wtf("Negative array exception from receiver", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Negative Array size")
                        .setFatal(false)
                        .build());
            } catch (IndexOutOfBoundsException e) {
                Log.wtf("IndexOutOfBounds exception from receiver", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("IndexOutOfBoundsException")
                        .setFatal(false)
                        .build());
            } catch (CRCFailRuntimeException e){
                // FIXME: may consider localizing this catch at a lower level (like ReadData) so that
                // if the CRC check fails on one type of record we can capture the values if it
                // doesn't fail on other types of records. This means we'd need to broadcast back
                // partial results to the UI. Adding it to a lower level could make the ReadData class
                // more difficult to maintain - needs discussion.
                Log.wtf("CRC failed", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("CRC Failed")
                        .setFatal(false)
                        .build());
            } catch (Exception e) {
                Log.wtf("Unhandled exception caught", e);
                tracker.send(new HitBuilders.ExceptionBuilder().setDescription("Catch all exception in handleActionSync")
                        .setFatal(false)
                        .build());
            } finally {
                // Close serial
                try {
                    transport.close();
                } catch (IOException e) {
                    tracker.send(new HitBuilders.ExceptionBuilder()
                                    .setDescription("Unable to close serial connection")
                                    .setFatal(false)
                                    .build()
                    );
                    Log.e(TAG, "Unable to close", e);
                }
            }
        }
        wl.release();
        return download;
    }

    private boolean acquireTransport() {
        // TODO: add logic to mimic serial device by reading from file here with options.
        transport = new USBSerialTransport(context);
        try {
            transport.open();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Unable to open USB. ", e);
            Tracker tracker;
            tracker = ((Nightscout) context).getTracker();
            tracker.send(new HitBuilders.ExceptionBuilder()
                            .setDescription("Unable to open serial connection")
                            .setFatal(false)
                            .build()
            );
        }
        return false;
    }
}
