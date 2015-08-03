package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.InvalidRecordLengthException;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.model.G4Insertion;
import com.nightscout.core.model.InsertionEntry;
import com.nightscout.core.model.v2.G4Timestamp;
import com.nightscout.core.model.v2.Insertion;

import net.tribe7.common.base.Function;

import java.util.List;

public class InsertionRecord extends GenericTimestampRecord {
    public static final int RECORD_SIZE = 14;
    private G4Insertion state = G4Insertion.INSERTION_NONE;

    public InsertionRecord(byte[] packet) {
        super(packet);
        if (packet.length != RECORD_SIZE) {
            throw new InvalidRecordLengthException("Unexpected record size: " + packet.length +
                    ". Expected size: " + RECORD_SIZE + ". Unparsed record: " + Utils.bytesToHex(packet));
        }
        state = G4Insertion.values()[packet[12]];
        setRecordType();
    }

    public InsertionRecord(InsertionEntry insertionEntry, long rcvrTime, long refTime) {
        super(insertionEntry.disp_timestamp_sec, insertionEntry.sys_timestamp_sec, rcvrTime, refTime);
        this.state = insertionEntry.state;
        setRecordType();
    }

    @Override
    protected void setRecordType() {
        this.recordType = "ins";
    }

    public G4Insertion getState() {
        return state;
    }

    public static List<InsertionEntry> toProtobufList(List<InsertionRecord> list) {
        return toProtobufList(list, InsertionEntry.class);
    }

    protected InsertionEntry toProtobuf() {
        InsertionEntry.Builder builder = new InsertionEntry.Builder();
        return builder.sys_timestamp_sec(rawSystemTimeSeconds)
                .disp_timestamp_sec(rawDisplayTimeSeconds)
                .state(G4Insertion.values()[state.ordinal()])
                .build();
    }

    public static Function<InsertionRecord, Insertion> v2ModelConverter() {
        return new Function<InsertionRecord, Insertion>() {
            @Override
            public Insertion apply(InsertionRecord input) {
                return new Insertion.Builder()
                    .state(Insertion.State.values()[input.getState().ordinal()])
                    .timestamp(new G4Timestamp(input.getRawSystemTimeSeconds(),
                                               input.getRawDisplayTimeSeconds()))
                    .build();
            }
        };
    }
}
