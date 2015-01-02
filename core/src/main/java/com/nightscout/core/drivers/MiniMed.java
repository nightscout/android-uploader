package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.Medtronic.InterfaceStatsResponse;
import com.nightscout.core.drivers.Medtronic.LinkStatusResponse;
import com.nightscout.core.drivers.Medtronic.OpCodes;
import com.nightscout.core.drivers.Medtronic.PowerOnCommand;
import com.nightscout.core.drivers.Medtronic.ProductInfoResponse;
import com.nightscout.core.drivers.Medtronic.ReadPumpModelCommand;
import com.nightscout.core.drivers.Medtronic.ReadRadioRequest;
import com.nightscout.core.drivers.Medtronic.ReadRadioResponse;
import com.nightscout.core.drivers.Medtronic.SignalStrengthResponse;
import com.nightscout.core.drivers.Medtronic.TransmitPacketRequest;
import com.nightscout.core.model.DownloadResults;
import com.nightscout.core.preferences.NightscoutPreferences;

import java.io.IOException;

public class MiniMed extends AbstractDevice {
    public static final Integer VENDOR_ID = 2593;
    public static Integer PRODUCT_ID = 32769;

    public MiniMed(DeviceTransport serialDriver, NightscoutPreferences preferences, AbstractUploaderDevice uploaderDevice) {
        super(serialDriver);
        try {
            serialDriver.open();
            serialDriver.write(OpCodes.PRODUCT_INFO, 0);
            byte[] response = serialDriver.read(0, 0);
            log.info("Response: {}", Utils.bytesToHex(response));
            ProductInfoResponse pi = new ProductInfoResponse(response);
            serialDriver.write(OpCodes.USB_STATS, 0);
            response = serialDriver.read(0, 0);
            InterfaceStatsResponse usbInterfaceStats = new InterfaceStatsResponse(response);

            log.info("USB CRC Errors: {}", usbInterfaceStats.getCrcErrors());
            log.info("USB Sequence Errors: {}", usbInterfaceStats.getSeqErrors());
            log.info("USB NAK Errors: {}", usbInterfaceStats.getNakErrors());
            log.info("USB Timeouts: {}", usbInterfaceStats.getTimeouts());
            log.info("USB Transmitted: {}", usbInterfaceStats.getTransmitted());
            log.info("USB Received: {}", usbInterfaceStats.getReceived());

            serialDriver.write(OpCodes.RADIO_STATS, 0);
            response = serialDriver.read(0, 0);
            InterfaceStatsResponse radioInterfaceStats = new InterfaceStatsResponse(response);

            log.info("Radio CRC Errors: {}", radioInterfaceStats.getCrcErrors());
            log.info("Radio Sequence Errors: {}", radioInterfaceStats.getSeqErrors());
            log.info("Radio NAK Errors: {}", radioInterfaceStats.getNakErrors());
            log.info("Radio Timeouts: {}", radioInterfaceStats.getTimeouts());
            log.info("Radio Transmitted: {}", radioInterfaceStats.getTransmitted());
            log.info("Radio Received: {}", radioInterfaceStats.getReceived());

            serialDriver.write(OpCodes.SIGNAL_STRENGTH, 0);
            response = serialDriver.read(0, 0);
            SignalStrengthResponse signalStrength = new SignalStrengthResponse(response);

            log.info("Signal strength: {}", signalStrength.getStrength());

            serialDriver.write(OpCodes.LINK_STATUS, 0);
            response = serialDriver.read(0, 0);
            LinkStatusResponse linkStatus = new LinkStatusResponse(response);

            log.info("Link Size: {}", linkStatus.getSize());

//            byte[] bensTestSerial = new byte[]{0x66, 0x54, 0x55};
            byte[] serial = new byte[]{(byte) 0x86, (byte) 0x80, 0x42};
            TransmitPacketRequest transmitRequest = new TransmitPacketRequest(serial, new PowerOnCommand(), (byte) 0);
            log.info("Transmit packet: {}", Utils.bytesToHex(transmitRequest.getPacket()));

            serialDriver.write(transmitRequest.getPacket(), 0);
            sleep(30000L);
            log.info("Response: ", Utils.bytesToHex(serialDriver.read(0, 0)));
            ReadPumpModelCommand readPump = new ReadPumpModelCommand();
            transmitRequest = new TransmitPacketRequest(serial, readPump, (byte) 3);
            serialDriver.write(transmitRequest.getPacket(), 0);
            response = serialDriver.read(0, 0);
            log.info("Response: {}", Utils.bytesToHex(response));
            sleep(1000);
            LinkStatusResponse resp = null;
            boolean success = false;
            for (int i = 0; i < 30; i++) {
                serialDriver.write(OpCodes.LINK_STATUS, 0);
                response = serialDriver.read(0, 0);
                resp = new LinkStatusResponse(response);
                if (resp.getSize() > 15) {
                    log.info("Stick has data waiting {}", resp.getSize());
                    success = true;
                    break;
                }
                log.info("Response: {}", Utils.bytesToHex(response));
                log.info("Response size: {}", resp.getSize());
                sleep(2000L);
            }
            if (success) {
                ReadRadioRequest req = new ReadRadioRequest(resp.getSize());
                ReadRadioResponse readResp = null;
                try {
                    for (int i = 0; i < 5; i++) {
                        log.info("Retry #{}", i);
                        log.info("Writing: {}", Utils.bytesToHex(req.getPacket()));
                        serialDriver.write(req.getPacket(), 0);
                        sleep(2000);
                        response = serialDriver.read(0, 0);
                        log.info("My data: {}", Utils.bytesToHex(response));
                        readResp = new ReadRadioResponse(response);
                        if (readResp.getData().length == 0) {
                            sleep(250);
                            readResp = new ReadRadioResponse(serialDriver.read(0, 0));
                        }
                        log.info("ReadRadio Response: {}", new String(readResp.getData()));
                        log.info("ReadRadio Response size: {}", resp.getSize());
                        if (readResp.getResultLength() == resp.getSize() + 14) {
                            log.info("SUCCESS!");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (resp == null) {
                    log.info("Returning null =(");
                }
//                ReadRadioResponse radioResp = downloadPacket(resp.getSize(), serialDriver);
//                log.info("Radio response: {}", radioResp.getData());
            }
            serialDriver.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sleep(long time) {
        try {
            log.info("Sleeping for {} seconds", time / 1000);
            Thread.sleep(time);
            log.info("Finished sleeping");
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    protected DownloadResults doDownload() {
        return null;
    }

    // Default to 5 retries
    public ReadRadioResponse downloadPacket(short size, DeviceTransport serialDriver) {
        return downloadPacket(size, serialDriver, 5);
    }

    public ReadRadioResponse downloadPacket(short size, DeviceTransport serialDriver, int retries) {
        ReadRadioRequest req = new ReadRadioRequest(size);
        ReadRadioResponse resp = null;
        try {
            for (int i = 0; i < retries; i++) {
                log.info("Retry #{}", i);
                log.info("Writing: {}", Utils.bytesToHex(req.getPacket()));
                serialDriver.write(req.getPacket(), 0);
                sleep(2000);
                byte[] response = serialDriver.read(0, 0);
                log.info("My data: {}", Utils.bytesToHex(response));
                resp = new ReadRadioResponse(response);
                if (resp.getData().length == 0) {
                    sleep(250);
                    resp = new ReadRadioResponse(serialDriver.read(0, 0));
                }
                if (resp.getData().length == size) {
                    log.info("SUCCESS!");
                    return resp;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (resp == null) {
            log.info("Returning null =(");
        }
        return resp;
    }
}
