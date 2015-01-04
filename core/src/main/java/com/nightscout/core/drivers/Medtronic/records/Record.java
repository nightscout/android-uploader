package com.nightscout.core.drivers.Medtronic.records;

import java.util.Map;

abstract public class Record {
    private byte opString;
    private Map<Byte, Class> recordMap;


    Record(byte[] data) {
        opString = data[0];
        recordMap.put((byte) 0x01, Bolus.class);
        recordMap.put((byte) 0x03, Prime.class);
        recordMap.put((byte) 0x06, NoDeliveryAlarm.class);
        recordMap.put((byte) 0x07, EndResultsTotals.class);
        recordMap.put((byte) 0x08, ChangeBasalProfile.class);
        recordMap.put((byte) 0x09, ChangeBasalProfile.class);
        recordMap.put((byte) 0x0A, CalBgForPh.class);
        recordMap.put((byte) 0x0c, ClearAlarm.class);
        recordMap.put((byte) 0x14, SelectBasalProfile.class);
        recordMap.put((byte) 0x17, ChangeTime.class);
        recordMap.put((byte) 0x18, NewTimeSet.class);
        recordMap.put((byte) 0x19, LowBattery.class);
        recordMap.put((byte) 0x1A, BatteryActivity.class);
        recordMap.put((byte) 0x1E, PumpSuspended.class);
        recordMap.put((byte) 0x1F, PumpResumed.class);
        recordMap.put((byte) 0x21, Rewound.class);
        recordMap.put((byte) 0x26, ToggleRemote.class);
        recordMap.put((byte) 0x27, ChangeRemoteId.class);
        recordMap.put((byte) 0x16, TempBasalDuration.class);
        recordMap.put((byte) 0x33, TempBasalRate.class);
        recordMap.put((byte) 0x34, LowReservoir.class);
        recordMap.put((byte) 0x5a, BolusWizardChange.class);
        recordMap.put((byte) 0x5b, BolusWizard.class);
        recordMap.put((byte) 0x5c, UnabsorbedInsulin.class);
        recordMap.put((byte) 0x6c, Old6c.class);
        recordMap.put((byte) 0x6d, ResultTotals.class);
        recordMap.put((byte) 0x6e, MidnightTotals.class);
        recordMap.put((byte) 0x63, ChangeUtility.class);
        recordMap.put((byte) 0x64, ChangeTimeDisplay.class);
        recordMap.put((byte) 0x7b, BasalProfileStart.class);
    }
}
