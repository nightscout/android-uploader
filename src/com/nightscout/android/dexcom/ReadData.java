package com.nightscout.android.dexcom;

import android.os.AsyncTask;
import android.util.Log;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.android.dexcom.records.EGRecord;
import com.nightscout.android.dexcom.records.GenericXMLRecord;
import com.nightscout.android.dexcom.records.MeterRecord;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;

public class ReadData extends AsyncTask<UsbSerialDriver, Object, Object> {

    private static final String TAG = ReadData.class.getSimpleName();
    private static final int IO_TIMEOUT = 200;
    private static final int MIN_LEN = 256;
    private UsbSerialDriver mSerialDevice;

    public ReadData(UsbSerialDriver device) {
        mSerialDevice = device;
    }

    public boolean ping() {
        writeCommand(Constants.PING);
        return read(MIN_LEN).getCommand() == Constants.ACK;
    }

    public long readDisplayTimeOffset() {
        writeCommand(Constants.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffffL;
    }

    public int readDataBasePageRange(int recordType) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        writeCommand(Constants.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private <T> T readDataBasePage(int recordType, int page) {
        byte numOfPages = 1;
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        byte[] pageInt = ByteBuffer.allocate(4).putInt(page).array();
        payload.add(pageInt[3]);
        payload.add(pageInt[2]);
        payload.add(pageInt[1]);
        payload.add(pageInt[0]);
        payload.add(numOfPages);
        writeCommand(Constants.READ_DATABASE_PAGES, payload);
        byte[] readData = read(2122).getData();
        ParsePage(readData, recordType);
        return ParsePage(readData, recordType);
    }

    private void writeCommand(int command, ArrayList<Byte> payload) {
        byte[] packet = new PacketBuilder(command, payload).compose();
        try {
            mSerialDevice.write(packet, IO_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to serial device.", e);
        }
    }

    private void writeCommand(int command) {
        byte[] packet = new PacketBuilder(command).compose();
        try {
            mSerialDevice.write(packet, IO_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to write to serial device.", e);
        }
    }

    private ReadPacket read(int numOfBytes) {
        byte[] readData = new byte[numOfBytes];
        int len = 0;
        try {
            len = mSerialDevice.read(readData, IO_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read from serial device.", e);
        }
        byte[] data = Arrays.copyOfRange(readData, 0, len);
        return new ReadPacket(data);
    }

    // TODO: not sure if I want to use generics or just separate methods, hmmm
    private <T> T ParsePage(byte[] data, int recordType) {
        int HEADER_LEN = 28;
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (Constants.RECORD_TYPES.values()[recordType]) {
            case EGV_DATA:
                rec_len = 13;
                EGRecord[] egRecords = new EGRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    egRecords[i] = new EGRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) egRecords;
            case METER_DATA:
                rec_len = 16;
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) meterRecords;
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(data);
                return (T) xmlRecord;
            default:
                // Throw error "Database record not supported"
                break;
        }

        return (T) null;
    }

    @Override
    protected Object doInBackground(UsbSerialDriver... params) {
        // TODO Auto-generated method stub
        return null;
    }
}