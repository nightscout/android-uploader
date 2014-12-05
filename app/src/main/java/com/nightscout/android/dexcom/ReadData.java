package com.nightscout.android.dexcom;

import android.util.Log;
import com.nightscout.android.dexcom.USB.UsbSerialDriver;
import com.nightscout.core.dexcom.*;
import com.nightscout.core.dexcom.records.*;
import org.w3c.dom.Element;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

public class ReadData {

    private static final String TAG = ReadData.class.getSimpleName();
    private static final int IO_TIMEOUT = 1000;
    private static final int MIN_LEN = 256;
    private UsbSerialDriver mSerialDevice;

    public ReadData(UsbSerialDriver device) {
        mSerialDevice = device;
    }

    public EGVRecord[] getRecentEGVs() {
        int endPage = readDataBasePageRange(RecordType.EGV_DATA);
        return readDataBasePage(RecordType.EGV_DATA, endPage);
    }

    public EGVRecord[] getRecentEGVsPages(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading EGV page range...");
        int endPage = readDataBasePageRange(RecordType.EGV_DATA);
        Log.d(TAG, "Reading " + numOfRecentPages + " EGV page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        EGVRecord[] allPages = new EGVRecord[0];
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " EGV pages (page number " + nextPage + ")");
            EGVRecord[] ithEGVRecordPage = readDataBasePage(RecordType.EGV_DATA, nextPage);
            EGVRecord[] result = Arrays.copyOf(allPages, allPages.length + ithEGVRecordPage.length);
            System.arraycopy(ithEGVRecordPage, 0, result, allPages.length, ithEGVRecordPage.length);
            allPages = result;
        }
        Log.d(TAG, "Read complete of EGV pages.");
        return allPages;
    }

    public long getTimeSinceEGVRecord(EGVRecord egvRecord) {
        return readSystemTime() - egvRecord.getRawSystemTimeSeconds();
    }

    public MeterRecord[] getRecentMeterRecords() {
        Log.d(TAG, "Reading Meter page...");
        int endPage = readDataBasePageRange(RecordType.METER_DATA);
        return readDataBasePage(RecordType.METER_DATA, endPage);
    }

    public SensorRecord[] getRecentSensorRecords(int numOfRecentPages) {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        Log.d(TAG, "Reading Sensor page range...");
        int endPage = readDataBasePageRange(RecordType.SENSOR_DATA);
        Log.d(TAG, "Reading " + numOfRecentPages + " Sensor page(s)...");
        numOfRecentPages = numOfRecentPages - 1;
        SensorRecord[] allPages = new SensorRecord[0];
        for (int i = Math.min(numOfRecentPages,endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            Log.d(TAG, "Reading #" + i + " Sensor pages (page number " + nextPage + ")");
            SensorRecord[] ithSensorRecordPage = readDataBasePage(RecordType.SENSOR_DATA, nextPage);
            SensorRecord[] result = Arrays.copyOf(allPages, allPages.length + ithSensorRecordPage.length);
            System.arraycopy(ithSensorRecordPage, 0, result, allPages.length, ithSensorRecordPage.length);
            allPages = result;
        }
        Log.d(TAG, "Read complete of Sensor pages.");
        return allPages;
    }

    public CalRecord[] getRecentCalRecords() {
        Log.d(TAG, "Reading Cal Records page range...");
        int endPage = readDataBasePageRange(RecordType.CAL_SET);
        Log.d(TAG, "Reading Cal Records page...");
        return readDataBasePage(RecordType.CAL_SET, endPage);
    }

    public boolean ping() {
        writeCommand(Command.PING);
        return read(MIN_LEN).getCommand() == Command.ACK;
    }

    public int readBatteryLevel() {
        Log.d(TAG, "Reading battery level...");
        writeCommand(Command.READ_BATTERY_LEVEL);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() {
        int PAGE_OFFSET = 0;
        byte[] readData = readDataBasePage(RecordType.MANUFACTURING_DATA, PAGE_OFFSET);
        Element md = ParsePage(readData, RecordType.MANUFACTURING_DATA);
        return md.getAttribute("SerialNumber");
    }

    public Date readDisplayTime() {
        return Utils.receiverTimeToDate(readSystemTime() + readDisplayTimeOffset());
    }

    public long readSystemTime() {
        Log.d(TAG, "Reading system time...");
        writeCommand(Command.READ_SYSTEM_TIME);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int readDisplayTimeOffset() {
        Log.d(TAG, "Reading display time offset...");
        writeCommand(Command.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private int readDataBasePageRange(RecordType recordType) {
        ArrayList<Byte> payload = new ArrayList<Byte>();
        payload.add((byte) recordType.ordinal());
        writeCommand(Command.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private <T> T readDataBasePage(RecordType recordType, int page) {
        byte numOfPages = 1;
        if (page < 0){
            throw new IllegalArgumentException("Invalid page requested:" + page);
        }
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) recordType.ordinal());
        byte[] pageInt = ByteBuffer.allocate(4).putInt(page).array();
        payload.add(pageInt[3]);
        payload.add(pageInt[2]);
        payload.add(pageInt[1]);
        payload.add(pageInt[0]);
        payload.add(numOfPages);
        writeCommand(Command.READ_DATABASE_PAGES, payload);
        byte[] readData = read(2122).getData();
        return ParsePage(readData, recordType);
    }

    private void writeCommand(Command command, ArrayList<Byte> payload) {
        byte[] packet = new PacketBuilder(command, payload).build();
        if (mSerialDevice != null) {
            try {
                Log.d("XXX","Command: "+command.name());
                Log.d("XXX", "Payload: " + Utils.bytesToHex(packet));
                mSerialDevice.write(packet, IO_TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }

    public void writeCommand(Command command) {
        byte[] packet = new PacketBuilder(command).build();
        if (mSerialDevice != null) {
            try {
                Log.d("XXX","Command: "+command.name());
                Log.d("XXX", "Payload: " + Utils.bytesToHex(packet));
                mSerialDevice.write(packet, IO_TIMEOUT);
            } catch (IOException e) {
                Log.e(TAG, "Unable to write to serial device.", e);
            }
        }
    }

    private ReadPacket read(int numOfBytes) {
        byte[] readData = new byte[numOfBytes];
        UsbSerialDriver.ReadResponse response = new UsbSerialDriver.ReadResponse();
        int len = 0;
        try {
            response = mSerialDevice.read(numOfBytes, IO_TIMEOUT);
            len = response.size;
            Log.d(TAG, "Read " + len + " byte(s) complete.");

            // Add a 100ms delay for when multiple write/reads are occurring in series
            Thread.sleep(100);

            // TODO: this debug code to print data of the read, should be removed after
            // finding the source of the reading issue
            String bytes = "";
            int readAmount = len;
            for (int i = 0; i < readAmount; i++) bytes += String.format("%02x", response.data[i]) + " ";
            Log.d(TAG, "Read data: " + bytes);
            ////////////////////////////////////////////////////////////////////////////////////////
        } catch (IOException e) {
            Log.e(TAG, "Unable to read from serial device.", e);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
//        byte[] data = Arrays.copyOfRange(readData, 0, len);
        return new ReadPacket(response.data);
    }

    private <T> T ParsePage(byte[] data, RecordType recordType) {
        PageHeader pageHeader=new PageHeader(data);
        int NUM_REC_OFFSET = 4;
        int numRec = data[NUM_REC_OFFSET];

        switch (recordType) {
            case MANUFACTURING_DATA:
                GenericXMLRecord xmlRecord = new GenericXMLRecord(Arrays.copyOfRange(data, PageHeader.HEADER_SIZE, data.length - 1));
                return (T) xmlRecord;
            case SENSOR_DATA:
                SensorRecord[] sensorRecords = new SensorRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = PageHeader.HEADER_SIZE + (SensorRecord.RECORD_SIZE + 1) * i;
                    try {
                        sensorRecords[i] = new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + SensorRecord.RECORD_SIZE));
                    } catch (InvalidRecordLengthException e) {
                        e.printStackTrace();
                    }
                }
                return (T) sensorRecords;
            case EGV_DATA:
                EGVRecord[] egvRecords = new EGVRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = PageHeader.HEADER_SIZE + (EGVRecord.RECORD_SIZE + 1) * i;
                    try {
                        egvRecords[i] = new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + EGVRecord.RECORD_SIZE));
                    } catch (InvalidRecordLengthException e) {
                        e.printStackTrace();
                    }
                }
                return (T) egvRecords;
            case METER_DATA:
                MeterRecord[] meterRecords = new MeterRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = PageHeader.HEADER_SIZE + (MeterRecord.RECORD_SIZE + 1) * i;
                    try {
                        meterRecords[i] = new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + MeterRecord.RECORD_SIZE));
                    } catch (InvalidRecordLengthException e) {
                        e.printStackTrace();
                    }
                }
                return (T) meterRecords;
            case CAL_SET:
                int rec_len = CalRecord.RECORD_V2_SIZE;
                if (pageHeader.getRevision()<=2) {
                    rec_len = CalRecord.RECORD_SIZE;
                }
                CalRecord[] calRecords = new CalRecord[numRec];
                for (int i = 0; i < numRec; i++) {
                    int startIdx = PageHeader.HEADER_SIZE + (rec_len + 1) * i;
                    try {
                        calRecords[i] = new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + rec_len));
                    } catch (InvalidRecordLengthException e) {
                        e.printStackTrace();
                    }
                }
                return (T) calRecords;
            default:
                // Throw error "Database record not supported"
                break;
        }

        return null;
    }
}