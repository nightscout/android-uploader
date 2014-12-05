package com.nightscout.core.dexcom;

public enum SpecialValue {
    NONE("??0", 0),
    SENSOR_NOT_ACTIVE("?SN", 1),
    MINIMALLY_EGV_AB("??2", 2),
    NO_ANTENNA("?NA", 3),
    SENSOR_OUT_OF_CAL("?NC", 5),
    COUNTS_AB("?CD", 6),
    ABSOLUTE_AB("?AD", 9),
    POWER_AB("???", 10),
    RF_BAD_STATUS("?RF", 12);


    private String name;
    private int val;
    SpecialValue(String s, int i){
        name=s;
        val=i;
    }

    public int getValue(){
        return val;
    }

    public String toString(){
        return name;
    }

    public static SpecialValue getEGVSpecialValue(int val){
        for (SpecialValue e: values()){
            if (e.getValue()==val)
                return e;
        }
        return null;
    }

    public static boolean isSpecialValue(int val){
        for (SpecialValue e: values()){
            if (e.getValue()==val)
                return true;
        }
        return false;
    }

}
