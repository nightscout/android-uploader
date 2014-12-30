package com.nightscout.core.utils;

import com.nightscout.core.dexcom.Constants;
import com.nightscout.core.model.GlucoseUnit;

public class GlucoseReading {
    private int valueMgdl;

    public GlucoseReading(float value, GlucoseUnit units) {
        this.valueMgdl = (units == GlucoseUnit.MGDL) ?
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

    public float as(GlucoseUnit units) {
        return (units == GlucoseUnit.MGDL) ? asMgdl() : asMmol();
    }

    public String asStr(GlucoseUnit units) {
        return (units == GlucoseUnit.MGDL) ? asMgdlStr() : asMmolStr();
    }

    public GlucoseReading subtract(GlucoseReading reading) {
        return new GlucoseReading(valueMgdl - reading.asMgdl(), GlucoseUnit.MGDL);
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
