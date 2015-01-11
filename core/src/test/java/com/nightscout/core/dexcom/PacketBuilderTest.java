package com.nightscout.core.dexcom;

import com.google.common.collect.Lists;

import org.junit.Test;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class PacketBuilderTest {

    @Test
    public void testPingCommand() {
        byte[] pingCommand = new byte[]{0x01, 0x06, 0x00, 0x0A, 0x5E, 0x65};
        PacketBuilder builder = new PacketBuilder(Command.PING);
        assertThat(builder.build(), is(pingCommand));
    }

    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 01070010048BB8
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 010700100A4559
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 01070010036CC8
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 0107001005AAA8
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 01070010048BB8
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 010700100A4559
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 01070010036CC8
    // Command: READ_DATABASE_PAGE_RANGE
    // Packet: 0107001005AAA8
    @Test
    public void testReadDatabasePageRangeCommand() {
        byte[] readDatabasePageRangeCommand = new byte[]{0x01, 0x07, 0x00, 0x10, 0x04, (byte) 0x8B, (byte) 0xB8};
        PacketBuilder builder = new PacketBuilder(Command.READ_DATABASE_PAGE_RANGE, Lists.newArrayList((byte) 0x04));
        assertThat(builder.build(), is(readDatabasePageRangeCommand));
    }

    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110488000000013263
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001104890000000163C9
    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110A01000000013D69
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001103CE000000019E77
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001103CF00000001CFDD
    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110526000000015EC3
    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110488000000013263
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001104890000000163C9
    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110A01000000013D69
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001103CE000000019E77
    // Command: READ_DATABASE_PAGES
    // Packet: 010C001103CF00000001CFDD
    // Command: READ_DATABASE_PAGES
    // Packet: 010C00110526000000015EC3
    @Test
    public void testReadDatabasePagesCommand() {
        byte[] readDatabasePages = new byte[]{0x01, 0x0C, 0x00, 0x11, 0x05, 0x26, 0x00, 0x00, 0x00, 0x01, 0x5E, (byte) 0xC3};
        ArrayList<Byte> payload = Lists.newArrayList((byte) 0x05, (byte) 0x26, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01);
        PacketBuilder builder = new PacketBuilder(Command.READ_DATABASE_PAGES, payload);
        assertThat(builder.build(), is(readDatabasePages));
    }

    // Command: READ_DISPLAY_TIME_OFFSET
    // Packet: 0106001D8807
    // Command: READ_DISPLAY_TIME_OFFSET
    // Packet: 0106001D8807
    @Test
    public void testReadDisplayTimeOffsetCommand() {
        byte[] readDatabasePages = new byte[]{0x01, 0x06, 0x00, 0x1D, (byte) 0x88, 0x07};
        PacketBuilder builder = new PacketBuilder(Command.READ_DISPLAY_TIME_OFFSET);
        assertThat(builder.build(), is(readDatabasePages));
    }

    // Command: READ_BATTERY_LEVEL
    // Packet: 0106002157F0
    @Test
    public void testReadBatteryLevelCommand() {
        byte[] readBatteryLevel = new byte[]{0x01, 0x06, 0x00, 0x21, 0x57, (byte) 0xF0};
        PacketBuilder builder = new PacketBuilder(Command.READ_BATTERY_LEVEL);
        assertThat(builder.build(), is(readBatteryLevel));
    }

    // Command: READ_SYSTEM_TIME
    // Packet: 0106002234C0
    // Command: READ_SYSTEM_TIME
    // Packet: 0106002234C0
    // Command: READ_SYSTEM_TIME
    // Packet: 0106002234C0
    // Command: READ_SYSTEM_TIME
    // Packet: 0106002234C0
    @Test
    public void testReadSystemTimeCommand() {
        byte[] readSystemTime = new byte[]{0x01, 0x06, 0x00, 0x22, 0x34, (byte) 0xC0};
        PacketBuilder builder = new PacketBuilder(Command.READ_SYSTEM_TIME);
        assertThat(builder.build(), is(readSystemTime));
    }
}
