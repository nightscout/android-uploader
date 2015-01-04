package com.nightscout.core.drivers.Medtronic;

public enum RecordType {
    BATTERY_ACTIVITY((byte) 0x1A, (byte) 1, (byte) 5, (byte) 0, "Battery Activity"),
    PUMP_SUSPENDED((byte) 0x1E, (byte) 1, (byte) 5, (byte) 0, "Pump suspended"),
    PUMP_RESUMED((byte) 0x1F, (byte) 1, (byte) 5, (byte) 0, "Pump resumed"),
    REWOUND((byte) 0x21, (byte) 1, (byte) 5, (byte) 0, "Rewound"),
    TOGGLE_REMOTE((byte) 0x26, (byte) 1, (byte) 5, (byte) 14, "Enable/Disable remote"),
    CHANGE_REMOTE_ID((byte) 0x27, (byte) 1, (byte) 5, (byte) 0, "Change handheld remote ID"),
    TEMP_BASAL_DURATION((byte) 0x16, (byte) 1, (byte) 5, (byte) 0, "Temp basal duration"),
    TEMP_BASAL_RATE((byte) 0x33, (byte) 1, (byte) 5, (byte) 1, "Temp basal rate"),
    LOW_RESERVOIR((byte) 0x34, (byte) 1, (byte) 5, (byte) 0, "Low Reservoir warning"),
    CHANGE_UTILITY((byte) 0x63, (byte) 1, (byte) 5, (byte) 0, "Change Utility"),
    CHANGE_TIME_DISPLAY((byte) 0x64, (byte) 1, (byte) 5, (byte) 0, "Change time display"),
    BASAL_PROFILE_START((byte) 0x7B, (byte) 1, (byte) 5, (byte) 3, "Basal profile start"),
    OLD_BOLUS_WIZARD_CHANGE((byte) 0x5a, (byte) 1, (byte) 5, (byte) 117, "Bolus Wizard Change"),
    BIG_BOLUS_WIZARD_CHANGE((byte) 0x5a, (byte) 1, (byte) 5, (byte) 143, "Bolus Wizard Change"),
    OLD_6C((byte) 0x6c, (byte) 1, (byte) 0, (byte) 38, "Old 6C"),
    RESULT_TOTALS((byte) 0x6D, (byte) 2, (byte) 0, (byte) 40, "Result totals"),
    UNK1((byte) 0x6E, (byte) 2, (byte) 0, (byte) 48, "Unknown - Midnight totals"),
    NO_DELIVERY_ALARM((byte) 0x06, (byte) 3, (byte) 5, (byte) 0, "No delivery alarm"),
    END_RESULTS_TOTALS((byte) 0x07, (byte) 4, (byte) 2, (byte) 0, "End results totals"), // Is this right?
    OLD_CHANGE_BASAL_PROFILE((byte) 0x08, (byte) 1, (byte) 5, (byte) 145, "Change basal profile"),
    NEW_CHANGE_BASAL_PROFILE((byte) 0x09, (byte) 1, (byte) 5, (byte) 145, "Change basal profile"),
    CLEAR_ALARM((byte) 0x0C, (byte) 1, (byte) 5, (byte) 0, "Clear alarm"),
    SELECT_BASAL_PROFILE((byte) 0x14, (byte) 1, (byte) 5, (byte) 0, "Select basal profile"),
    CHANGE_TIME((byte) 0x17, (byte) 1, (byte) 5, (byte) 0, "Change time"),
    NEW_TIME_SET((byte) 0x18, (byte) 1, (byte) 5, (byte) 0, "New time set"),
    LOW_BATTERY((byte) 0x19, (byte) 1, (byte) 5, (byte) 0, "Low battery"),
    BOLUS_508((byte) 0x01, (byte) 3, (byte) 5, (byte) 0, "Bolus"),
    BOLUS_523((byte) 0x01, (byte) 7, (byte) 5, (byte) 0, "Bolus"),
    BOLUS_WIZARD_522_EARLIER((byte) 0x5b, (byte) 1, (byte) 5, (byte) 13, "Bolus Wizard"),
    BOLUS_WIZARD_523((byte) 0x5b, (byte) 1, (byte) 5, (byte) 15, "Bolus Wizard"),
    UNABSORBED_INSULIN((byte) 0x5c, (byte) 1, (byte) 0, (byte) -1, "Unabsorbed insulin"),
    PRIME((byte) 0x03, (byte) 1, (byte) 5, (byte) 0, "Prime"),
    CAL_BG_FOR_PH((byte) 0x0A, (byte) 1, (byte) 5, (byte) 0, "Cal BG for PH");

    private byte opCode;
    private byte headerSize;
    private byte dateSize;
    private byte bodySize;
    private Class clazz;
    private String description;

    RecordType(byte opCode, byte headerSize, byte dateSize, byte bodySize, String description) {
        this.opCode = opCode;
        this.headerSize = headerSize;
        this.dateSize = dateSize;
        this.bodySize = bodySize;
        this.description = description;
    }

}
