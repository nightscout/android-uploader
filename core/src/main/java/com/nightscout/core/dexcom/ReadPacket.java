package com.nightscout.core.dexcom;

import com.nightscout.core.drivers.PacketTooSmallException;
import com.nightscout.core.drivers.UnknownG4CommandException;

import net.tribe7.common.base.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;

public class ReadPacket {
    private static final Logger log = LoggerFactory.getLogger(ReadPacket.class);
    private Command command;
    private byte[] data;
    private static final int OFFSET_CMD = 3;
    private static final int OFFSET_DATA = 4;
    private static final int CRC_LEN = 2;

    public ReadPacket(byte[] readPacket) throws IOException {
        if (readPacket.length <= OFFSET_CMD) {
            throw new PacketTooSmallException("Insufficient data in reading");
        }
        Optional<Command> optCmd = Command.getCommandByValue(readPacket[OFFSET_CMD]);
        if (optCmd.isPresent()) {
            command = optCmd.get();
        } else {
            throw new UnknownG4CommandException("Unknown command: " + readPacket[OFFSET_CMD]);
        }
        log.debug("Initialize read packet of type {}", command.name());
        this.data = Arrays.copyOfRange(readPacket, OFFSET_DATA, readPacket.length - CRC_LEN);
        byte[] crc = Arrays.copyOfRange(readPacket, readPacket.length - CRC_LEN, readPacket.length);
        byte[] crc_calc = CRC16.calculate(readPacket, 0, readPacket.length - CRC_LEN);
        if (!Arrays.equals(crc, crc_calc)) {
            log.error("CRC check failed for command {}. Was {}, expected {}. Read packet length {}.",
                      command.name(), Utils.bytesToHex(crc), Utils.bytesToHex(crc_calc), readPacket.length);
            throw new CRCFailError("CRC check failed. Was: " + Utils.bytesToHex(crc) + " Expected: " + Utils.bytesToHex(crc_calc));
        }
    }

    public Command getCommand() {
        return command;
    }

    public byte[] getData() {
        return data;
    }
}
