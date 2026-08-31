package com.example.nfcnexus.nfc.parser

import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

object WifiRecordParser {

    private const val ATTR_NETWORK_INDEX: Short = 0x1026
    private const val ATTR_SSID: Short = 0x1045
    private const val ATTR_AUTH_TYPE: Short = 0x1003
    private const val ATTR_ENCR_TYPE: Short = 0x100F
    private const val ATTR_NETWORK_KEY: Short = 0x1027
    private const val ATTR_MAC_ADDRESS: Short = 0x1020

    fun parse(record: NdefRecord): ParsedRecord.Wifi {
        val payload = record.payload ?: byteArrayOf()
        val rawHex = payload.joinToString("") { "%02X".format(it) }

        var ssid = ""
        var authType = "WPA2-Personal"
        var encryptionType = "AES"
        var networkKey = ""
        var macAddress = ""

        try {
            val buffer = ByteBuffer.wrap(payload)
            while (buffer.remaining() >= 4) {
                val attrId = buffer.short
                val attrLen = buffer.short.toInt() and 0xFFFF

                if (attrLen > buffer.remaining()) break

                val valueBytes = ByteArray(attrLen)
                buffer.get(valueBytes)

                when (attrId) {
                    ATTR_SSID -> {
                        ssid = String(valueBytes, StandardCharsets.UTF_8)
                    }
                    ATTR_AUTH_TYPE -> {
                        if (attrLen >= 2) {
                            val authVal = ByteBuffer.wrap(valueBytes).short.toInt() and 0xFFFF
                            authType = when (authVal) {
                                0x0001 -> "Open (None)"
                                0x0002 -> "WPA-Personal"
                                0x0004 -> "Shared (WEP)"
                                0x0008 -> "WPA-Enterprise"
                                0x0010 -> "WPA2-Enterprise"
                                0x0020 -> "WPA2-Personal"
                                0x0022 -> "WPA/WPA2-Personal"
                                0x0040 -> "WPA3-Personal"
                                else -> "0x%04X".format(authVal)
                            }
                        }
                    }
                    ATTR_ENCR_TYPE -> {
                        if (attrLen >= 2) {
                            val encrVal = ByteBuffer.wrap(valueBytes).short.toInt() and 0xFFFF
                            encryptionType = when (encrVal) {
                                0x0001 -> "None"
                                0x0002 -> "WEP"
                                0x0004 -> "TKIP"
                                0x0008 -> "AES / CCMP"
                                0x000C -> "TKIP/AES Mixed"
                                else -> "0x%04X".format(encrVal)
                            }
                        }
                    }
                    ATTR_NETWORK_KEY -> {
                        networkKey = String(valueBytes, StandardCharsets.UTF_8)
                    }
                    ATTR_MAC_ADDRESS -> {
                        macAddress = valueBytes.joinToString(":") { "%02X".format(it) }
                    }
                }
            }
        } catch (e: Exception) {
            // Fallback to text parsing if payload is raw text formatted
            val str = String(payload, StandardCharsets.UTF_8)
            if (str.startsWith("WIFI:")) {
                // Parse WIFI:S:SSID;T:WPA;P:Password;; format
                val parts = str.removePrefix("WIFI:").removeSuffix(";;").split(";")
                for (part in parts) {
                    when {
                        part.startsWith("S:") -> ssid = part.substring(2)
                        part.startsWith("T:") -> authType = part.substring(2)
                        part.startsWith("P:") -> networkKey = part.substring(2)
                    }
                }
            }
        }

        return ParsedRecord.Wifi(
            ssid = ssid.ifEmpty { "Unknown Wi-Fi" },
            authType = authType,
            encryptionType = encryptionType,
            networkKey = networkKey,
            macAddress = macAddress,
            rawBytesHex = rawHex
        )
    }
}
