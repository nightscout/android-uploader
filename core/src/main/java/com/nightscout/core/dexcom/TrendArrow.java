package com.nightscout.core.dexcom;


import com.nightscout.core.model.G4Trend;

public enum TrendArrow {
    NONE,
    DOUBLE_UP("\u21C8", "DoubleUp"),
    SINGLE_UP("\u2191", "SingleUp"),
    UP_45("\u2197", "FortyFiveUp"),
    FLAT("\u2192", "Flat"),
    DOWN_45("\u2198", "FortyFiveDown"),
    SINGLE_DOWN("\u2193", "SingleDown"),
    DOUBLE_DOWN("\u21CA", "DoubleDown"),
    NOT_COMPUTABLE,
    OUT_OF_RANGE;

    private String arrowSymbol;
    private String trendName;

    TrendArrow(String arrowSymbol, String trendName) {
        this.arrowSymbol = arrowSymbol;
        this.trendName = trendName;
    }

    TrendArrow() {
        this(null, null);
    }

    public String symbol() {
        if (arrowSymbol == null) {
            return "\u2194";
        } else {
            return arrowSymbol;
        }
    }

    public String friendlyTrendName() {
        if (trendName == null) {
            return this.name().replace("_", " ");
        } else {
            return this.trendName;
        }
    }

    public G4Trend toProtobuf() {
        return G4Trend.values()[ordinal()];
    }

}