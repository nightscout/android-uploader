package com.nightscout.core.dexcom;

import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class SpecialValueTest {

    @Test
    public void testIsSpecialValues() {
        int[] values = new int[]{0, 1, 2, 3, 5, 6, 9, 10, 12};
        for (int aValue : values) {
            assertThat(SpecialValue.isSpecialValue(aValue), is(true));
        }
    }

    @Test
    public void testIsNotSpecialValue() {
        int[] values = new int[]{11, 39, 100, 52, 250, 401, 72, 53, 80};
        for (int aValue : values) {
            assertThat(SpecialValue.isSpecialValue(aValue), is(false));
        }
    }
}
