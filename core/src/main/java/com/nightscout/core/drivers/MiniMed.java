package com.nightscout.core.drivers;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.Medtronic.MinimedClient;
import com.nightscout.core.drivers.Medtronic.Page;
import com.nightscout.core.drivers.Medtronic.PumpModel;
import com.nightscout.core.drivers.Medtronic.remote_commands.CommandBase;
import com.nightscout.core.drivers.Medtronic.remote_commands.PowerOnCommand;
import com.nightscout.core.drivers.Medtronic.remote_commands.ReadHistoryDataCommand;
import com.nightscout.core.drivers.Medtronic.request.ProductInfoRequest;
import com.nightscout.core.drivers.Medtronic.request.RadioInterfaceStatsRequest;
import com.nightscout.core.drivers.Medtronic.request.ReadRadioRequest;
import com.nightscout.core.drivers.Medtronic.request.SignalStrengthRequest;
import com.nightscout.core.drivers.Medtronic.request.UsbInterfaceStatsRequest;
import com.nightscout.core.drivers.Medtronic.response.InterfaceStatsResponse;
import com.nightscout.core.drivers.Medtronic.response.ProductInfoResponse;
import com.nightscout.core.drivers.Medtronic.response.ReadRadioResponse;
import com.nightscout.core.drivers.Medtronic.response.SignalStrengthResponse;
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
            MinimedClient client = new MinimedClient(serialDriver);
            ProductInfoRequest productInfoRequest = new ProductInfoRequest();
            ProductInfoResponse productInfo = client.execute(productInfoRequest, ProductInfoResponse.class);
            log.info("Serial number: {}", productInfo.getSerialNumber());

            UsbInterfaceStatsRequest usbStatsRequest = new UsbInterfaceStatsRequest();
            InterfaceStatsResponse statsResponse = client.execute(usbStatsRequest, InterfaceStatsResponse.class);
            log.info("USB CRC Errors: {}", statsResponse.getCrcErrors());
            log.info("USB Sequence Errors: {}", statsResponse.getSeqErrors());
            log.info("USB NAK Errors: {}", statsResponse.getNakErrors());
            log.info("USB Timeouts: {}", statsResponse.getTimeouts());
            log.info("USB Transmitted: {}", statsResponse.getTransmitted());
            log.info("USB Received: {}", statsResponse.getReceived());

            RadioInterfaceStatsRequest radioStatsRequest = new RadioInterfaceStatsRequest();
            statsResponse = client.execute(radioStatsRequest, InterfaceStatsResponse.class);
            log.info("Radio CRC Errors: {}", statsResponse.getCrcErrors());
            log.info("Radio Sequence Errors: {}", statsResponse.getSeqErrors());
            log.info("Radio NAK Errors: {}", statsResponse.getNakErrors());
            log.info("Radio Timeouts: {}", statsResponse.getTimeouts());
            log.info("Radio Transmitted: {}", statsResponse.getTransmitted());
            log.info("Radio Received: {}", statsResponse.getReceived());

            SignalStrengthRequest signalStrengthRequest = new SignalStrengthRequest();
            SignalStrengthResponse signalStrengthResponse = client.execute(signalStrengthRequest, SignalStrengthResponse.class);
            log.info("Signal Strength: {}", signalStrengthResponse.getStrength());

            byte[] serial = convertSerialBytes(preferences.getMedtronicSerial());
            log.info("Using Serial {}", Utils.bytesToHex(serial));
            CommandBase command = new PowerOnCommand(serial);
            ReadRadioResponse r = client.execute(command);

            command = new ReadHistoryDataCommand(serial, (byte) 0x00);
            r = client.execute(command);

            if (r != null) {
                log.info("Command Response: {}", Utils.bytesToHex(r.getData()));
                log.info("Command Response size: {}", r.getData().length);
                Page page = new Page(r.getData(), PumpModel.MM523);
            } else {
                log.warn("Command failed");
            }

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

    public byte[] convertSerialBytes (String serialnum) {
        int len = serialnum.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(serialnum.charAt(i), 16) << 4)
                    + Character.digit(serialnum.charAt(i + 1), 16));
        }
        return data;
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
