package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class CalSubrecord {
    private Date dateEntered;
    private int rawDateEntered;
    private int calBGL;
    private int calRaw;
    private Date dateApplied;
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

    public CalSubrecord(int calBGL, int calRaw, Date dateApplied, Date dateEntered) {
        this.calBGL = calBGL;
        this.calRaw = calRaw;
        this.dateEntered = dateEntered;
        this.dateApplied = dateApplied;
    }

    public CalSubrecord(int calBGL, int calRaw, int dateApplied, int dateEntered) {
        this.calBGL = calBGL;
        this.calRaw = calRaw;
        this.rawDateEntered = dateEntered;
        this.rawDateApplied = dateApplied;
    }


    public Date getDateEntered() {
        return dateEntered;
    }

    public int getCalBGL() {
        return calBGL;
    }

    public int getCalRaw() {
        return calRaw;
    }

    public Date getDateApplied() {
        return dateApplied;
    }

    public byte getUnk() {
        return unk;
    }

    public int getRawDateEntered() {
        return rawDateEntered;
    }

    public void setRawDateEntered(int rawDateEntered) {
        this.rawDateEntered = rawDateEntered;
    }

    public int getRawDateApplied() {
        return rawDateApplied;
    }

    public void setRawDateApplied(int rawDateApplied) {
        this.rawDateApplied = rawDateApplied;
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
