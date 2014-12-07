package com.nightscout.core.dexcom;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;


@RunWith(JUnit4.class)
public class CRC16Test {

    @Test
    public void testCRC16(){
        // 01 07 00 10 04 8b b8
        byte[] testArray = new byte[]{(byte) 0x01, (byte) 0x07, (byte) 0x00,
                (byte) 0x10, (byte) 0x04};
        byte[] expectedCrc = new byte[]{(byte) 0x8b,(byte) 0xb8};
        byte[] calculatedCrc = CRC16.calculate(testArray,0,testArray.length);
        assertThat(calculatedCrc, is(expectedCrc));
    }
}
