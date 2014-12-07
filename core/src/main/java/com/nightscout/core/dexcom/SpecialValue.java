package com.nightscout.core.dexcom;

public enum SpecialValue {
    NONE("??0", 0),
    SENSORNOTACTIVE("?SN", 1),
    MINIMALLYEGVAB("??2", 2),
    NOANTENNA("?NA", 3),
    SENSOROUTOFCAL("?NC", 5),
    COUNTSAB("?CD", 6),
    ABSOLUTEAB("?AD", 9),
    POWERAB("???", 10),
    RFBADSTATUS("?RF", 12);


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
