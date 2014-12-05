package com.nightscout.core.dexcom.records;

import com.nightscout.core.dexcom.Utils;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Date;

public class CalSubrecord {
    private Date dateEntered;
    private int calBGL;
    private int calRaw;
    private Date dateApplied;
    private byte unk;

    public CalSubrecord(byte[] packet, long displayTimeOffset) {
        int delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt();
        dateEntered = Utils.receiverTimeToDate(delta + displayTimeOffset);
        calBGL = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
        calRaw = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(8);
        delta = ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).getInt(12);
        dateApplied = Utils.receiverTimeToDate(delta + displayTimeOffset);
        unk = packet[16];
    }

    public CalSubrecord(int calBGL, int calRaw, Date dateApplied, Date dateEntered) {
        this.calBGL = calBGL;
        this.calRaw = calRaw;
        this.dateEntered = dateEntered;
        this.dateApplied = dateApplied;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CalSubrecord that = (CalSubrecord) o;

        if (calBGL != that.calBGL) return false;
        if (calRaw != that.calRaw) return false;
        if (dateApplied != null ? !dateApplied.equals(that.dateApplied) : that.dateApplied != null) return false;
        if (dateEntered != null ? !dateEntered.equals(that.dateEntered) : that.dateEntered != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = dateEntered != null ? dateEntered.hashCode() : 0;
        result = 31 * result + calBGL;
        result = 31 * result + calRaw;
        result = 31 * result + (dateApplied != null ? dateApplied.hashCode() : 0);
        return result;
    }
}
