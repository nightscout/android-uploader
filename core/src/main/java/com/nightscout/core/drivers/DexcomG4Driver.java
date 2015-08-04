package com.nightscout.core.drivers;

import com.nightscout.core.Timestamped;
import com.nightscout.core.TimestampedInstances;
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
import com.nightscout.core.model.v2.Calibration;
import com.nightscout.core.model.v2.Insertion;
import com.nightscout.core.model.v2.ManualMeterEntry;
import com.nightscout.core.model.v2.RawSensorReading;
import com.nightscout.core.model.v2.SensorGlucoseValue;

import net.tribe7.common.base.Optional;
import net.tribe7.common.collect.Lists;
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
import java.sql.Time;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

public final class DexcomG4Driver {
    private static final Logger log = LoggerFactory.getLogger(DexcomG4Driver.class);

    public static final int EXPECTED_PAGE_SIZE_BYTES = 534;
    public static final int EXPECTED_PAGE_RANGE_SIZE_BYTES = 14;

    private static final int TIMEOUT_MS = 25000;
    private static final int MIN_LEN = 256;

    // Storing this to reduce the number of reads from the device for other attributes
    private static Document manufacturingDataXml;
    private static String transmitterId;

    private DexcomG4Driver() {}

    public static List<Insertion> getRecentInsertions(DeviceTransport deviceTransport) throws IOException {
        return Lists.transform(getRecentInsertion(deviceTransport), InsertionRecord.v2ModelConverter());
    }
    private static List<InsertionRecord> getRecentInsertion(DeviceTransport deviceTransport) throws IOException {
        int endPage = readDataBasePageRange(deviceTransport, RecordType.INSERTION_TIME);
        byte[] data = readDataBasePage(deviceTransport, RecordType.INSERTION_TIME, endPage);
        return parsePage(data, InsertionRecord.class);
    }

