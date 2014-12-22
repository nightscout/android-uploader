package com.nightscout.core.dexcom;

import com.google.common.base.CaseFormat;
import com.google.common.base.Optional;

public enum Command {
    NULL(0),
    ACK(1),
    NAK(2),
    INVALID_COMMAND(3),
    INVALID_PARAM(4),
    INCOMPLETE_PACKET_RECEIVED(5),
    RECEIVER_ERROR(6),
    INVALID_MODE(7),
    PING(10),
    READ_FIRMWARE_HEADER(11),
    READ_DATABASE_PARTITION_INFO(15),
    READ_DATABASE_PAGE_RANGE(16),
    READ_DATABASE_PAGES(17),
    READ_DATABASE_PAGE_HEADER(18),
    READ_TRANSMITTER_ID(25),
    WRITE_TRANSMITTER_ID(26),
    READ_LANGUAGE(27),
    WRITE_LANGUAGE(28),
    READ_DISPLAY_TIME_OFFSET(29),
    WRITE_DISPLAY_TIME_OFFSET(30),
    READ_RTC(31),
    RESET_RECEIVER(32),
    READ_BATTERY_LEVEL(33),
    READ_SYSTEM_TIME(34),
    READ_SYSTEM_TIME_OFFSET(35),
    WRITE_SYSTEM_TIME(36),
    READ_GLUCOSE_UNIT(37),
    WRITE_GLUCOSE_UNIT(38),
    READ_BLINDED_MODE(39),
    WRITE_BLINDED_MODE(40),
    READ_CLOCK_MODE(41),
    WRITE_CLOCK_MODE(42),
    READ_DEVICE_MODE(43),
    ERASE_DATABASE(45),
    SHUTDOWN_RECEIVER(46),
    WRITE_PC_PARAMETERS(47),
    READ_BATTERY_STATE(48),
    READ_HARDWARE_BOARD_ID(49),
    READ_FIRMWARE_SETTINGS(54),
    READ_ENABLE_SETUP_WIZARD_FLAG(55),
    READ_SETUP_WIZARD_STATE(57);

    private byte value;

    Command(int command){
        value = (byte) command;
    }

    public byte getValue() {
        return value;
    }

    public String toString(){
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL,this.name()).replace("_", " ");
    }

    public static Optional<Command> getCommandByValue(int value){
        for(Command command:values()){
            if (command.getValue() == value){
                return Optional.of(command);
            }
        }
        return Optional.absent();
    }
}
