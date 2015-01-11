package com.nightscout.core.drivers.Medtronic;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.DeviceTransport;
import com.nightscout.core.drivers.Medtronic.remote_commands.CommandBase;
import com.nightscout.core.drivers.Medtronic.request.LinkStatusRequest;
import com.nightscout.core.drivers.Medtronic.request.ReadRadioRequest;
import com.nightscout.core.drivers.Medtronic.request.RequestBase;
import com.nightscout.core.drivers.Medtronic.request.TransmitPacketResponse;
import com.nightscout.core.drivers.Medtronic.response.LinkStatusResponse;
import com.nightscout.core.drivers.Medtronic.response.ReadRadioResponse;
import com.nightscout.core.drivers.Medtronic.response.ResponseBase;
import com.nightscout.core.drivers.Medtronic.response.TransmitPacketRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class MinimedClient {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());
    DeviceTransport transport;

    public MinimedClient(DeviceTransport transport) {
        this.transport = transport;

    }

    public <T extends ResponseBase> T execute(RequestBase request, Class<T> clazz) throws IOException {
        return this.execute(request, 64, clazz);
    }

    public <T extends ResponseBase> T execute(RequestBase request, int size, Class<T> clazz) throws IOException {
        log.info("Attempting request: {}", request.getClass().getSimpleName());
        T response;
        if (request.getPacket() == null) {
            throw new IllegalArgumentException("Request is null");
        }
        log.info("Get packet: {}", Utils.bytesToHex(request.getPacket()));
        transport.write(request.getPacket(), 2000);
        log.info("Max response size set to: {}", size);
        byte[] rawResponse = transport.read(size, 2000);
        try {
            Constructor<T> ctor = clazz.getConstructor(byte[].class);
            response = ctor.newInstance(rawResponse);
            return response;
        } catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return null;
    }

    public ReadRadioResponse execute(CommandBase request, int retries) throws IOException {
        ReadRadioResponse response = null;
        TransmitPacketRequest transmitPacketRequest = request.getTransmitPacketRequest();
        execute(transmitPacketRequest, TransmitPacketResponse.class);
        sleep(request.getDelayAfterCommand());

        byte[] previousData = new byte[0];

        boolean finished = false;
        int counter = 0;
        while (!finished || counter == 30) {
            short responseSize = getRemoteResponseSize(request.getRetries(), request.getMinBufferSizeToStartReading());
            log.info("LinkStatus reports size: {}", responseSize);
            ReadRadioRequest readRadioRequest = new ReadRadioRequest(responseSize);
//        for (int i = 0; i < retries; i++) {
            response = execute(readRadioRequest, responseSize, ReadRadioResponse.class);
            response.prependData(previousData);
            finished = response.isEOD();
            log.info("My data: {}", Utils.bytesToHex(response.getData()));
            if (response.getData().length == 0) {
                sleep(250);
                log.warn("Uh oh, length was 0?");
            }
            log.info("ReadRadio Response: {}", Utils.bytesToHex(response.getData()));
            log.info("ReadRadio Response size: {}", response.getResultLength());
            log.info("LinkStatus reports size: {}", responseSize);
            if (response.isEOD()) {
                break;
            }
            counter += 1;
            previousData = response.getData();
            sleep(request.getDelayAfterCommand());
        }
        return response;
    }

    public ReadRadioResponse execute(CommandBase request) throws IOException {
        return this.execute(request, request.getRetries());
    }

    private short getRemoteResponseSize(int retries, int minSize) throws IOException {
        LinkStatusRequest linkStatusRequest = new LinkStatusRequest();
        short responseSize = 0;
        for (int i = 0; i < 30; i++) {
            LinkStatusResponse linkStatusResponse = this.execute(linkStatusRequest, LinkStatusResponse.class);
            log.info("Response size: {}", linkStatusResponse.getSize());
            if (linkStatusResponse.getSize() > minSize) {
                log.info("About to break");
                log.info("Response: {}", Utils.bytesToHex(linkStatusResponse.getData()));
                responseSize = linkStatusResponse.getSize();
                break;
            }
            log.info("About to sleep");
            sleep(2000);
        }
        log.info("getRemoteResponseSize() Returning: {}", responseSize);
        return responseSize;
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


}
