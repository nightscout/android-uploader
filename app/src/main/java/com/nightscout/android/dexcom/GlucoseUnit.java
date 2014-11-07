package com.nightscout.android.dexcom;


public enum GlucoseUnit {
    MGDL(0, "mg/dL"),
    MMOL(1, "mmol/L");

    private int id;
    private String friendlyName;

    GlucoseUnit(int ID, String friendlyName) {
        this.id = ID;
        this.friendlyName = friendlyName;
    }

    public String getFriendlyName() {
        return friendlyName;
    }

    public int getId() {
        return id;
    }
}
