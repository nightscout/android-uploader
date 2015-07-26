package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.Command;
import com.nightscout.core.dexcom.PacketBuilder;
import com.nightscout.core.dexcom.ReadPacket;
import com.nightscout.core.dexcom.RecordType;
import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.dexcom.records.CalRecord;
import com.nightscout.core.dexcom.records.EGVRecord;
import com.nightscout.core.dexcom.records.GenericTimestampRecord;
import com.nightscout.core.dexcom.records.InsertionRecord;
import com.nightscout.core.dexcom.records.MeterRecord;
import com.nightscout.core.dexcom.records.PageHeader;
import com.nightscout.core.dexcom.records.SensorRecord;

import net.tribe7.common.base.Optional;
import net.tribe7.common.primitives.Bytes;

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
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public class ReadData {

    public static final int EXPECTED_PAGE_SIZE_BYTES = 534;
    public static final int EXPECTED_PAGE_RANGE_SIZE_BYTES = 14;

    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    private static final int TIMEOUT_MS = 25000;
    private static final int MIN_LEN = 256;
    private DeviceTransport mSerialDevice;

    // Storing this to reduce the number of reads from the device for other attributes
    private Document manufacturingDataXml;

    public ReadData(DeviceTransport device) {
        mSerialDevice = device;
    }

    public List<InsertionRecord> getRecentInsertion(long rcvrTime, long refTime) throws IOException {
        int endPage = readDataBasePageRange(RecordType.INSERTION_TIME);
        byte[] data = readDataBasePage(RecordType.INSERTION_TIME, endPage);
        return parsePage(data, InsertionRecord.class, rcvrTime, refTime);
    }

    public List<EGVRecord> getRecentEGVsPages(int numOfRecentPages, long rcvrTime, long refTime) throws IOException {
        if (numOfRecentPages < 1) {
            log.warn("numOfRecentPages less than 1. Setting to 1.");
            numOfRecentPages = 1;
        }
        int endPage = readDataBasePageRange(RecordType.EGV_DATA);
        numOfRecentPages = numOfRecentPages - 1;
        List<EGVRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} EGV pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(RecordType.EGV_DATA, nextPage);
            allPages.addAll(parsePage(data, EGVRecord.class, rcvrTime, refTime));
        }
        return allPages;
    }

    public List<MeterRecord> getRecentMeterRecords(long rcvrTime, long refTime) throws IOException {
        int endPage = readDataBasePageRange(RecordType.METER_DATA);
        byte[] data = readDataBasePage(RecordType.METER_DATA, endPage);
        return parsePage(data, MeterRecord.class, rcvrTime, refTime);
    }

    public List<SensorRecord> getRecentSensorRecords(int numOfRecentPages, long rcvrTime, long refTime) throws IOException {
        if (numOfRecentPages < 1) {
            log.warn("Number of recent pages requested less than 1. Setting to 1.");
            numOfRecentPages = 1;
        }
        int endPage = readDataBasePageRange(RecordType.SENSOR_DATA);
        numOfRecentPages = numOfRecentPages - 1;
        List<SensorRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} Sensor pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(RecordType.SENSOR_DATA, nextPage);
            allPages.addAll(parsePage(data, SensorRecord.class, rcvrTime, refTime));
        }
        return allPages;
    }

    public List<CalRecord> getRecentCalRecords(long rcvrTime, long refTime) throws IOException {
        int endPage = readDataBasePageRange(RecordType.CAL_SET);
        byte[] data = readDataBasePage(RecordType.CAL_SET, endPage);
        return parsePage(data, CalRecord.class, rcvrTime, refTime);
    }

    public boolean ping() throws IOException {
        writeCommand(Command.PING);
        return read(MIN_LEN).getCommand() == Command.ACK;
    }

    public int readBatteryLevel() throws IOException {
        log.debug("Reading battery level...");
        writeCommand(Command.READ_BATTERY_LEVEL);
        byte[] readData = read(10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public String readSerialNumber() throws IOException {
        return getManufacturingAttribute("SerialNumber").or("");
    }

    public String readTrasmitterId() throws IOException {
        writeCommand(Command.READ_TRANSMITTER_ID);
        byte[] data = read(11).getData();
        return new String(data);
    }

    private Optional<String> getManufacturingAttribute(String attribute) throws IOException {
        String result;
        if (manufacturingDataXml == null) {
            byte[] packet = readDataBasePage(RecordType.MANUFACTURING_DATA, 0);
            String raw = new String(packet);
            String xml = raw.substring(raw.indexOf('<'), raw.lastIndexOf('>') + 1);
            try {
                log.debug("Manufacturing Response size: {}, data: {}", packet.length, xml);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                manufacturingDataXml = builder.parse(new InputSource(new StringReader(xml)));
                Element element = manufacturingDataXml.getDocumentElement();
                result = element.getAttribute(attribute);
            } catch (ParserConfigurationException | SAXException e) {
                throw new IOException("Unable to parse manufacturing data", e);
            }
        } else {
            Element element = manufacturingDataXml.getDocumentElement();
            result = element.getAttribute(attribute);
        }
        return Optional.fromNullable(result);
    }

    public long readSystemTime() throws IOException {
        writeCommand(Command.READ_SYSTEM_TIME);
        byte[] readData = read(10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public int readDisplayTimeOffset() throws IOException {
        log.debug("Reading display time offset...");
        writeCommand(Command.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads the entire page range for the given record type.
     * @param recordType Record type to read the page range for.
     * @return The last page number available.
     * @throws IOException
     */
    private int readDataBasePageRange(RecordType recordType) throws IOException {
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) recordType.ordinal());
        writeCommand(Command.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(EXPECTED_PAGE_RANGE_SIZE_BYTES).getData();
        ByteBuffer buffer = ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN);
        log.debug("Reading page range for {}: {}", recordType.name(),
                  Utils.bytesToHex(buffer.array()));
        return buffer.getInt(4);
    }

    private byte[] readDataBasePage(RecordType recordType, int page) throws IOException {
        int numOfPages = 1;
        if (page < 0) {
            page = 0;
            log.warn("Requested invalid page {}. Setting to 0.", page);
        }
        log.debug("Reading {} pages, starting at page {} for record type {}.", numOfPages, page, recordType.name());
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) recordType.ordinal());
        byte[] pageInt = ByteBuffer.allocate(4).putInt(page).array();
        payload.add(pageInt[3]);
        payload.add(pageInt[2]);
        payload.add(pageInt[1]);
        payload.add(pageInt[0]);
        payload.add(((byte) numOfPages));
        writeCommand(Command.READ_DATABASE_PAGES, payload);
        return read(EXPECTED_PAGE_SIZE_BYTES).getData();
    }

    private void writeCommand(Command command, List<Byte> payload) throws IOException {
        String payloadString = "";
        if (payload != null) {
            payloadString = Utils.bytesToHex(Bytes.toArray(payload));
        }
        log.debug("Writing command {} with payload '{}'.", command.name(), payloadString);
        mSerialDevice.write(new PacketBuilder(command, payload).build(), TIMEOUT_MS);
    }

    protected void writeCommand(Command command) throws IOException {
        writeCommand(command, null);
    }

    private ReadPacket read(int numOfBytes) throws IOException {
        byte[] response = mSerialDevice.read(numOfBytes, TIMEOUT_MS);
        if (response.length != numOfBytes) {
            log.error("Response numBytes {}, requested {}", response.length, numOfBytes);
        }
        log.debug("Read {} byte(s).", response.length);
        return new ReadPacket(response);
    }

    private <T extends GenericTimestampRecord> List<T> parsePage(byte[] data, Class<T> clazz, long rcvrTime, long refTime) {
        PageHeader pageHeader = new PageHeader(data);
        List<T> records = new ArrayList<>();
        for (int i = 0; i < pageHeader.getNumOfRecords(); i++) {
            int startIdx;
            switch (pageHeader.getRecordType()) {
                case EGV_DATA:
                    startIdx = PageHeader.HEADER_SIZE + (EGVRecord.RECORD_SIZE + 1) * i;
                    records.add(clazz.cast(new EGVRecord(Arrays.copyOfRange(data, startIdx, startIdx + EGVRecord.RECORD_SIZE), rcvrTime, refTime)));
                    break;
                case CAL_SET:
                    int recordLength = (pageHeader.getRevision() <= 2) ? CalRecord.RECORD_SIZE : CalRecord.RECORD_V2_SIZE;
                    startIdx = PageHeader.HEADER_SIZE + (recordLength + 1) * i;
                    records.add(clazz.cast(new CalRecord(Arrays.copyOfRange(data, startIdx, startIdx + recordLength), rcvrTime, refTime)));
                    break;
                case METER_DATA:
                    startIdx = PageHeader.HEADER_SIZE + (MeterRecord.RECORD_SIZE + 1) * i;
                    records.add(clazz.cast(new MeterRecord(Arrays.copyOfRange(data, startIdx, startIdx + MeterRecord.RECORD_SIZE), rcvrTime, refTime)));
                    break;
                case SENSOR_DATA:
                    startIdx = PageHeader.HEADER_SIZE + (SensorRecord.RECORD_SIZE + 1) * i;
                    records.add(clazz.cast(new SensorRecord(Arrays.copyOfRange(data, startIdx, startIdx + SensorRecord.RECORD_SIZE), rcvrTime, refTime)));
                    break;
                case INSERTION_TIME:
                    startIdx = PageHeader.HEADER_SIZE + (InsertionRecord.RECORD_SIZE + 1) * i;
                    records.add(clazz.cast(new InsertionRecord(Arrays.copyOfRange(data, startIdx, startIdx + InsertionRecord.RECORD_SIZE), rcvrTime, refTime)));
                    break;
                default:
                    log.error("Unknown record type {} encountered. Ignoring.", pageHeader.getRecordType().name());
                    break;
            }
        }
        return records;
    }
}