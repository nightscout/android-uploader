package com.nightscout.core.dexcom;

import com.nightscout.core.protobuf.G4Download;

public enum NoiseMode {
    NONE(0),
    CLEAN(1),
    LIGHT(2),
    MEDIUM(3),
    HEAVY(4),
    NOT_COMPUTED(5),
    MAX(6);

    private int index;
    private NoiseMode(int i){
        index=i;
    }

    public int getValue(){
        return index;
    }

    public static NoiseMode getNoiseMode(int val){
        for (NoiseMode e: values()){
            if (e.getValue() == val)
                return e;
        }
        return null;
    }

    public G4Download.Noise toProtobuf(){
        return G4Download.Noise.values()[index];
    }

}