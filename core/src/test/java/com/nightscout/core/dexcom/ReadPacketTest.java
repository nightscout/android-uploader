package com.nightscout.core.dexcom;

import com.google.common.primitives.UnsignedBytes;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class ReadPacketTest {

    byte[] testPacket = new byte[]{
            /** HEADER **/0x1, 0x1, 0x1,
            /** COMMAND **/0x5,
            /** DATA **/0x10, 0x15,
            /** CRC */0x52, 0x33
    };

    byte[] testPacketNoData = new byte[]{
            /** HEADER **/0x1, 0x1, 0x1,
            /** COMMAND **/0x1A,
            /** CRC **/UnsignedBytes.checkedCast(0xCE), UnsignedBytes.checkedCast(0xC1)
    };

    byte[] testPacketBadCrc = new byte[]{
            /** HEADER **/0x1, 0x1, 0x1,
            /** COMMAND **/0x1A,
            /** CRC **/UnsignedBytes.checkedCast(0xCE), UnsignedBytes.checkedCast(0xC0)
    };

    @Test
    public void testReadPacket_command() {
        assertThat(new ReadPacket(testPacket).getCommand().getValue(), is((byte) 0x05));
    }

    @Test
    public void testReadPacket_data() {
        assertThat(new ReadPacket(testPacket).getData(), is(new byte[]{0x10, 0x15}));
    }

    @Test
    public void testReadPacket_noDataPacket_command() {
        assertThat(new ReadPacket(testPacketNoData).getCommand().getValue(), is((byte) 0x1A));
    }

    @Test
    public void testReadPacket_noDataPacket_emptyData() {
        assertThat(new ReadPacket(testPacketNoData).getData(), is(new byte[]{}));
    }

    @Test
    public void testReadPacket_badCrc() throws Exception {
        try {
            new ReadPacket(testPacketBadCrc);
            fail("Should receive CRC error");
        } catch (CRCFailError error) {
            // nom
        }
    }
}
