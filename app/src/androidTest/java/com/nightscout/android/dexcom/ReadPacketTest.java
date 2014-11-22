package com.nightscout.android.dexcom;

import com.google.common.primitives.UnsignedBytes;

import junit.framework.TestCase;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


public class ReadPacketTest extends TestCase {

    byte[] testPacket = new byte[] {
            /** HEADER **/ 0x1, 0x1, 0x1,
            /** COMMAND **/ 0x5,
            /** DATA **/ 0x10, 0x15,
            /** CRC */ 0x52, 0x33
    };

    byte[] testPacketNoData = new byte[] {
        /** HEADER **/ 0x1, 0x1, 0x1,
        /** COMMAND **/ 0x1A,
        /** CRC **/ UnsignedBytes.checkedCast(0xCE), UnsignedBytes.checkedCast(0xC1)
    };

    public void testReadPacket_command() {
        assertThat(new ReadPacket(testPacket).getCommand(), is(0x5));
    }

    public void testReadPacket_data() {
        assertThat(new ReadPacket(testPacket).getData(), is(new byte[]{0x10, 0x15}));
    }

    public void testReadPacket_noDataPacket_command() {
        assertThat(new ReadPacket(testPacketNoData).getCommand(), is(0x1A));
    }

    public void testReadPacket_noDataPacket_emptyData() {
        assertThat(new ReadPacket(testPacketNoData).getData(), is(new byte[]{}));
    }
}
