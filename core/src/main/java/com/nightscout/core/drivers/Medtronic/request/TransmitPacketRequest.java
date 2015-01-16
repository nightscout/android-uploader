package com.nightscout.core.drivers.Medtronic.request;

import com.nightscout.core.dexcom.Utils;
import com.nightscout.core.drivers.Medtronic.OpCodes;
import com.nightscout.core.drivers.Medtronic.remote_commands.CommandBase;
import com.nightscout.core.drivers.Medtronic.request.RequestBase;
import com.nightscout.core.utils.CRC;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

// See https://gist.github.com/bewest/6330546#file-transmit_packet-packet-png
public class TransmitPacketRequest extends RequestBase {
    protected final Logger log = LoggerFactory.getLogger(this.getClass());

//    protected byte[] packet;


    // POWER ON:
    // SN = 868042
    //    0100A701 383638303432 00
    //    0100A70138363830343200025502005B010AA2

    //PowerControl(serial='665455').format() ==  bytearray( [ 0x01, 0x00, 0xA7, 0x01, 0x66, 0x54, 0x55, 0x80, 0x02, 0x55, 0x00, 0x00, 0x00, 0x5D, 0xE6, 0x01, 0x0A, 0xA2 ] )
    //    0100A7016654558002550000005DE6010AA2
    public TransmitPacketRequest(byte[] serial, CommandBase command, byte retries) {
        log.info("Transmitting command: {}", command.getClass().getSimpleName());
//        MAX_RESPONSE_SIZE = command.getMaxResponseSize();
        byte[] opCode = OpCodes.TRANSMIT_PACKET;
        byte[] argLength = ByteBuffer.allocate(2).putShort((short) command.getParams().length).array();
        argLength[0] = (byte) (argLength[0] | (byte) 0x80);

        // unknown what this does. Special case if command = 93
        byte button = (command.getCode() == 93) ? (byte) 85 : 0;

        // Doesn't quite make sense but see:
        // https://bitbucket.org/bewest/carelink/src/2c650855822d35a9124fde110f47e75cc77b0374/ddmsDTWApplet.src/minimed/ddms/deviceportreader/ComLink2.java?at=default#cl-645
        byte recs = calcRecordsRequired(command);
        byte type = (recs) > 1 ? (byte) 2 : recs;
        byte unk = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        try {
            outputStream.write(opCode);
            outputStream.write(serial);
            outputStream.write(argLength);
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream.write(button);
        outputStream.write(retries);
        outputStream.write(type);
        outputStream.write(unk);
        log.info("Code: " + command.getCode());
        outputStream.write(command.getCode());
        byte packetCrc = CRC.calculate8(outputStream.toByteArray());
        outputStream.write(packetCrc);
        try {
            outputStream.write(command.getParams());
        } catch (IOException e) {
            e.printStackTrace();
        }
        outputStream.write(CRC.calculate8(command.getParams()));
        log.info("Request: {}", Utils.bytesToHex(outputStream.toByteArray()));
        packet = outputStream.toByteArray();
    }


    private byte calcRecordsRequired(CommandBase command) {
        int length = command.getBytesPerRecord() * command.getMaxRecords();
        int i = length / 64;
        int j = length % 64;
        if (j > 0) {
            return (byte) (i + 1);
        }
        return (byte) i;
    }

}