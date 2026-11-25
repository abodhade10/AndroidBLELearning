package com.example.blelearning

import com.example.blelearning.core.common.ValueFormatter
import com.example.blelearning.core.common.ValueFormatter.toHex
import com.example.blelearning.core.common.ValueFormatter.toUIntLE
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValueFormatterTest {
    @Test fun parseHex_acceptsSpacesAndPrefix() {
        assertArrayEquals(byteArrayOf(0x48, 0x65), ValueFormatter.parseHex("0x48 65"))
    }

    @Test fun parseHex_rejectsOddLengthAndGarbage() {
        assertNull(ValueFormatter.parseHex("ABC"))
        assertNull(ValueFormatter.parseHex("ZZ"))
        assertNull(ValueFormatter.parseHex(""))
    }

    @Test fun toHex_formatsUppercasePairs() {
        assertEquals("0A FF", byteArrayOf(0x0A, 0xFF.toByte()).toHex())
    }

    @Test fun uintLittleEndian() {
        assertEquals(0x0201L, byteArrayOf(0x01, 0x02).toUIntLE())
    }

    @Test fun heartRate_uint8AndUint16() {
        assertEquals(72, ValueFormatter.parseHeartRate(byteArrayOf(0x00, 72)))
        assertEquals(300, ValueFormatter.parseHeartRate(byteArrayOf(0x01, 0x2C, 0x01)))
    }
}
