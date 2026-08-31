package com.example.nfcnexus.nfc.builder

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object WifiTlvEncoder {

    private const val ATTR_NETWORK_INDEX: Short = 0x1026
    private const val ATTR_SSID: Short = 0x1045
    private const val ATTR_AUTH_TYPE: Short = 0x1003
    private const val ATTR_ENCR_TYPE: Short = 0x100F
    private const val ATTR_NETWORK_KEY: Short = 0x1027
    private const val ATTR_MAC_ADDRESS: Short = 0x1020

    enum class AuthType(val code: Short, val defaultEncr: Short) {
        OPEN(0x0001, 0x0001),
        WPA_PERSONAL(0x0002, 0x0004),
        WPA2_PERSONAL(0x0020, 0x0008),
        WPA_WPA2_MIXED(0x0022, 0x000C),
        WPA3_PERSONAL(0x0040, 0x0008)
    }

    fun encode(
        ssid: String,
        networkKey: String,
        authType: AuthType = AuthType.WPA2_PERSONAL
    ): ByteArray {
        val out = ByteArrayOutputStream()

        // 1. Network Index
        writeTlv(out, ATTR_NETWORK_INDEX, byteArrayOf(0x01))

        // 2. SSID
        val ssidBytes = ssid.toByteArray(StandardCharsets.UTF_8)
        writeTlv(out, ATTR_SSID, ssidBytes)

        // 3. Auth Type
        val authBuffer = ByteBuffer.allocate(2).putShort(authType.code)
        writeTlv(out, ATTR_AUTH_TYPE, authBuffer.array())

        // 4. Encryption Type
        val encrBuffer = ByteBuffer.allocate(2).putShort(authType.defaultEncr)
        writeTlv(out, ATTR_ENCR_TYPE, encrBuffer.array())

        // 5. Network Key (Password)
        if (authType != AuthType.OPEN && networkKey.isNotEmpty()) {
            val keyBytes = networkKey.toByteArray(StandardCharsets.UTF_8)
            writeTlv(out, ATTR_NETWORK_KEY, keyBytes)
        }

        // 6. MAC Address (Broadcast / Any)
        val macBytes = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        writeTlv(out, ATTR_MAC_ADDRESS, macBytes)

        return out.toByteArray()
    }

    private fun writeTlv(out: ByteArrayOutputStream, tag: Short, value: ByteArray) {
        val header = ByteBuffer.allocate(4)
            .putShort(tag)
            .putShort(value.size.toShort())
            .array()
        out.write(header)
        out.write(value)
    }
}