    public static List<EGVRecord> getRecentEGVsPages(DeviceTransport deviceTransport, int numOfRecentPages) throws IOException {
        if (numOfRecentPages < 1) {
            log.warn("numOfRecentPages less than 1. Setting to 1.");
            numOfRecentPages = 1;
        }
        int endPage = readDataBasePageRange(deviceTransport, RecordType.EGV_DATA);
        numOfRecentPages = numOfRecentPages - 1;
        List<EGVRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} EGV pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(deviceTransport, RecordType.EGV_DATA, nextPage);
            allPages.addAll(parsePage(data, EGVRecord.class));
        }
        return allPages;
    }

    public static List<SensorGlucoseValue> getAllSensorGlucoseValuesAfter(DeviceTransport deviceTransport, Optional<Timestamped> timestampedOptional) throws IOException {
        return Lists.transform(getAllEGVRecordsAfter(deviceTransport, timestampedOptional), EGVRecord.v2ModelConverter());
    }

    private static List<EGVRecord> getAllEGVRecordsAfter(DeviceTransport deviceTransport, Optional<Timestamped> timestamped) throws IOException {
        Timestamped alreadyDownloadedEntry;
        if (timestamped.isPresent()) {
            alreadyDownloadedEntry = timestamped.get();
        } else {
            alreadyDownloadedEntry = TimestampedInstances.epoch();
        }
        int endPage = readDataBasePageRange(deviceTransport, RecordType.EGV_DATA);
        List<EGVRecord> allPages = new ArrayList<>();
        int currentPage = 0;
        Timestamped earliestDownloaded = TimestampedInstances.now();

        while(earliestDownloaded.compareTo(alreadyDownloadedEntry) > 0 && currentPage <= endPage) {
            byte[] data = readDataBasePage(deviceTransport, RecordType.EGV_DATA, currentPage);
            List<EGVRecord> records = parsePage(data, EGVRecord.class);
            List<SensorGlucoseValue> sensorGlucoseValues = Lists.transform(records, EGVRecord.v2ModelConverter());
            if (sensorGlucoseValues.size() > 0) {
                earliestDownloaded = TimestampedInstances.fromG4Timestamp(sensorGlucoseValues.get(0).timestamp);
            }
            allPages.addAll(records);
            currentPage++;
        }
        return allPages;
    }

    public static List<ManualMeterEntry> getRecentManualMeterEntries(DeviceTransport deviceTransport) throws IOException {
        return Lists.transform(getRecentMeterRecords(deviceTransport), MeterRecord.v2ModelConverter());
    }
    private static List<MeterRecord> getRecentMeterRecords(DeviceTransport deviceTransport) throws IOException {
        int endPage = readDataBasePageRange(deviceTransport, RecordType.METER_DATA);
        byte[] data = readDataBasePage(deviceTransport, RecordType.METER_DATA, endPage);
        return parsePage(data, MeterRecord.class);
    }

    public static List<RawSensorReading> getRecentRawSensorReadings(DeviceTransport deviceTransport, int numOfRecentPages) throws IOException {
        return Lists.transform(getRecentSensorRecords(deviceTransport, numOfRecentPages),
                               SensorRecord.v2ModelConverter());
    }
    private static List<SensorRecord> getRecentSensorRecords(DeviceTransport deviceTransport, int numOfRecentPages) throws IOException {
        if (numOfRecentPages < 1) {
            log.warn("Number of recent pages requested less than 1. Setting to 1.");
            numOfRecentPages = 1;
        }
        int endPage = readDataBasePageRange(deviceTransport, RecordType.SENSOR_DATA);
        numOfRecentPages = numOfRecentPages - 1;
        List<SensorRecord> allPages = new ArrayList<>();
        for (int i = Math.min(numOfRecentPages, endPage); i >= 0; i--) {
            int nextPage = endPage - i;
            log.debug("Reading #{} Sensor pages (page number {})", i, nextPage);
            byte[] data = readDataBasePage(deviceTransport, RecordType.SENSOR_DATA, nextPage);
            allPages.addAll(parsePage(data, SensorRecord.class));
        }
        return allPages;
    }

    public static List<Calibration> getRecentCalibrations(DeviceTransport deviceTransport) throws IOException {
        return Lists.transform(getRecentCalRecords(deviceTransport), CalRecord.v2ModelConverter());
    }
    private static List<CalRecord> getRecentCalRecords(DeviceTransport deviceTransport) throws IOException {
        int endPage = readDataBasePageRange(deviceTransport, RecordType.CAL_SET);
        byte[] data = readDataBasePage(deviceTransport, RecordType.CAL_SET, endPage);
        return parsePage(data, CalRecord.class);
    }

    public static boolean ping(DeviceTransport deviceTransport) throws IOException {
        writeCommand(deviceTransport, Command.PING);
        return read(deviceTransport, MIN_LEN).getCommand() == Command.ACK;
    }

    public static int readBatteryLevel(DeviceTransport deviceTransport) throws IOException {
        log.debug("Reading battery level...");
        writeCommand(deviceTransport, Command.READ_BATTERY_LEVEL);
        byte[] readData = read(deviceTransport, 10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static String readSerialNumber(DeviceTransport deviceTransport) throws IOException {
        return getManufacturingAttribute(deviceTransport, "SerialNumber").or("");
    }

    public static String readTrasmitterId(DeviceTransport deviceTransport) throws IOException {
        if (transmitterId == null) {
            writeCommand(deviceTransport, Command.READ_TRANSMITTER_ID);
            byte[] data = read(deviceTransport, 11).getData();
            transmitterId = new String(data);
        }
        return transmitterId;
    }

    private static Optional<String> getManufacturingAttribute(DeviceTransport deviceTransport, String attribute) throws IOException {
        String result;
        if (manufacturingDataXml == null) {
            byte[] packet = readDataBasePage(deviceTransport, RecordType.MANUFACTURING_DATA, 0);
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

    public static long readSystemTime(DeviceTransport deviceTransport) throws IOException {
        writeCommand(deviceTransport, Command.READ_SYSTEM_TIME);
        byte[] readData = read(deviceTransport, 10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    public static int readDisplayTimeOffset(DeviceTransport deviceTransport) throws IOException {
        log.debug("Reading display time offset...");
        writeCommand(deviceTransport, Command.READ_DISPLAY_TIME_OFFSET);
        byte[] readData = read(deviceTransport, 10).getData();
        return ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN).getInt();
    }

    /**
     * Reads the entire page range for the given record type.
     * @param recordType Record type to read the page range for.
     * @return The last page number available.
     * @throws IOException
     */
    private static int readDataBasePageRange(DeviceTransport deviceTransport, RecordType recordType) throws IOException {
        ArrayList<Byte> payload = new ArrayList<>();
        payload.add((byte) recordType.ordinal());
        writeCommand(deviceTransport, Command.READ_DATABASE_PAGE_RANGE, payload);
        byte[] readData = read(deviceTransport, EXPECTED_PAGE_RANGE_SIZE_BYTES).getData();
        ByteBuffer buffer = ByteBuffer.wrap(readData).order(ByteOrder.LITTLE_ENDIAN);
        log.debug("Reading page range for {}: {}", recordType.name(),
                  Utils.bytesToHex(buffer.array()));
        return buffer.getInt(4);
    }

    private static byte[] readDataBasePage(DeviceTransport deviceTransport, RecordType recordType, int page) throws IOException {
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
        writeCommand(deviceTransport, Command.READ_DATABASE_PAGES, payload);
        return read(deviceTransport, EXPECTED_PAGE_SIZE_BYTES).getData();
    }

    private static void writeCommand(DeviceTransport deviceTransport, Command command, List<Byte> payload) throws IOException {
        String payloadString = "";
        if (payload != null) {
            payloadString = Utils.bytesToHex(Bytes.toArray(payload));
        }
        log.debug("Writing command {} with payload '{}'.", command.name(), payloadString);
        
        deviceTransport.write(new PacketBuilder(command, payload).build(), TIMEOUT_MS);
    }

    protected static void writeCommand(DeviceTransport deviceTransport, Command command) throws IOException {
        writeCommand(deviceTransport, command, null);
    }

    private static ReadPacket read(DeviceTransport deviceTransport, int numOfBytes) throws IOException {
        byte[] response = deviceTransport.read(numOfBytes, TIMEOUT_MS);
        if (response.length != numOfBytes) {
            log.error("Response numBytes {}, requested {}", response.length, numOfBytes);
        }
        log.debug("Read {} byte(s).", response.length);
        return new ReadPacket(response);
    }

    private static <T extends GenericTimestampRecord> List<T> parsePage(byte[] data, Class<T> clazz) {
        PageHeader pageHeader = new PageHeader(data);
        List<T> records = new ArrayList<>();
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
                case INSERTION_TIME:
                    startIdx = PageHeader.HEADER_SIZE + (InsertionRecord.RECORD_SIZE + 1) * i;
                    records.add(clazz.cast(new InsertionRecord(Arrays.copyOfRange(data, startIdx, startIdx + InsertionRecord.RECORD_SIZE))));
                    break;
                default:
                    log.error("Unknown record type {} encountered. Ignoring.", pageHeader.getRecordType().name());
                    break;
            }
        }
        return records;
    }
}