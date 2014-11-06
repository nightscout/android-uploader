package com.nightscout.android.dexcom;

import com.google.common.primitives.UnsignedBytes;

import junit.framework.TestCase;

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;

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
        MatcherAssert.assertThat(new ReadPacket(testPacket).getCommand(), Is.is(0x5));
    }

    public void testReadPacket_data() {
        MatcherAssert.assertThat(new ReadPacket(testPacket).getData(), Is.is(new byte[]{0x10, 0x15}));
    }

    public void testReadPacket_noDataPacket_command() {
        MatcherAssert.assertThat(new ReadPacket(testPacketNoData).getCommand(), Is.is(0x1A));
    }

    public void testReadPacket_noDataPacket_emptyData() {
        MatcherAssert.assertThat(new ReadPacket(testPacketNoData).getData(), Is.is(new byte[]{}));
    }
}
