package com.example.nfcnexus.nfc.parser

import android.nfc.NdefRecord
import com.example.nfcnexus.data.model.ParsedRecord
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

object TextRecordParser {

    fun parse(record: NdefRecord): ParsedRecord.Text {
        val payload = record.payload ?: byteArrayOf()
        val rawHex = payload.joinToString("") { "%02X".format(it.toInt() and 0xFF) }

        if (payload.isEmpty()) {
            return ParsedRecord.Text(
                text = "",
                languageCode = "en",
                encoding = "UTF-8",
                rawBytesHex = rawHex
            )
        }

        val statusByte = payload[0].toInt()
        val isUtf16 = (statusByte and 0x80) != 0
        val textEncoding: Charset = if (isUtf16) StandardCharsets.UTF_16 else StandardCharsets.UTF_8
        val languageCodeLength = statusByte and 0x3F

        val langBytes = if (payload.size > 1 && languageCodeLength > 0 && languageCodeLength < payload.size) {
            payload.copyOfRange(1, 1 + languageCodeLength)
        } else {
            byteArrayOf()
        }
        val languageCode = String(langBytes, StandardCharsets.US_ASCII)

        val textStart = 1 + languageCodeLength
        val textBytes = if (textStart < payload.size) {
            payload.copyOfRange(textStart, payload.size)
        } else {
            byteArrayOf()
        }
        val text = String(textBytes, textEncoding)

        return ParsedRecord.Text(
            text = text,
            languageCode = languageCode.ifEmpty { "en" },
            encoding = if (isUtf16) "UTF-16" else "UTF-8",
            rawBytesHex = rawHex
        )
    }
}
