package com.nightscout.core.utils;

import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.protobuf.G4Download;

public class GlucoseReading {
    private int valueMgdl;

    public GlucoseReading(float value, G4Download.GlucoseUnit units) {
        this.valueMgdl = (units == G4Download.GlucoseUnit.MGDL) ?
                Math.round(value) : Math.round(value * Constants.MMOL_L_TO_MG_DL);
    }

    public float asMmol() {
        return valueMgdl * Constants.MG_DL_TO_MMOL_L;
    }

    public String asMmolStr() {
        return String.format("%.1f", asMmol());
    }

    public int asMgdl() {
        return valueMgdl;
    }

    public String asMgdlStr() {
        return String.valueOf(valueMgdl);
    }

    public float as(G4Download.GlucoseUnit units) {
        return (units == G4Download.GlucoseUnit.MGDL) ? asMgdl() : asMmol();
    }

    public String asStr(G4Download.GlucoseUnit units) {
        return (units == G4Download.GlucoseUnit.MGDL) ? asMgdlStr() : asMmolStr();
    }

    public GlucoseReading subtract(GlucoseReading reading) {
        return new GlucoseReading(valueMgdl - reading.asMgdl(), G4Download.GlucoseUnit.MGDL);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GlucoseReading that = (GlucoseReading) o;

        if (valueMgdl != that.valueMgdl) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return valueMgdl;
    }
}
