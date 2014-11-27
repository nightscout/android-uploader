package com.nightscout.core.dexcom;

public enum NoiseMode {
    None(0),
    Clean(1),
    Light(2),
    Medium(3),
    Heavy(4),
    NotComputed(5),
    Max(6);

    private int index;
    private NoiseMode(int i){
        index=i;
    }

    public int getValue(){
        return index;
    }

    public static NoiseMode getNoiseMode(int val){
        for (NoiseMode e: values()){
            if (e.getValue()==val)
                return e;
        }
        return null;
    }

}