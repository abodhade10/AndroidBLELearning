package com.example.blelearning.core.common

import com.example.blelearning.core.bluetooth.BleUuids
import java.util.UUID

/** Helpers to turn raw GATT bytes into something a human (and a unit test) can read. */
object ValueFormatter {

    fun ByteArray.toHex(): String = joinToString(" ") { "%02X".format(it) }

    /** "0A ff 01" / "0aff01" -> bytes. Returns null when the input is not valid hex. */
    fun parseHex(input: String): ByteArray? {
        val clean = input.filter { !it.isWhitespace() && it != ':' && it != ',' }
            .removePrefix("0x")
        if (clean.isEmpty() || clean.length % 2 != 0) return null
        return runCatching {
            ByteArray(clean.length / 2) { i -> clean.substring(i * 2, i * 2 + 2).toInt(16).toByte() }
        }.getOrNull()
    }

    fun ByteArray.toPrintableAscii(): String? =
        if (isNotEmpty() && all { it in 0x20..0x7E }) String(this, Charsets.US_ASCII) else null

    /** Little-endian unsigned integer for 1..4 byte payloads (BLE is little-endian on the wire). */
    fun ByteArray.toUIntLE(): Long? {
        if (isEmpty() || size > 4) return null
        var result = 0L
        for (i in indices) result = result or ((this[i].toLong() and 0xFF) shl (8 * i))
        return result
    }

    /** Heart Rate Measurement (0x2A37): bit 0 of the flags selects UINT8 vs UINT16 format. */
    fun parseHeartRate(bytes: ByteArray): Int? {
        if (bytes.size < 2) return null
        val flags = bytes[0].toInt() and 0xFF
        return if ((flags and 0x01) == 0) {
            bytes[1].toInt() and 0xFF
        } else if (bytes.size >= 3) {
            ((bytes[2].toInt() and 0xFF) shl 8) or (bytes[1].toInt() and 0xFF)
        } else null
    }

    /** One-line, human friendly interpretation of a value for a given characteristic. */
    fun describe(characteristic: UUID, bytes: ByteArray): String {
        val parts = mutableListOf("HEX ${bytes.toHex().ifEmpty { "(empty)" }}")
        when (characteristic) {
            BleUuids.HEART_RATE_MEASUREMENT ->
                parseHeartRate(bytes)?.let { parts += "$it bpm" }
            BleUuids.BATTERY_LEVEL ->
                bytes.firstOrNull()?.let { parts += "${it.toInt() and 0xFF} %" }
            else -> {
                val ascii = bytes.toPrintableAscii()
                if (ascii != null) parts += "\"$ascii\"" else bytes.toUIntLE()?.let { parts += "uint=$it" }
            }
        }
        return parts.joinToString("  |  ")
    }
}
