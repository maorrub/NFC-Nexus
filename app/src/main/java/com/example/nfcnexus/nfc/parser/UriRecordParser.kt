package com.example.nfcnexus.nfc.parser

import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import java.nio.charset.StandardCharsets

object UriRecordParser {

    val URI_PREFIX_MAP = mapOf(
        0x00.toByte() to "",
        0x01.toByte() to "http://www.",
        0x02.toByte() to "https://www.",
        0x03.toByte() to "http://",
        0x04.toByte() to "https://",
        0x05.toByte() to "tel:",
        0x06.toByte() to "mailto:",
        0x07.toByte() to "ftp://anonymous:anonymous@",
        0x08.toByte() to "ftp://ftp.",
        0x09.toByte() to "ftps://",
        0x0A.toByte() to "sftp://",
        0x0B.toByte() to "smb://",
        0x0C.toByte() to "nfs://",
        0x0D.toByte() to "ftp://",
        0x0E.toByte() to "dav://",
        0x0F.toByte() to "news:",
        0x10.toByte() to "telnet://",
        0x11.toByte() to "imap:",
        0x12.toByte() to "rtsp://",
        0x13.toByte() to "urn:",
        0x14.toByte() to "pop:",
        0x15.toByte() to "sip:",
        0x16.toByte() to "sips:",
        0x17.toByte() to "tftp:",
        0x18.toByte() to "btspp://",
        0x19.toByte() to "btl2cap://",
        0x1A.toByte() to "btgoep://",
        0x1B.toByte() to "tcpobex://",
        0x1C.toByte() to "irdaobex://",
        0x1D.toByte() to "file://",
        0x1E.toByte() to "urn:epc:id:",
        0x1F.toByte() to "urn:epc:tag:",
        0x20.toByte() to "urn:epc:pat:",
        0x21.toByte() to "urn:epc:raw:",
        0x22.toByte() to "urn:epc:",
        0x23.toByte() to "urn:nfc:"
    )

    fun parse(record: NdefRecord): ParsedRecord.Uri {
        val payload = record.payload ?: byteArrayOf()
        val rawHex = payload.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

        if (payload.isEmpty()) {
            return ParsedRecord.Uri(uri = "", rawBytesHex = rawHex)
        }

        val prefixByte = payload[0]
        val prefix = URI_PREFIX_MAP[prefixByte] ?: ""
        val suffix = if (payload.size > 1) {
            String(payload, 1, payload.size - 1, StandardCharsets.UTF_8)
        } else {
            ""
        }

        val fullUri = prefix + suffix
        val scheme = fullUri.substringBefore(":", "").let { if (it.isNotEmpty()) "$it:" else "" }
        val title = when {
            fullUri.startsWith("tel:") -> "Call: ${fullUri.removePrefix("tel:")}"
            fullUri.startsWith("mailto:") -> "Email: ${fullUri.removePrefix("mailto:")}"
            fullUri.startsWith("sms:") -> "SMS: ${fullUri.removePrefix("sms:")}"
            fullUri.startsWith("geo:") -> "Map Location"
            fullUri.startsWith("http://") || fullUri.startsWith("https://") -> fullUri.removePrefix("https://").removePrefix("http://").take(30)
            else -> fullUri.take(30)
        }

        return ParsedRecord.Uri(
            uri = fullUri,
            title = title,
            scheme = scheme,
            rawBytesHex = rawHex
        )
    }
}
