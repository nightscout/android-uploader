package com.nightscout.android.dexcom;

import android.util.Log;

import com.nightscout.android.devices.Constants;
import com.nightscout.android.devices.DeviceTransportAbstract;
import com.nightscout.android.dexcom.records.CalRecord;
import com.nightscout.android.dexcom.records.EGVRecord;
import com.nightscout.android.dexcom.records.GenericXMLRecord;
import com.nightscout.android.dexcom.records.MeterRecord;
import com.nightscout.android.dexcom.records.PageHeader;
import com.nightscout.android.dexcom.records.SensorRecord;

import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ReadData {

    private static final String TAG = ReadData.class.getSimpleName();
    private static final int IO_TIMEOUT = 1000;
    private static final int MIN_LEN = 256;
    private DeviceTransportAbstract transport;

    public ReadData(DeviceTransportAbstract transport) {
        this.transport = transport;
    }

    public EGVRecord[] getRecentEGVs() {
        int recordType = RecordTypes.EGV_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        return readDataBasePage(recordType, endPage);
    }

    // TODO: Add unit test once transport abstraction is in place. Test that < 20 pages work with
    // gap sync
    public List<EGVRecord> getRecentEGVsPages(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading EGV page range...");
        int recordType = RecordTypes.EGV_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading " + numOfRecentPages + " EGV page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        List<EGVRecord> allPages = new ArrayList<EGVRecord>();
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " EGV pages (page number " + nextPage + ")");
            EGVRecord[] ithEGVRecordPage = readDataBasePage(recordType, nextPage);
            allPages.addAll(Arrays.asList(ithEGVRecordPage));
        }
        Log.d(TAG, "Read complete of EGV pages.");
        return allPages;
    }

    public long getTimeSinceEGVRecord(EGVRecord egvRecord) {
        return readSystemTime() - egvRecord.getSystemTimeSeconds();
    }

    public List<MeterRecord> getRecentMeterRecords() {
        int recordType = RecordTypes.METER_DATA.ordinal();
        Log.d(TAG, "Reading Meter page...");
        int endPage = readDataBasePageRange(recordType);
        //FIXME - too much casting going on here
        return Arrays.asList((MeterRecord[]) readDataBasePage(recordType, endPage));
    }

    // TODO: Add unit test once transport abstraction is in place. Test that < 20 pages work with
    // gap sync
    public List<SensorRecord> getRecentSensorRecords(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading Sensor page range...");
        int recordType = RecordTypes.SENSOR_DATA.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading " + numOfRecentPages + " Sensor page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        List<SensorRecord> allPages = new ArrayList<SensorRecord>();
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " Sensor pages (page number " + nextPage + ")");
            SensorRecord[] ithSensorRecordPage = readDataBasePage(recordType, nextPage);
            allPages.addAll(Arrays.asList(ithSensorRecordPage));
        }
        Log.d(TAG, "Read complete of Sensor pages.");
        return allPages;
    }

    public List<CalRecord> getRecentCalRecords() {
        Log.d(TAG, "Reading Cal Records page range...");
        int recordType = RecordTypes.CAL_SET.ordinal();
        int endPage = readDataBasePageRange(recordType);
        Log.d(TAG, "Reading Cal Records page...");
        //FIXME - too much casting going on here
        return Arrays.asList((CalRecord[]) readDataBasePage(recordType, endPage));
    }

    public boolean ping() {
        writeCommand(Constants.PING);
        return read(MIN_LEN).getCommand() == Constants.ACK;
    }

    public int readBatteryLevel() {
        Log.d(TAG, "Reading battery level...");
        writeCommand(Constants.READ_BATTERY_LEVEL);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() {
        int PAGE_OFFSET = 0;
        byte[] readData = readDataBasePage(RecordTypes.MANUFACTURING_DATA.ordinal(), PAGE_OFFSET);
        Element md = ParsePage(readData, RecordTypes.MANUFACTURING_DATA.ordinal());
        return md.getAttribute("SerialNumber");
    }

    public Date readDisplayTime() {
        return Utils.receiverTimeToDate(readSystemTime() + readDisplayTimeOffset());
    }

    public long readSystemTime() {
        Log.d(TAG, "Reading system time...");
        writeCommand(Constants.READ_SYSTEM_TIME);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    public int readDisplayTimeOffset() {
        Log.d(TAG, "Reading display time offset...");
        writeCommand(Constants.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt() & 0xffffffff;
    }

    private int readDataBasePageRange(int recordType) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType);
        writeCommand(Constants.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private <T> T readDataBasePage(int recordType, int page) {
        byte numOfPages = 1;
        if (page < 0){
            throw new IllegalArgumentException("Invalid page requested:" + page);
        }
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
        return ParsePage(readData, recordType);
    }

    private void writeCommand(int command, ArrayList<Byte> payload) {
        byte[] packet = new PacketBuilder(command, payload).compose();
        if (transport != null) {
            try {
                transport.write(packet, IO_TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }

    private void writeCommand(int command) {
        byte[] packet = new PacketBuilder(command).compose();
        if (transport != null) {
            try {
                transport.write(packet, IO_TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }

    private ReadPacket read(int numOfBytes) {
        byte[] readData = new byte[numOfBytes];
        int len = 0;
        try {
            len = transport.read(readData, IO_TIMEOUT);
        } catch (IOException e) {
            Log.e(TAG, "Unable to read from serial device.", e);
        }
        byte[] data = Arrays.copyOfRange(readData, 0, len);
        return new ReadPacket(data);
    }

    private <T> T ParsePage(byte[] data, int recordType) {
        int HEADER_LEN = 28;
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];
        int rec_len;

        switch (RecordTypes.values()[recordType]) {
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(Arrays.copyOfRange(data, HEADER_LEN, data.length - 1));
                return (T) xmlRecord;
            case SENSOR_DATA:
                rec_len = 20;
                SensorRecord[] sensorRecords = new SensorRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    sensorRecords[i] = new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) sensorRecords;
            case EGV_DATA:
                rec_len = 13;
                EGVRecord[] egvRecords = new EGVRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    egvRecords[i] = new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) egvRecords;
            case METER_DATA:
                rec_len = 16;
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) meterRecords;
            case CAL_SET:
                rec_len = 249;
                if (pageHeader.getRevision()<=2) {
                    rec_len = 148;
                }
                CalRecord[] calRecords = new CalRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = HEADER_LEN + rec_len * i;
                    calRecords[i] = new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len - 1));
                }
                return (T) calRecords;
            default:
                // Throw error "Database record not supported"
                break;
        }

        return (T) null;
    }
}