package com.nightscout.core.dexcom;


import com.nightscout.core.model.Trend;

public enum TrendArrow {
    NONE(0),
    DOUBLE_UP(1, "\u21C8", "DoubleUp"),
    SINGLE_UP(2, "\u2191", "SingleUp"),
    UP_45(3, "\u2197", "FortyFiveUp"),
    FLAT(4, "\u2192", "Flat"),
    DOWN_45(5, "\u2198", "FortyFiveDown"),
    SINGLE_DOWN(6, "\u2193", "SingleDown"),
    DOUBLE_DOWN(7, "\u21CA", "DoubleDown"),
    NOT_COMPUTABLE(8),
    OUT_OF_RANGE(9);

    private String arrowSymbol;
    private String trendName;
    private int myID;

    TrendArrow(int id, String a, String t) {
        myID = id;
        arrowSymbol = a;
        trendName = t;
    }

    TrendArrow(int id) {
        this(id, null, null);
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

    public int getID() {
        return myID;
    }

    public Trend toProtobuf() {
        return Trend.values()[myID];
    }

}