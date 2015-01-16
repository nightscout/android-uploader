package com.nightscout.core.drivers.Medtronic;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.Medtronic.records.BasalProfileStart;
import com.nightscout.core.drivers.Medtronic.records.BatteryActivity;
import com.nightscout.core.drivers.Medtronic.records.Bolus;
import com.nightscout.core.drivers.Medtronic.records.BolusWizard;
import com.nightscout.core.drivers.Medtronic.records.BolusWizardChange;
import com.nightscout.core.drivers.Medtronic.records.CalBgForPh;
import com.nightscout.core.drivers.Medtronic.records.ChangeBasalProfile;
import com.nightscout.core.drivers.Medtronic.records.ChangeRemoteId;
import com.nightscout.core.drivers.Medtronic.records.ChangeTime;
import com.nightscout.core.drivers.Medtronic.records.ChangeTimeDisplay;
import com.nightscout.core.drivers.Medtronic.records.ChangeUtility;
import com.nightscout.core.drivers.Medtronic.records.ClearAlarm;
import com.nightscout.core.drivers.Medtronic.records.EndResultsTotals;
import com.nightscout.core.drivers.Medtronic.records.LowBattery;
import com.nightscout.core.drivers.Medtronic.records.LowReservoir;
import com.nightscout.core.drivers.Medtronic.records.NewTimeSet;
import com.nightscout.core.drivers.Medtronic.records.NoDeliveryAlarm;
import com.nightscout.core.drivers.Medtronic.records.Old6c;
import com.nightscout.core.drivers.Medtronic.records.Prime;
import com.nightscout.core.drivers.Medtronic.records.PumpResumed;
import com.nightscout.core.drivers.Medtronic.records.PumpSuspended;
import com.nightscout.core.drivers.Medtronic.records.Record;
import com.nightscout.core.drivers.Medtronic.records.ResultTotals;
import com.nightscout.core.drivers.Medtronic.records.Rewound;
import com.nightscout.core.drivers.Medtronic.records.Sara6E;
import com.nightscout.core.drivers.Medtronic.records.SelectBasalProfile;
import com.nightscout.core.drivers.Medtronic.records.TempBasalDuration;
import com.nightscout.core.drivers.Medtronic.records.TempBasalRate;
import com.nightscout.core.drivers.Medtronic.records.ToggleRemote;
import com.nightscout.core.drivers.Medtronic.records.UnabsorbedInsulin;
import com.nightscout.core.utils.CRC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkNotNull;


public class Page {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

    private byte[] crc;
    private byte[] data;
    protected Map<Byte, Class> recordMap;
    protected PumpModel model;

    public Page(byte[] rawPage, PumpModel model) {
        if (rawPage.length != 1024) {
            throw new IllegalArgumentException("Unexpected page size. Expected: 1024 Was: " + rawPage.length);
        }
        checkNotNull(model);
        this.model = model;
        log.info("Parsing page");
        this.data = Arrays.copyOfRange(rawPage, 0, 1022);
        this.crc = Arrays.copyOfRange(rawPage, 1022, 1024);
        byte[] expectedCrc = CRC.calculate16CCITT(this.data);
        log.info("Data length: {}", data.length);
        if (!Arrays.equals(crc, expectedCrc)) {
            log.warn("CRC does not match expected value. Expected: {} Was: {}", Utils.bytesToHex(expectedCrc), Utils.bytesToHex(crc));
        } else {
            log.info("CRC checks out");
        }

        recordMap = new HashMap<>();
        recordMap.put((byte) 0x01, Bolus.class);
        recordMap.put((byte) 0x03, Prime.class);
        recordMap.put((byte) 0x06, NoDeliveryAlarm.class);
        recordMap.put((byte) 0x07, EndResultsTotals.class);
        recordMap.put((byte) 0x08, ChangeBasalProfile.class);
        recordMap.put((byte) 0x09, ChangeBasalProfile.class);
        recordMap.put((byte) 0x0A, CalBgForPh.class);
        recordMap.put((byte) 0x0c, ClearAlarm.class);
        recordMap.put((byte) 0x14, SelectBasalProfile.class);
        recordMap.put((byte) 0x16, TempBasalDuration.class);
        recordMap.put((byte) 0x17, ChangeTime.class);
        recordMap.put((byte) 0x18, NewTimeSet.class);
        recordMap.put((byte) 0x19, LowBattery.class);
        recordMap.put((byte) 0x1A, BatteryActivity.class);
        recordMap.put((byte) 0x1E, PumpSuspended.class);
        recordMap.put((byte) 0x1F, PumpResumed.class);
        recordMap.put((byte) 0x21, Rewound.class);
        recordMap.put((byte) 0x26, ToggleRemote.class);
        recordMap.put((byte) 0x27, ChangeRemoteId.class);
        recordMap.put((byte) 0x33, TempBasalRate.class);
        recordMap.put((byte) 0x34, LowReservoir.class);
        recordMap.put((byte) 0x5a, BolusWizardChange.class);
        recordMap.put((byte) 0x5b, BolusWizard.class);
        recordMap.put((byte) 0x5c, UnabsorbedInsulin.class);
        recordMap.put((byte) 0x6c, Old6c.class);
        recordMap.put((byte) 0x6d, ResultTotals.class);
        recordMap.put((byte) 0x6e, Sara6E.class);
        recordMap.put((byte) 0x63, ChangeUtility.class);
        recordMap.put((byte) 0x64, ChangeTimeDisplay.class);
        recordMap.put((byte) 0x7b, BasalProfileStart.class);

        List<Record> recordList = new ArrayList<>();
        Record record = parseRecord(data, 0, recordMap.get(data[0]));
        recordList.add(record);
        log.info("Record size: {}", record.getSize());
        byte[] remainingData = Arrays.copyOfRange(data, record.getSize(), 1022);
        while (remainingData[0] != 0x00) {
            log.info("Remaining data: {}", Utils.bytesToHex(remainingData));
            record = parseRecord(remainingData, 0, recordMap.get(remainingData[0]));
            log.info("Record size: {}", record.getSize());
            recordList.add(record);
            remainingData = Arrays.copyOfRange(remainingData, record.getSize(), 1022);
        }
        log.info("Number of records: {}", recordList.size());
        int index = 1;
        for (Record r : recordList) {
//            log.info("Record #{}", index);
            r.logRecord();
            index += 1;
        }

    }


    protected <T extends Record> T parseRecord(byte[] data, int offsetStart, Class<T> clazz) {
        Constructor<T> ctor;
        T record = null;
        try {
            log.info("Record type: {} {}", Utils.bytesToHex(new byte[]{data[0]}), clazz.getSimpleName());
            ctor = clazz.getConstructor(byte[].class, PumpModel.class);
            record = ctor.newInstance(data, model);
        } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return record;
    }
}