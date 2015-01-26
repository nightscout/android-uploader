package com.nightscout.core.drivers;

import com.google.common.base.Optional;
import com.nightscout.core.dexcom.Command;
import com.nightscout.core.dexcom.PacketBuilder;
import com.nightscout.core.dexcom.ReadPacket;
import com.nightscout.core.dexcom.RecordType;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GenericTimestampRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.PageHeader;
import com.nightscout.core.dexcom.records.SensorRecord;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ReadData {

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final int IO_TIMEOUT = 1000;
    private static final int MIN_LEN = 256;
    private DeviceTransport mSerialDevice;

    // Storing this to reduce the number of reads from the device for other attributes
    private Document manufacturingDataXml;

    public ReadData(DeviceTransport device) {
        mSerialDevice = device;
    }

    public List<EGVRecord> getRecentEGVs() throws IOException {
        int endPage = readDataBasePageRange(RecordType.EGV_DATA);
        byte[] data = readDataBasePage(RecordType.EGV_DATA, endPage);
        return parsePage(data, EGVRecord.class);
    }

    public List<EGVRecord> getRecentEGVsPages(int numOfRecentPages) throws IOException {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        log.debug("Reading EGV page range...");
        int endPage = readDataBasePageRange(RecordType.EGV_DATA);
        log.debug("Reading {} EGV page(s)...", numOfRecentPages);
        numOfRecentPages = numOfRecentPages - 1;
        List<EGVRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} EGV pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(RecordType.EGV_DATA, nextPage);
            allPages.addAll(parsePage(data, EGVRecord.class));
        }
        log.debug("Read complete of EGV pages.");
        return allPages;
    }

    public long getTimeSinceEGVRecord(EGVRecord egvRecord) throws IOException {
        return readSystemTime() - egvRecord.getRawSystemTimeSeconds();
    }

    public List<MeterRecord> getRecentMeterRecords() throws IOException {
        log.debug("Reading Meter page...");
        int endPage = readDataBasePageRange(RecordType.METER_DATA);
        byte[] data = readDataBasePage(RecordType.METER_DATA, endPage);
        return parsePage(data, MeterRecord.class);
    }

    public List<SensorRecord> getRecentSensorRecords(int numOfRecentPages) throws IOException {
        if (numOfRecentPages < 1) {
            throw new IllegalArgumentException("Number of pages must be greater than 1.");
        }
        log.debug("Reading Sensor page range...");
        int endPage = readDataBasePageRange(RecordType.SENSOR_DATA);
        log.debug("Reading {} Sensor page(s)...", numOfRecentPages);
        numOfRecentPages = numOfRecentPages - 1;
        List<SensorRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} Sensor pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(RecordType.SENSOR_DATA, nextPage);
            allPages.addAll(parsePage(data, SensorRecord.class));
        }
        log.debug("Read complete of Sensor pages.");
        return allPages;
    }

    public List<CalRecord> getRecentCalRecords() throws IOException {
        log.debug("Reading Cal Records page range...");
        int endPage = readDataBasePageRange(RecordType.CAL_SET);
        log.debug("Reading Cal Records page...");
        byte[] data = readDataBasePage(RecordType.CAL_SET, endPage);
        return parsePage(data, CalRecord.class);
    }

    public boolean ping() throws IOException {
        writeCommand(Command.PING);
        return read(MIN_LEN).getCommand() == Command.ACK;
    }

    public int readBatteryLevel() throws IOException {
        log.debug("Reading battery level...");
        writeCommand(Command.READ_BATTERY_LEVEL);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() throws IOException {
        return getManufacturingAttribute("SerialNumber").or("");
    }

    private Optional<String> getManufacturingAttribute(String attribute) throws IOException {
        String result = null;
        if (manufacturingDataXml == null) {
            byte[] packet = readDataBasePage(RecordType.MANUFACTURING_DATA, 0);
            String xml = new String(Arrays.copyOfRange(packet, 8, 241));
            // TODO: it would be best if we could just remove /x00 characters and read till end
            try {

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                manufacturingDataXml = builder.parse(new InputSource(new StringReader(xml)));
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException("Unable to parse manufacturing data", e);
            }
        } else {
            Element element = manufacturingDataXml.getDocumentElement();
            result = element.getAttribute(attribute);
        }
        return Optional.fromNullable(result);
    }

    public Date readDisplayTime() throws IOException {
        return Utils.receiverTimeToDate(readSystemTime() + readDisplayTimeOffset());
    }

    public long readSystemTime() throws IOException {
        log.debug("Reading system time...");
        writeCommand(Command.READ_SYSTEM_TIME);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int readDisplayTimeOffset() throws IOException {
        log.debug("Reading display time offset...");
        writeCommand(Command.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    private int readDataBasePageRange(RecordType recordType) throws IOException {
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) recordType.ordinal());
        writeCommand(Command.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(MIN_LEN).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
    }

    private byte[] readDataBasePage(RecordType recordType, int page) throws IOException {
        byte numOfPages = 1;
        if (page < 0) {
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
        return read(2122).getData();
    }

    private void writeCommand(Command command, ArrayList<Byte> payload) throws IOException {
        byte[] packet = new PacketBuilder(command, payload).build();
        if (mSerialDevice != null) {
            mSerialDevice.write(packet, IO_TIMEOUT);
        }
    }

    protected void writeCommand(Command command) throws IOException {
        byte[] packet = new PacketBuilder(command).build();
        if (mSerialDevice != null) {
            mSerialDevice.write(packet, IO_TIMEOUT);
        }
    }

    private ReadPacket read(int numOfBytes) throws IOException {
        byte[] response = new byte[numOfBytes];
        int len;
        try {
            response = mSerialDevice.read(numOfBytes, IO_TIMEOUT);
            len = response.length;
            log.debug("Read {} byte(s) complete.", len);

            // Add a 100ms delay for when multiple write/reads are occurring in series
            Thread.sleep(100);

            // TODO: this debug code to print data of the read, should be removed after
            // finding the source of the reading issue
            String bytes = "";
            int readAmount = len;
            for (int i = 0; i < readAmount; i++) bytes += String.format("%02x", response[i]) + " ";
            log.debug("Read data: {}", bytes);
            ////////////////////////////////////////////////////////////////////////////////////////
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new ReadPacket(response);
    }

    private <T extends GenericTimestampRecord> List<T> parsePage(byte[] data, Class<T> clazz) {
        PageHeader pageHeader = new PageHeader(data);
        List<T> records = new ArrayList<>();
        try {
            for (int i = 0; i < pageHeader.getNumOfRecords(); i++) {
                int startIdx;
                switch (pageHeader.getRecordType()) {
                    case EGV_DATA:
                        startIdx = PageHeader.HEADER_SIZE + (EGVRecord.RECORD_SIZE + 1) * i;
                        records.add(clazz.cast(new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + EGVRecord.RECORD_SIZE))));
                        break;
                    case CAL_SET:
                        int recordLength = (pageHeader.getRevision() <= 2) ? CalRecord.RECORD_SIZE : CalRecord.RECORD_V2_SIZE;
                        startIdx = PageHeader.HEADER_SIZE + (recordLength + 1) * i;
                        records.add(clazz.cast(new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + recordLength))));
                        break;
                    case METER_DATA:
                        startIdx = PageHeader.HEADER_SIZE + (MeterRecord.RECORD_SIZE + 1) * i;
                        records.add(clazz.cast(new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + MeterRecord.RECORD_SIZE))));
                        break;
                    case SENSOR_DATA:
                        startIdx = PageHeader.HEADER_SIZE + (SensorRecord.RECORD_SIZE + 1) * i;
                        records.add(clazz.cast(new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + SensorRecord.RECORD_SIZE))));
                        break;
                    default:
                        throw new IllegalArgumentException(String.format("Unknown record type: %s", pageHeader.getRecordType().name()));
                }
            }
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        return records;
    }
}