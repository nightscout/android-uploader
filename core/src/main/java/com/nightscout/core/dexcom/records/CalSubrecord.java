package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;

import org.joda.time.DateTime;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class CalSubrecord {
    private DateTime dateEntered;
    private int rawDateEntered;
    private int calBGL;
    private int calRaw;
    private DateTime dateApplied;
    private int rawDateApplied;
    private byte unk;

    public CalSubrecord(byte[] packet, long displayTimeOffset) {
        int delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt();
        rawDateEntered = delta;
        dateEntered = Utils.receiverTimeToDate(delta + displayTimeOffset);
        calBGL = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        calRaw = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
        delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
        rawDateApplied = delta;
        dateApplied = Utils.receiverTimeToDate(delta + displayTimeOffset);
        unk = packet[16];
    }

    public CalSubrecord(int calBGL, int calRaw, int dateApplied, int dateEntered) {
        this.calBGL = calBGL;
        this.calRaw = calRaw;
        this.rawDateEntered = dateEntered;
        this.rawDateApplied = dateApplied;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalSubrecord that = (CalSubrecord) o;

        if (calBGL != that.calBGL) return false;
        if (calRaw != that.calRaw) return false;
        if (rawDateApplied != that.rawDateApplied) return false;
        if (rawDateEntered != that.rawDateEntered) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rawDateEntered;
        result = 31 * result + calBGL;
        result = 31 * result + calRaw;
        result = 31 * result + rawDateApplied;
        return result;
    }
}
